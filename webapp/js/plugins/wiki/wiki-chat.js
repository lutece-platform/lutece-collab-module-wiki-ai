(function() {
    'use strict';

    const ICONS = {
        check: '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"></polyline></svg>',
        copy: '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path></svg>',
        spinner: '<span class="wiki-ai-tool-spinner"></span>'
    };

    const WikiAIChat = {
        elements: {},
        conversationId: null,
        isOpen: false,
        rateLimitState: { remaining: null, limit: null },
        conversationSources: {},

        init: function(config) {
            this.config = config || {};
            this.cacheElements();
            this.setupMarkdown();
            this.bindEvents();
            this.fetchRateLimitStatus();
        },

        cacheElements: function() {
            this.elements = {
                chatWrapper: document.getElementById('wikiAiChatWrapper'),
                chatOverlay: document.getElementById('wikiAiChatOverlay'),
                closeBtn: document.getElementById('wikiAiChatCloseBtn'),
                chatForm: document.getElementById('wikiAiChatForm'),
                queryInput: document.getElementById('wikiAiChatInput'),
                sendBtn: document.getElementById('wikiAiChatSendBtn'),
                chatMessages: document.getElementById('wikiAiChatMessages'),
                chatLayout: document.getElementById('wikiAiChatLayout'),
                chatInputBar: document.getElementById('wikiAiChatInputBar'),
                sourcePanel: document.getElementById('wikiAiSourcePanel'),
                sourceTitle: document.getElementById('wikiAiSourceTitle'),
                sourceContent: document.getElementById('wikiAiSourceContent'),
                sourceCloseBtn: document.getElementById('wikiAiSourceCloseBtn'),
                sourceOpenLink: document.getElementById('wikiAiSourceOpenLink')
            };
        },

        setupMarkdown: function() {
            MarkedWiki.init();
        },

        bindEvents: function() {
            const self = this;
            const { queryInput, closeBtn, chatOverlay, chatForm, sourceCloseBtn } = this.elements;

            queryInput.addEventListener('input', function() {
                this.style.height = 'auto';
                this.style.height = Math.min(this.scrollHeight, 120) + 'px';
            });

            queryInput.addEventListener('keydown', function(e) {
                if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    e.stopPropagation();
                    self.handleSubmit();
                }
            });

            closeBtn.addEventListener('click', () => self.closeChat());
            chatOverlay.addEventListener('click', e => e.target === chatOverlay && self.closeChat());
            chatForm.addEventListener('submit', e => { e.preventDefault(); self.handleSubmit(); });
            sourceCloseBtn.addEventListener('click', () => self.closeSourcePanel());

            document.addEventListener('keydown', function(e) {
                if (e.key === 'Escape' && self.isOpen) {
                    self.elements.chatLayout.classList.contains('with-source')
                        ? self.closeSourcePanel()
                        : self.closeChat();
                }
                if (e.ctrlKey && e.key === 'i') {
                    e.preventDefault();
                    self.toggleChat();
                }
            });
        },

        openChat: function() {
            this.isOpen = true;
            this.elements.chatWrapper.classList.add('active');
            document.body.style.overflow = 'hidden';
            this.elements.queryInput.focus();
            if (!this.conversationId) {
                this.createNewConversation().catch(err => {
                    console.error('Error initializing conversation:', err);
                    this.showError(err.message || 'Erreur lors de la création de la conversation');
                });
            }
        },

        closeChat: function() {
            this.isOpen = false;
            this.elements.chatWrapper.classList.remove('active');
            document.body.style.overflow = '';
            this.closeSourcePanel();
            this.resetConversation();
        },

        toggleChat: function() {
            this.isOpen ? this.closeChat() : this.openChat();
        },

        handleSubmit: function() {
            const query = this.elements.queryInput.value.trim();
            if (!query) return;

            if (!this.isOpen) this.openChat();

            this.addUserMessage(query);
            this.elements.queryInput.value = '';
            this.elements.queryInput.style.height = 'auto';
            this.elements.sendBtn.disabled = true;

            if (this.rateLimitState.remaining !== null && this.rateLimitState.remaining > 0) {
                this.rateLimitState.remaining--;
                this.updateRateLimitNotice();
            }

            if (!this.conversationId) {
                this.createNewConversation()
                    .then(() => this.streamChat(query))
                    .catch(err => {
                        console.error('Error creating conversation:', err);
                        this.showError(err.message || 'Erreur lors de la création de la conversation');
                        this.elements.sendBtn.disabled = false;
                    });
            } else {
                this.streamChat(query);
            }
        },

        addUserMessage: function(text) {
            const messageDiv = this.createElement('div', 'wiki-ai-chat-message user');
            const contentDiv = this.createElement('div', 'wiki-ai-chat-message-content');
            contentDiv.textContent = text;
            messageDiv.appendChild(contentDiv);
            this.elements.chatMessages.appendChild(messageDiv);
            this.scrollToBottom();
        },

        showError: function(message) {
            const alertDiv = this.createElement('div', 'alert alert-danger');
            alertDiv.setAttribute('role', 'alert');
            alertDiv.textContent = message;
            this.elements.chatMessages.appendChild(alertDiv);
            this.scrollToBottom();
            this.elements.queryInput.disabled = true;
            this.elements.sendBtn.disabled = true;
        },

        addLoadingIndicator: function() {
            const messageDiv = this.createElement('div', 'wiki-ai-chat-message loading');
            messageDiv.id = 'loadingIndicator';
            const contentDiv = this.createElement('div', 'wiki-ai-chat-message-content');
            contentDiv.innerHTML = '<div class="wiki-ai-chat-progress"><span class="wiki-ai-chat-progress-loader"></span><span class="wiki-ai-chat-progress-text"></span></div>';
            messageDiv.appendChild(contentDiv);
            this.elements.chatMessages.appendChild(messageDiv);
            this.scrollToBottom();
            return messageDiv;
        },

        addToolStep: function(container, toolName, message, status, inline = true) {
            const baseClass = inline ? 'wiki-ai-tool-step-inline' : 'wiki-ai-tool-step';
            const stepDiv = this.createElement('div', `${baseClass} ${status}`);
            stepDiv.dataset.toolName = toolName;

            const iconSpan = this.createElement('span', 'wiki-ai-tool-step-icon');
            iconSpan.innerHTML = status === 'running' ? ICONS.spinner : ICONS.check;

            const textSpan = this.createElement('span', 'wiki-ai-tool-step-text');
            textSpan.textContent = message;

            stepDiv.appendChild(iconSpan);
            stepDiv.appendChild(textSpan);
            container.appendChild(stepDiv);
            return stepDiv;
        },

        updateToolStep: function(stepDiv, status) {
            stepDiv.className = 'wiki-ai-tool-step ' + status;
            const iconSpan = stepDiv.querySelector('.wiki-ai-tool-step-icon');
            if (iconSpan && status === 'completed') {
                iconSpan.innerHTML = ICONS.check;
            }
        },

        createNewConversation: function() {
            return this.fetchJson('rest/wiki/ai/chat/new', { method: 'POST' })
                .then(data => {
                    if (data.conversation_id) {
                        this.conversationId = data.conversation_id;
                        return this.conversationId;
                    }
                    throw new Error(data.error || 'Failed to create conversation');
                });
        },

        resetConversation: function() {
            this.conversationId = null;
            this.conversationSources = {};
            this.elements.chatMessages.querySelectorAll('.wiki-ai-chat-message:not(.wiki-ai-chat-welcome), .alert')
                .forEach(el => el.remove());
            this.elements.queryInput.disabled = false;
            this.elements.sendBtn.disabled = false;
        },

        streamChat: function(query) {
            if (!this.conversationId) {
                console.error('No conversation ID available');
                return;
            }

            const loadingDiv = this.addLoadingIndicator();
            const state = { contentContainer: null, currentTextBlock: null, accumulatedText: '', currentToolStep: null };

            this.fetchJson('rest/wiki/ai/chat/stream/init', {
                method: 'POST',
                body: JSON.stringify({ query, conversation_id: this.conversationId, from: window.location.href })
            })
            .then(data => {
                if (data.error) throw new Error(data.error);
                if (!data.stream_id) throw new Error('No stream ID received');
                this.setupEventSource(data.stream_id, loadingDiv, state);
            })
            .catch(error => {
                console.error('Error initializing stream:', error);
                this.removeElement(loadingDiv);
                this.showError(error.message || 'Erreur lors de l\'initialisation du chat');
                this.elements.sendBtn.disabled = false;
            });
        },

        setupEventSource: function(streamId, loadingDiv, state) {
            const eventSource = new EventSource('rest/wiki/ai/chat/stream/events/' + streamId);

            const ensureContentContainer = () => {
                if (!state.contentContainer) {
                    this.removeElement(loadingDiv);
                    const messageDiv = this.createElement('div', 'wiki-ai-chat-message assistant');
                    state.contentContainer = this.createElement('div', 'wiki-ai-chat-message-content wiki-ai-streaming-content');
                    messageDiv.appendChild(state.contentContainer);
                    this.elements.chatMessages.appendChild(messageDiv);
                }
            };

            const ensureTextBlock = () => {
                if (!state.currentTextBlock) {
                    state.currentTextBlock = this.createElement('div', 'wiki-ai-text-block wiki-markdown-content');
                    state.contentContainer.appendChild(state.currentTextBlock);
                    state.accumulatedText = '';
                }
            };

            eventSource.addEventListener('tool_execution_started', e => {
                const data = JSON.parse(e.data);
                ensureContentContainer();
                state.currentTextBlock = null;
                state.currentToolStep = this.addToolStep(state.contentContainer, data.tool_name, data.message, 'running');
                this.scrollToBottom();
            });

            eventSource.addEventListener('tool_execution_completed', () => {
                if (state.currentToolStep) {
                    this.updateToolStep(state.currentToolStep, 'completed');
                    state.currentToolStep = null;
                }
                this.scrollToBottom();
            });

            eventSource.addEventListener('sources_metadata', e => {
                const data = JSON.parse(e.data);
                if (data.sources && Array.isArray(data.sources)) {
                    data.sources.forEach(source => {
                        if (source.code) this.conversationSources[source.code] = source;
                    });
                }
            });

            eventSource.addEventListener('token', e => {
                ensureContentContainer();
                ensureTextBlock();
                const data = JSON.parse(e.data);
                state.accumulatedText += data.token;
                const parsedHtml = marked.parse(state.accumulatedText);
                const withCitations = this.replaceCitations(parsedHtml);
                state.currentTextBlock.innerHTML = DOMPurify.sanitize(withCitations);
                this.applyCodeHighlighting(state.currentTextBlock);
                this.bindCitationLinks(state.currentTextBlock);
                this.scrollToBottom();
            });

            eventSource.addEventListener('completed', () => {
                eventSource.close();
                this.elements.sendBtn.disabled = false;
                this.elements.queryInput.focus();
                if (state.contentContainer) {
                    this.bindCitationLinks(state.contentContainer);
                    this.applyMarkdownExtensions(state.contentContainer);
                }
                this.scrollToBottom();
            });

            const handleError = (errorMessage) => {
                this.removeElement(loadingDiv);
                ensureContentContainer();
                ensureTextBlock();
                state.currentTextBlock.textContent = errorMessage;
                eventSource.close();
                this.elements.sendBtn.disabled = false;
            };

            eventSource.addEventListener('error', e => {
                const errorData = e.data ? JSON.parse(e.data) : {};
                handleError('Erreur: ' + (errorData.error || 'Erreur lors de la génération de la réponse'));
            });

            eventSource.onerror = () => handleError('Erreur de connexion au serveur');
        },

        applyCodeHighlighting: function(container) {
            container.querySelectorAll('pre code').forEach(block => {
                hljs.highlightElement(block);
                this.wrapCodeBlock(block);
            });
        },

        applyMarkdownExtensions: function(container) {
            if (window.renderMermaidBlocks) renderMermaidBlocks(container);
            setTimeout(() => {
                if (window.initLightbox) initLightbox(container);
            }, 100);
        },

        scrollToBottom: function() {
            const chatMain = this.elements.chatMessages.closest('.wiki-ai-chat-main');
            if (chatMain) chatMain.scrollTop = chatMain.scrollHeight;
        },

        wrapCodeBlock: function(codeElement) {
            const pre = codeElement.parentElement;
            if (pre.parentElement.classList.contains('wiki-ai-chat-code-block')) return;

            const language = codeElement.className.match(/language-(\w+)/)?.[1] || 'text';
            const wrapper = this.createElement('div', 'wiki-ai-chat-code-block');
            const header = this.createElement('div', 'wiki-ai-chat-code-header');

            const langSpan = this.createElement('span', 'wiki-ai-chat-code-language');
            langSpan.textContent = language;

            const copyBtn = this.createElement('button', 'wiki-ai-chat-code-copy');
            copyBtn.innerHTML = ICONS.copy + '<span>Copier</span>';
            copyBtn.addEventListener('click', () => {
                navigator.clipboard.writeText(codeElement.textContent).then(() => {
                    const span = copyBtn.querySelector('span');
                    const originalText = span.textContent;
                    span.textContent = 'Copié !';
                    copyBtn.classList.add('copied');
                    setTimeout(() => {
                        span.textContent = originalText;
                        copyBtn.classList.remove('copied');
                    }, 2000);
                });
            });

            header.appendChild(langSpan);
            header.appendChild(copyBtn);
            pre.parentNode.insertBefore(wrapper, pre);
            wrapper.appendChild(header);
            wrapper.appendChild(pre);
        },

        escapeHtmlAttr: function(str) {
            return String(str || '').replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/'/g, '&#39;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
        },

        replaceCitations: function(text) {
            const sourceMap = this.conversationSources;
            if (!sourceMap || Object.keys(sourceMap).length === 0) return text;

            return text.replace(/\[Source[s]?\s+([^\]]+)\]/gi, (match, sourceList) => {
                const codes = sourceList.split(/[,\s]+/).filter(c => c.length > 0);
                if (codes.length === 0) return match;

                const links = codes.map(code => {
                    const source = sourceMap[code];
                    if (source) {
                        const title = this.escapeHtmlAttr(source.title || code);
                        const url = this.escapeHtmlAttr(source.url || '');
                        const safeCode = this.escapeHtmlAttr(code);
                        if (code) {
                            return `<a href="#" data-item-code="${safeCode}" data-url="${url}" data-title="${title}" class="wiki-citation-link" title="${title}">${title}</a>`;
                        }
                        if (url) {
                            return `<a href="${url}" title="${title}" target="_blank" rel="noopener noreferrer">${title}</a>`;
                        }
                    }
                    return `<span class="wiki-citation-unknown">${this.escapeHtmlAttr(code)}</span>`;
                });
                return `<span class="wiki-citation-group">${links.join('')}</span>`;
            });
        },

        bindCitationLinks: function(container) {
            container.querySelectorAll('.wiki-citation-link:not([data-bound])').forEach(link => {
                link.dataset.bound = 'true';
                link.addEventListener('click', e => {
                    e.preventDefault();
                    const { itemCode, url, title } = link.dataset;
                    if (itemCode) this.openSourcePanel(itemCode, url, title);
                });
            });
        },

        openSourcePanel: function(itemCode, url, title) {
            const { chatLayout, chatInputBar, sourceTitle, sourceOpenLink, sourceContent } = this.elements;

            chatLayout.classList.add('with-source');
            chatInputBar.classList.add('with-source');
            sourceTitle.textContent = title || 'Source';
            sourceOpenLink.href = url || '#';
            sourceOpenLink.style.display = url ? 'inline-flex' : 'none';
            sourceContent.innerHTML = '<div class="wiki-ai-source-loading"><div class="wiki-ai-chat-progress"><span class="wiki-ai-chat-progress-loader"></span><span class="wiki-ai-chat-progress-text">Chargement...</span></div></div>';

            fetch('rest/wiki/item/' + encodeURIComponent(itemCode))
                .then(response => {
                    if (!response.ok) throw new Error('Impossible de charger la source');
                    return response.json();
                })
                .then(data => {
                    const content = data.currentRevision?.content || '';
                    if (content) {
                        sourceContent.innerHTML = DOMPurify.sanitize(marked.parse(content));
                        this.applyCodeHighlighting(sourceContent);
                        this.applyMarkdownExtensions(sourceContent);
                    } else {
                        sourceContent.innerHTML = '<div class="wiki-ai-source-error">Aucun contenu disponible pour cette source.</div>';
                    }
                })
                .catch(error => {
                    sourceContent.innerHTML = '<div class="wiki-ai-source-error">' + error.message + '</div>';
                });
        },

        closeSourcePanel: function() {
            this.elements.chatLayout.classList.remove('with-source');
            this.elements.chatInputBar.classList.remove('with-source');
        },

        fetchRateLimitStatus: function() {
            this.fetchJson('rest/wiki/ai/features/ratelimit')
                .then(data => {
                    this.rateLimitState.remaining = data.remaining || data._nRemaining;
                    this.rateLimitState.limit = data.limit || data._nLimit;
                    this.updateRateLimitNotice();
                })
                .catch(error => console.error('Error fetching rate limit status:', error));
        },

        updateRateLimitNotice: function() {
            const counterElement = document.getElementById('wikiAiRateLimitCounter');
            if (!counterElement || this.rateLimitState.remaining === null || this.rateLimitState.limit === null) return;

            let counterClass = 'wiki-ai-chat-rate-limit';
            if (this.rateLimitState.remaining === 0) {
                counterClass += ' depleted';
            } else if (this.rateLimitState.remaining < this.rateLimitState.limit * 0.2) {
                counterClass += ' low';
            }
            counterElement.className = counterClass;
            counterElement.textContent = this.rateLimitState.remaining + ' messages restants';
        },

        createElement: function(tag, className) {
            const el = document.createElement(tag);
            if (className) el.className = className;
            return el;
        },

        removeElement: function(el) {
            if (el && el.parentNode) el.remove();
        },

        fetchJson: function(url, options = {}) {
            return fetch(url, {
                ...options,
                headers: { 'Content-Type': 'application/json', ...options.headers }
            }).then(response => response.json());
        }
    };

    window.WikiAIChat = WikiAIChat;
})();
