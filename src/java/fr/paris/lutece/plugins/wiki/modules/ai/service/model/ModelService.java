/*
 * Copyright (c) 2002-2026, City of Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.wiki.modules.ai.service.model;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import fr.paris.lutece.portal.service.spring.SpringContextService;

/**
 * Service for managing LangChain4j models from Spring context. Provides centralized access to embedding, chat, and streaming chat models.
 */
public class ModelService
{
    private static final String BEAN_EMBEDDING_MODEL = "wiki-ai.embeddingModel";
    private static final String BEAN_CHAT_MODEL = "wiki-ai.chatModel";
    private static final String BEAN_STREAMING_CHAT_MODEL = "wiki-ai.streamingChatModel";

    private static class SingletonHolder
    {
        static final ModelService INSTANCE = new ModelService( );
    }

    private EmbeddingModel _embeddingModel;
    private ChatModel _chatModel;
    private StreamingChatModel _streamingChatModel;

    private ModelService( )
    {
    }

    public static ModelService getInstance( )
    {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Retrieves the embedding model from Spring context
     *
     * @return The embedding model
     */
    public EmbeddingModel getEmbeddingModel( )
    {
        if ( _embeddingModel == null )
        {
            _embeddingModel = SpringContextService.getBean( BEAN_EMBEDDING_MODEL );
        }
        return _embeddingModel;
    }

    /**
     * Retrieves the chat model from Spring context
     *
     * @return The chat model
     */
    public ChatModel getChatModel( )
    {
        if ( _chatModel == null )
        {
            _chatModel = SpringContextService.getBean( BEAN_CHAT_MODEL );
        }
        return _chatModel;
    }

    /**
     * Retrieves the streaming chat model from Spring context
     *
     * @return The streaming chat model
     */
    public StreamingChatModel getStreamingChatModel( )
    {
        if ( _streamingChatModel == null )
        {
            _streamingChatModel = SpringContextService.getBean( BEAN_STREAMING_CHAT_MODEL );
        }
        return _streamingChatModel;
    }
}
