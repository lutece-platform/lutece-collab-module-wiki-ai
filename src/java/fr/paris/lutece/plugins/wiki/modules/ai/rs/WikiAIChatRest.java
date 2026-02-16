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
package fr.paris.lutece.plugins.wiki.modules.ai.rs;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.langchain4j.service.TokenStream;
import fr.paris.lutece.plugins.wiki.modules.ai.service.chat.MemoryService;
import fr.paris.lutece.plugins.wiki.modules.ai.service.chat.StreamingService;
import fr.paris.lutece.plugins.wiki.modules.ai.service.event.WikiEvent;
import fr.paris.lutece.plugins.wiki.modules.ai.service.event.WikiEventService;
import fr.paris.lutece.plugins.wiki.modules.ai.service.event.WikiEventTypes;
import fr.paris.lutece.plugins.wiki.modules.ai.service.rag.RAGService;
import fr.paris.lutece.plugins.wiki.modules.ai.service.rag.RAGService.StreamingContext;
import fr.paris.lutece.plugins.wiki.modules.ai.service.rate.AiRateLimitService;
import fr.paris.lutece.plugins.wiki.modules.ai.service.rate.RateLimitResult;
import fr.paris.lutece.plugins.wiki.modules.ai.service.tools.AbstractWikiTool;
import fr.paris.lutece.portal.service.security.LuteceUser;
import fr.paris.lutece.portal.service.security.SecurityService;
import fr.paris.lutece.portal.service.util.AppLogService;

/**
 * REST endpoint for Wiki AI chat operations with streaming support.
 */
@RequestScoped
@Path( "wiki/ai" )
public class WikiAIChatRest extends AbstractRestEndpoint
{
    private static final String LOG_AUTH_FAILED = "Authentication failed for wiki AI chat: {}";
    private static final String LOG_SSE_ERROR = "Error setting up SSE stream for streamId: {}";
    private static final String LOG_NO_AUTH_USER_NEW_CONVERSATION = "No authenticated user for new conversation";
    private static final String LOG_NO_AUTH_USER = "No authenticated user";
    private static final String LOG_ERROR_SENDING_TOKEN = "Error sending token event";
    private static final String LOG_ERROR_SENDING_COMPLETED = "Error sending completed event";
    private static final String LOG_ERROR_SENDING_TOOL_EVENT = "Error sending tool event";
    private static final String LOG_ERROR_DURING_STREAMING = "Error during streaming: {}";
    private static final String LOG_ERROR_EXECUTING_STREAMING = "Error executing streaming chat: {}";
    private static final String LOG_DEBUG_PARSE_TOOL_ARGS = "Failed to parse tool arguments: {}";

    private static final String KEY_STATUS = "status";
    private static final String KEY_TOOL_NAME = "tool_name";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_SOURCES = "sources";
    private static final String KEY_QUERY = "query";
    private static final String KEY_CONVERSATION_ID = "conversation_id";
    private static final String KEY_FROM = "from";
    private static final String VALUE_STATUS_DONE = "done";

    private static final String ERROR_INIT_STREAMING = "Erreur lors de l'initialisation du streaming";
    private static final String MSG_TOOL_SEARCH = "Recherche : ";
    private static final String MSG_TOOL_SEARCH_IN_SPACE = "Recherche dans ";
    private static final String MSG_TOOL_GREP = "Grep : ";
    private static final String MSG_TOOL_READ = "Lecture : ";
    private static final String MSG_TOOL_LIST_SPACES = "Exploration des espaces wiki";
    private static final String MSG_TOOL_BROWSE_SPACE = "Navigation dans l'espace : ";
    private static final String MSG_TOOL_BROWSE_BOOK = "Navigation dans le livre : ";
    private static final String MSG_TOOL_COMPLETED = "Traitement terminé";
    private static final String MSG_TOOL_EXECUTION_DEFAULT = "Exécution de %s...";

    private static final String TOOL_NAME_SEARCH = "search";
    private static final String TOOL_NAME_SEARCH_IN_SPACE = "searchInSpace";
    private static final String TOOL_NAME_GREP = "grep";
    private static final String TOOL_NAME_READ = "readContent";
    private static final String TOOL_NAME_LIST_SPACES = "listSpaces";
    private static final String TOOL_NAME_BROWSE_SPACE = "browseSpace";
    private static final String TOOL_NAME_BROWSE_BOOK = "browseBook";

    private static final String ARG_QUERY = "query";
    private static final String ARG_TERM = "term";
    private static final String ARG_CODE = "code";
    private static final String ARG_SPACE_CODE = "spaceCode";
    private static final String ARG_BOOK_CODE = "bookCode";
    private static final String ARG_0 = "arg0";
    private static final String ARG_1 = "arg1";

    private static final String SEPARATOR_COLON = " : ";
    private static final String SEPARATOR_IN = " (dans ";
    private static final String SEPARATOR_CLOSE = ")";
    private static final String ELLIPSIS = "...";
    private static final int MAX_ARG_LENGTH = 50;
    private static final int TRUNCATE_LENGTH = 47;

    @Context
    private HttpServletRequest _request;

    @Inject
    private MemoryService _memoryService;
    @Inject
    private RAGService _ragService;
    @Inject
    private AiRateLimitService _rateLimitService;
    @Inject
    private StreamingService _sseStreamManager;
    @Inject
    private WikiEventService _eventService;
    /**
     * Creates a new conversation for the authenticated user.
     *
     * @return response containing the conversation_id or error details
     */
    @POST
    @Path( "chat/new" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response createNewConversation( )
    {
        LuteceUser user = SecurityService.getInstance( ).getRegisteredUser( _request );
        if ( user == null )
        {
            AppLogService.error( LOG_AUTH_FAILED, LOG_NO_AUTH_USER_NEW_CONVERSATION );
            return createErrorResponse( Response.Status.UNAUTHORIZED, WikiAIRestConstants.ERROR_AUTHENTICATION_FAILED );
        }

        try
        {
            String conversationId = _memoryService.createConversation( user );
            return Response.status( Response.Status.OK ).entity( Map.of( WikiAIRestConstants.KEY_CONVERSATION_ID, conversationId ) ).build( );
        }
        catch( Exception e )
        {
            return handleException( e, WikiAIRestConstants.ERROR_CREATING_CONVERSATION );
        }
    }

    /**
     * Initializes a streaming chat session.
     *
     * @param request
     *            the request containing query and conversation_id
     * @return response containing the stream_id or error details
     */
    @POST
    @Path( "chat/stream/init" )
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Response initStreamingChat( Map<String, Object> request )
    {
        LuteceUser user = SecurityService.getInstance( ).getRegisteredUser( _request );
        if ( user == null )
        {
            AppLogService.error( LOG_AUTH_FAILED, LOG_NO_AUTH_USER );
            return createErrorResponse( Response.Status.UNAUTHORIZED, WikiAIRestConstants.ERROR_AUTHENTICATION_FAILED );
        }

        RateLimitResult rateLimitResult = _rateLimitService.checkRateLimit( user.getName( ) );
        if ( !rateLimitResult.isAllowed( ) )
        {
            return createErrorResponse( Response.Status.TOO_MANY_REQUESTS, rateLimitResult.getErrorMessage( ) );
        }

        String query = (String) request.get( KEY_QUERY );
        String conversationId = (String) request.get( KEY_CONVERSATION_ID );
        String fromUrl = (String) request.get( KEY_FROM );

        if ( query == null || query.trim( ).isEmpty( ) )
        {
            return createErrorResponse( Response.Status.BAD_REQUEST, WikiAIRestConstants.ERROR_QUERY_REQUIRED );
        }

        if ( conversationId == null || conversationId.trim( ).isEmpty( ) )
        {
            return createErrorResponse( Response.Status.BAD_REQUEST, WikiAIRestConstants.ERROR_CONVERSATION_ID_REQUIRED );
        }

        if ( !isValidConversationId( conversationId ) )
        {
            return createErrorResponse( Response.Status.BAD_REQUEST, WikiAIRestConstants.ERROR_INVALID_CONVERSATION_ID );
        }

        if ( !_sseStreamManager.canCreateNewStream( ) )
        {
            return createErrorResponse( Response.Status.SERVICE_UNAVAILABLE, WikiAIRestConstants.ERROR_HIGH_TRAFFIC );
        }

        String streamId = UUID.randomUUID( ).toString( );
        executeStreamingChat( query, conversationId, user, streamId, _request.getLocale( ), fromUrl );

        return Response.status( Response.Status.OK ).entity( Map.of( WikiAIRestConstants.KEY_STREAM_ID, streamId ) ).build( );
    }

    /**
     * Establishes an SSE connection for receiving chat streaming events.
     *
     * @param streamId
     *            the stream identifier
     * @param eventSink
     *            the SSE event sink
     * @param sse
     *            the SSE context
     */
    @GET
    @Path( "chat/stream/events/{streamId}" )
    @Produces( MediaType.SERVER_SENT_EVENTS )
    public void getStreamingChatEvents( @PathParam( "streamId" ) String streamId, @Context SseEventSink eventSink, @Context Sse sse )
    {
        LuteceUser user = SecurityService.getInstance( ).getRegisteredUser( _request );
        if ( user == null )
        {
            AppLogService.error( LOG_AUTH_FAILED, streamId );
            closeEventSinkWithError( eventSink, sse, WikiAIRestConstants.ERROR_AUTHENTICATION_FAILED );
            return;
        }

        try
        {
            _sseStreamManager.registerSseStream( streamId, user.getName( ), eventSink, sse );
        }
        catch( Exception e )
        {
            AppLogService.error( LOG_SSE_ERROR, streamId, e );
            closeEventSinkWithError( eventSink, sse, WikiAIRestConstants.ERROR_STREAM_SETUP_FAILED );
        }
    }

    /**
     * Executes the streaming chat asynchronously.
     *
     * @param query
     *            the user query
     * @param conversationId
     *            the conversation identifier
     * @param user
     *            the authenticated user
     * @param streamId
     *            the stream identifier
     * @param locale
     *            the user's locale for language detection
     * @param fromUrl
     *            the URL the user is currently viewing
     */
    private void executeStreamingChat( String query, String conversationId, LuteceUser user, String streamId, java.util.Locale locale, String fromUrl )
    {
        CompletableFuture.runAsync( ( ) -> {
            try
            {
                StreamingContext streamingContext = _ragService.chatWithStreaming( query, conversationId, user, streamId, locale, fromUrl );
                if ( streamingContext == null )
                {
                    sendErrorEvent( streamId, ERROR_INIT_STREAMING );
                    return;
                }

                TokenStream tokenStream = streamingContext.tokenStream( );

                tokenStream.beforeToolExecution( beforeTool -> {
                    handleBeforeToolExecution( streamId, beforeTool.request( ).name( ), beforeTool.request( ).arguments( ) );
                } ).onToolExecuted( toolExecution -> {
                    handleToolExecuted( streamId, streamingContext, toolExecution.request( ).name( ) );
                } ).onPartialResponse( token -> {
                    handlePartialResponse( streamId, token );
                } ).onCompleteResponse( response -> {
                    handleCompleteResponse( streamId );
                } ).onError( error -> {
                    AppLogService.error( LOG_ERROR_DURING_STREAMING, error.getMessage( ), error );
                    sendErrorEvent( streamId, error.getMessage( ) );
                } ).start( );
            }
            catch( Exception e )
            {
                AppLogService.error( LOG_ERROR_EXECUTING_STREAMING, e.getMessage( ), e );
                sendErrorEvent( streamId, e.getMessage( ) );
            }
        } );
    }

    /**
     * Handles the before tool execution event.
     *
     * @param streamId
     *            the stream identifier
     * @param toolName
     *            the tool name
     * @param toolArgs
     *            the tool arguments
     */
    private void handleBeforeToolExecution( String streamId, String toolName, String toolArgs )
    {
        try
        {
            String message = getToolStartMessage( toolName, toolArgs );
            WikiEvent event = new WikiEvent( streamId, WikiEventTypes.TOOL_EXECUTION_STARTED, Map.of( KEY_TOOL_NAME, toolName, KEY_MESSAGE, message ) );
            _eventService.dispatchEvent( event );
        }
        catch( Exception e )
        {
            AppLogService.error( LOG_ERROR_SENDING_TOOL_EVENT, e );
        }
    }

    /**
     * Handles the tool executed event.
     *
     * @param streamId
     *            the stream identifier
     * @param streamingContext
     *            the streaming context
     * @param toolName
     *            the tool name
     */
    private void handleToolExecuted( String streamId, StreamingContext streamingContext, String toolName )
    {
        try
        {
            WikiEvent event = new WikiEvent( streamId, WikiEventTypes.TOOL_EXECUTION_COMPLETED,
                    Map.of( KEY_TOOL_NAME, toolName, KEY_MESSAGE, MSG_TOOL_COMPLETED ) );
            _eventService.dispatchEvent( event );

            AbstractWikiTool tool = getToolByName( streamingContext, toolName );
            if ( tool != null )
            {
                List<Map<String, Object>> sources = tool.getLastSources( );
                if ( sources != null && !sources.isEmpty( ) )
                {
                    WikiEvent sourcesEvent = new WikiEvent( streamId, WikiEventTypes.SOURCES_METADATA, Map.of( KEY_SOURCES, sources ) );
                    _eventService.dispatchEvent( sourcesEvent );
                }
            }
        }
        catch( Exception e )
        {
            AppLogService.error( LOG_ERROR_SENDING_TOOL_EVENT, e );
        }
    }

    /**
     * Handles a partial response token.
     *
     * @param streamId
     *            the stream identifier
     * @param token
     *            the token
     */
    private void handlePartialResponse( String streamId, String token )
    {
        try
        {
            WikiEvent event = new WikiEvent( streamId, WikiEventTypes.TOKEN_STREAMED, Map.of( WikiAIRestConstants.KEY_TOKEN, token ) );
            _eventService.dispatchEvent( event );
        }
        catch( Exception e )
        {
            AppLogService.error( LOG_ERROR_SENDING_TOKEN, e );
        }
    }

    /**
     * Handles the complete response event.
     *
     * @param streamId
     *            the stream identifier
     */
    private void handleCompleteResponse( String streamId )
    {
        try
        {
            WikiEvent event = new WikiEvent( streamId, WikiEventTypes.STREAM_COMPLETED, Map.of( KEY_STATUS, VALUE_STATUS_DONE ) );
            _eventService.dispatchEvent( event );
        }
        catch( Exception e )
        {
            AppLogService.error( LOG_ERROR_SENDING_COMPLETED, e );
        }
    }

    /**
     * Gets the tool instance by name from the streaming context.
     *
     * @param ctx
     *            the streaming context
     * @param toolName
     *            the tool name
     * @return the tool instance, or null if not found
     */
    private AbstractWikiTool getToolByName( StreamingContext ctx, String toolName )
    {
        switch( toolName )
        {
            case TOOL_NAME_SEARCH:
                return ctx.searchTool( );
            case TOOL_NAME_SEARCH_IN_SPACE:
                return ctx.searchInSpaceTool( );
            case TOOL_NAME_GREP:
                return ctx.grepTool( );
            case TOOL_NAME_READ:
                return ctx.readTool( );
            case TOOL_NAME_LIST_SPACES:
                return ctx.listSpacesTool( );
            case TOOL_NAME_BROWSE_SPACE:
                return ctx.browseSpaceTool( );
            case TOOL_NAME_BROWSE_BOOK:
                return ctx.browseBookTool( );
            default:
                return null;
        }
    }

    /**
     * Gets the localized start message for a tool execution.
     *
     * @param toolName
     *            the tool name
     * @param arguments
     *            the tool arguments as JSON string
     * @return the formatted start message
     */
    private String getToolStartMessage( String toolName, String arguments )
    {
        String detail = extractArgumentDetail( toolName, arguments );
        switch( toolName )
        {
            case TOOL_NAME_SEARCH:
                return MSG_TOOL_SEARCH + detail;
            case TOOL_NAME_SEARCH_IN_SPACE:
                return MSG_TOOL_SEARCH_IN_SPACE + detail;
            case TOOL_NAME_GREP:
                return MSG_TOOL_GREP + detail;
            case TOOL_NAME_READ:
                return MSG_TOOL_READ + detail;
            case TOOL_NAME_LIST_SPACES:
                return MSG_TOOL_LIST_SPACES;
            case TOOL_NAME_BROWSE_SPACE:
                return MSG_TOOL_BROWSE_SPACE + detail;
            case TOOL_NAME_BROWSE_BOOK:
                return MSG_TOOL_BROWSE_BOOK + detail;
            default:
                return String.format( MSG_TOOL_EXECUTION_DEFAULT, toolName );
        }
    }

    /**
     * Extracts a human-readable detail from tool arguments.
     *
     * @param toolName
     *            the tool name
     * @param arguments
     *            the tool arguments as JSON string
     * @return the extracted detail, or empty string
     */
    private String extractArgumentDetail( String toolName, String arguments )
    {
        if ( arguments == null || arguments.isEmpty( ) )
        {
            return "";
        }
        try
        {
            JsonNode node = new ObjectMapper( ).readTree( arguments );
            String value = extractValueByToolName( toolName, node );

            if ( value != null )
            {
                return value.length( ) > MAX_ARG_LENGTH ? value.substring( 0, TRUNCATE_LENGTH ) + ELLIPSIS : value;
            }
        }
        catch( Exception e )
        {
            AppLogService.debug( LOG_DEBUG_PARSE_TOOL_ARGS, e.getMessage( ) );
        }
        return "";
    }

    /**
     * Extracts the relevant value from JSON node based on tool name.
     *
     * @param toolName
     *            the tool name
     * @param node
     *            the JSON node containing arguments
     * @return the extracted value, or null
     */
    private String extractValueByToolName( String toolName, JsonNode node )
    {
        switch( toolName )
        {
            case TOOL_NAME_SEARCH:
                return getFirstFieldValue( node, ARG_QUERY, ARG_0 );
            case TOOL_NAME_SEARCH_IN_SPACE:
                return extractSearchInSpaceValue( node );
            case TOOL_NAME_GREP:
                return extractGrepValue( node );
            case TOOL_NAME_READ:
                return getFirstFieldValue( node, ARG_CODE, ARG_0 );
            case TOOL_NAME_BROWSE_SPACE:
                return getFirstFieldValue( node, ARG_SPACE_CODE, ARG_0 );
            case TOOL_NAME_BROWSE_BOOK:
                return getFirstFieldValue( node, ARG_BOOK_CODE, ARG_0 );
            default:
                return null;
        }
    }

    /**
     * Extracts the value for searchInSpace tool.
     *
     * @param node
     *            the JSON node
     * @return the formatted value
     */
    private String extractSearchInSpaceValue( JsonNode node )
    {
        String spaceCode = getFirstFieldValue( node, ARG_SPACE_CODE, ARG_0 );
        String query = getFirstFieldValue( node, ARG_QUERY, ARG_1 );
        if ( spaceCode != null && query != null )
        {
            return spaceCode + SEPARATOR_COLON + query;
        }
        return spaceCode;
    }

    /**
     * Extracts the value for grep tool.
     *
     * @param node
     *            the JSON node
     * @return the formatted value
     */
    private String extractGrepValue( JsonNode node )
    {
        String term = getFirstFieldValue( node, ARG_TERM, ARG_0 );
        String grepSpaceCode = getFirstFieldValue( node, ARG_SPACE_CODE, ARG_1 );
        if ( term != null && grepSpaceCode != null && !grepSpaceCode.isEmpty( ) )
        {
            return term + SEPARATOR_IN + grepSpaceCode + SEPARATOR_CLOSE;
        }
        return term;
    }

    /**
     * Gets the first available field value from a JSON node.
     *
     * @param node
     *            the JSON node
     * @param fieldNames
     *            the field names to try in order
     * @return the first found value, or null
     */
    private String getFirstFieldValue( JsonNode node, String... fieldNames )
    {
        for ( String fieldName : fieldNames )
        {
            if ( node.has( fieldName ) )
            {
                return node.get( fieldName ).asText( );
            }
        }
        if ( node.isObject( ) && node.size( ) > 0 )
        {
            return node.elements( ).next( ).asText( );
        }
        return null;
    }

    /**
     * Validates a conversation ID format (userName_UUID)
     *
     * @param conversationId
     *            the conversation ID to validate
     * @return true if the conversation ID contains a valid UUID part
     */
    private boolean isValidConversationId( String conversationId )
    {
        int lastSeparator = conversationId.lastIndexOf( '_' );
        if ( lastSeparator < 0 || lastSeparator >= conversationId.length( ) - 1 )
        {
            return false;
        }
        try
        {
            UUID.fromString( conversationId.substring( lastSeparator + 1 ) );
            return true;
        }
        catch( IllegalArgumentException e )
        {
            return false;
        }
    }
}
