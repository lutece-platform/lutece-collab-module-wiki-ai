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
package fr.paris.lutece.plugins.wiki.modules.ai.service.rag;

import java.util.Locale;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import fr.paris.lutece.plugins.wiki.modules.ai.business.IWikiAssistant;
import fr.paris.lutece.plugins.wiki.modules.ai.service.chat.MemoryService;
import fr.paris.lutece.plugins.wiki.modules.ai.service.tools.WikiBrowseBookTool;
import fr.paris.lutece.plugins.wiki.modules.ai.service.tools.WikiBrowseSpaceTool;
import fr.paris.lutece.plugins.wiki.modules.ai.service.tools.WikiGrepTool;
import fr.paris.lutece.plugins.wiki.modules.ai.service.tools.WikiListSpacesTool;
import fr.paris.lutece.plugins.wiki.modules.ai.service.tools.WikiReadContentTool;
import fr.paris.lutece.plugins.wiki.modules.ai.service.tools.WikiSearchInSpaceTool;
import fr.paris.lutece.plugins.wiki.modules.ai.service.tools.WikiSearchTool;
import fr.paris.lutece.portal.service.security.LuteceUser;
import fr.paris.lutece.portal.service.util.AppLogService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@ApplicationScoped
public class RAGService
{
    public static class StreamingContext
    {
        private final TokenStream _tokenStream;
        private final WikiSearchTool _searchTool;
        private final WikiSearchInSpaceTool _searchInSpaceTool;
        private final WikiGrepTool _grepTool;
        private final WikiReadContentTool _readTool;
        private final WikiListSpacesTool _listSpacesTool;
        private final WikiBrowseSpaceTool _browseSpaceTool;
        private final WikiBrowseBookTool _browseBookTool;

        public StreamingContext( TokenStream tokenStream, WikiSearchTool searchTool, WikiSearchInSpaceTool searchInSpaceTool, WikiGrepTool grepTool,
                WikiReadContentTool readTool, WikiListSpacesTool listSpacesTool, WikiBrowseSpaceTool browseSpaceTool, WikiBrowseBookTool browseBookTool )
        {
            _tokenStream = tokenStream;
            _searchTool = searchTool;
            _searchInSpaceTool = searchInSpaceTool;
            _grepTool = grepTool;
            _readTool = readTool;
            _listSpacesTool = listSpacesTool;
            _browseSpaceTool = browseSpaceTool;
            _browseBookTool = browseBookTool;
        }

        public TokenStream tokenStream( )
        {
            return _tokenStream;
        }

        public WikiSearchTool searchTool( )
        {
            return _searchTool;
        }

        public WikiSearchInSpaceTool searchInSpaceTool( )
        {
            return _searchInSpaceTool;
        }

        public WikiGrepTool grepTool( )
        {
            return _grepTool;
        }

        public WikiReadContentTool readTool( )
        {
            return _readTool;
        }

        public WikiListSpacesTool listSpacesTool( )
        {
            return _listSpacesTool;
        }

        public WikiBrowseSpaceTool browseSpaceTool( )
        {
            return _browseSpaceTool;
        }

        public WikiBrowseBookTool browseBookTool( )
        {
            return _browseBookTool;
        }
    }

    private static final String SYSTEM_PROMPT = "You are an AI assistant that helps users find information in Wiki documentation.\n"
            + "You have several tools to access wiki content:\n\n" + "SEARCH TOOLS:\n"
            + "- 'search': semantic search across the entire wiki (concepts, questions, synonyms)\n"
            + "- 'searchInSpace': targeted semantic search within a specific space\n"
            + "- 'grep': exact term search (counting occurrences, proper nouns, codes, precise references)\n"
            + "- 'readContent': read full content of an element (requires code from search results)\n\n" + "NAVIGATION TOOLS:\n"
            + "- 'listSpaces': list all available wiki spaces\n" + "- 'browseSpace': explore a space (categories and books)\n"
            + "- 'browseBook': explore a book (chapters and pages)\n\n" + "RECOMMENDED STRATEGY:\n"
            + "- Conceptual question: 'search' or 'searchInSpace' then 'readContent'\n" + "- Technical/exact term: 'grep' then 'readContent'\n"
            + "- Discovery: 'listSpaces' -> 'browseSpace' -> 'browseBook' -> 'readContent'\n\n" + "INVESTIGATION APPROACH:\n"
            + "- If initial search gives few or no results, try different terms, synonyms, or tools\n"
            + "- Cross-reference multiple sources when possible for complete answers\n"
            + "- Use grep to find all occurrences, then readContent on the most relevant ones\n"
            + "- Explore wiki structure (listSpaces, browseSpace) if semantic search fails\n"
            + "- Better to search thoroughly and give a complete answer than respond too quickly\n"
            + "- Combine tools: grep for precise counts, search for context, readContent for details\n\n" + "MANDATORY CITATIONS:\n"
            + "- Each search result has a unique code (e.g., fastdeploy, pag-01-ric)\n"
            + "- You MUST cite your sources using: [Source code] after each piece of information\n" + "- For multiple sources: [Source code1, code2, code3]\n"
            + "- Example: 'Lutece was created in 2001 [Source lutece-history].'\n"
            + "- Example with multiple: 'This feature is documented here [Source fastdeploy, pag-01-fd].'\n\n" + "IMPORTANT CONSTRAINTS:\n"
            + "- You can ONLY answer questions using information found in the wiki\n"
            + "- If the information is not in the wiki, clearly state that you cannot find it in the documentation\n"
            + "- NEVER use your general knowledge to answer - only wiki content\n"
            + "- If the question is off-topic (not related to wiki content), politely redirect to wiki-related questions\n\n"
            + "If information is not found after thorough investigation, state it clearly and suggest alternative approaches (different keywords, exploring spaces).";

    private static final String ERROR_SECURITY_STREAMING = "Security error during streaming chat: {}";
    private static final String ERROR_STREAMING_CHAT = "Error during streaming chat for query: {}";

    @Inject
    @Named( "wiki-ai.streamingChatModel" )
    private StreamingChatModel _streamingChatModel;

    @Inject
    private MemoryService _memoryService;

    public StreamingContext chatWithStreaming( String query, String conversationId, LuteceUser user, String streamId, Locale locale, String fromUrl )
    {
        try
        {
            ChatMemory chatMemory = _memoryService.getChatMemory( conversationId, user );

            if ( chatMemory.messages( ).isEmpty( ) )
            {
                String systemPrompt = buildSystemPrompt( locale, fromUrl );
                chatMemory.add( SystemMessage.from( systemPrompt ) );
            }

            WikiSearchTool searchTool = new WikiSearchTool( user );
            WikiSearchInSpaceTool searchInSpaceTool = new WikiSearchInSpaceTool( user );
            WikiGrepTool grepTool = new WikiGrepTool( user );
            WikiReadContentTool readContentTool = new WikiReadContentTool( user );
            WikiListSpacesTool listSpacesTool = new WikiListSpacesTool( user );
            WikiBrowseSpaceTool browseSpaceTool = new WikiBrowseSpaceTool( user );
            WikiBrowseBookTool browseBookTool = new WikiBrowseBookTool( user );

            IWikiAssistant assistant = AiServices.builder( IWikiAssistant.class ).streamingChatModel( _streamingChatModel ).chatMemory( chatMemory )
                    .tools( searchTool, searchInSpaceTool, grepTool, readContentTool, listSpacesTool, browseSpaceTool, browseBookTool ).build( );

            TokenStream tokenStream = assistant.chatStream( query );
            return new StreamingContext( tokenStream, searchTool, searchInSpaceTool, grepTool, readContentTool, listSpacesTool, browseSpaceTool,
                    browseBookTool );
        }
        catch( SecurityException e )
        {
            AppLogService.error( ERROR_SECURITY_STREAMING, e.getMessage( ), e );
            return null;
        }
        catch( Exception e )
        {
            AppLogService.error( ERROR_STREAMING_CHAT, query, e );
            return null;
        }
    }

    private String buildSystemPrompt( Locale locale, String fromUrl )
    {
        StringBuilder prompt = new StringBuilder( SYSTEM_PROMPT );

        prompt.append( "\n\nRESPONSE LANGUAGE:\n" ).append( "The user's detected locale is: " ).append( locale.getDisplayLanguage( Locale.ENGLISH ) )
                .append( " (" ).append( locale.getLanguage( ) ).append( ").\n" ).append( "You MUST respond in " )
                .append( locale.getDisplayLanguage( Locale.ENGLISH ) ).append( " unless the user's message is clearly written in a different language.\n" )
                .append( "If the user writes in a specific language, respond in that language instead." );

        if ( fromUrl != null && !fromUrl.isEmpty( ) )
        {
            prompt.append( "\n\nUSER CONTEXT:\n" ).append( "The user is currently viewing: " ).append( fromUrl ).append( "\n" )
                    .append( "This may provide context about what they are looking for. " )
                    .append( "If their question seems related to the current page, prioritize information relevant to that context." );
        }

        return prompt.toString( );
    }
}
