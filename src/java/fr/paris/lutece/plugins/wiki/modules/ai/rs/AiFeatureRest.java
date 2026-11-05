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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;

import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import fr.paris.lutece.plugins.wiki.modules.ai.business.AiFeature;
import fr.paris.lutece.plugins.wiki.modules.ai.business.AiRateLimitStatus;
import fr.paris.lutece.plugins.wiki.modules.ai.service.chat.AiFeatureService;
import fr.paris.lutece.plugins.wiki.modules.ai.service.chat.EditStep;
import fr.paris.lutece.plugins.wiki.modules.ai.service.chat.StreamingService;
import fr.paris.lutece.plugins.wiki.modules.ai.service.event.WikiEvent;
import fr.paris.lutece.plugins.wiki.modules.ai.service.event.WikiEventService;
import fr.paris.lutece.plugins.wiki.modules.ai.service.event.WikiEventTypes;
import fr.paris.lutece.plugins.wiki.modules.ai.service.rate.AiRateLimitService;
import fr.paris.lutece.plugins.wiki.modules.ai.service.rate.RateLimitResult;
import fr.paris.lutece.portal.service.security.LuteceUser;
import fr.paris.lutece.portal.service.security.SecurityService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import fr.paris.lutece.portal.service.util.AppLogService;

/**
 * REST endpoint for AI feature operations including streaming responses.
 */
@RequestScoped
@Path( "wiki/ai/features" )
public class AiFeatureRest extends AbstractRestEndpoint
{
    private static final String LOG_ERROR_AI_FEATURES = "Error in AI features REST API";
    private static final String LOG_FEATURE_NOT_FOUND = "Feature not found for stream: {}";
    private static final String LOG_AUTH_FAILED = "Authentication failed for AI feature: {}";
    private static final String LOG_SSE_ERROR = "Error setting up SSE stream for streamId: {}";
    private static final String LOG_ERROR_SENDING_TOKEN = "Error sending token event";
    private static final String LOG_ERROR_SENDING_COMPLETED = "Error sending completed event";
    private static final String LOG_ERROR_DURING_STREAMING = "Error during streaming: {}";
    private static final String LOG_ERROR_EXECUTING_STREAMING = "Error executing streaming feature: {}";

    private static final String KEY_STATUS = "status";
    private static final String KEY_FEATURE_ID = "feature_id";
    private static final String KEY_TEXT = "text";
    private static final String KEY_STEPS = "steps";
    private static final String KEY_INSTRUCTION = "instruction";
    private static final String KEY_RESULT = "result";
    private static final String PROPERTY_EDIT_MAX_TEXT_LENGTH = "wiki.ai.edit.maxTextLength";
    private static final String PROPERTY_EDIT_MAX_INSTRUCTION_LENGTH = "wiki.ai.edit.maxInstructionLength";
    private static final String PROPERTY_EDIT_MAX_STEPS = "wiki.ai.edit.maxSteps";
    private static final int DEFAULT_EDIT_MAX_TEXT_LENGTH = 20000;
    private static final int DEFAULT_EDIT_MAX_INSTRUCTION_LENGTH = 1000;
    private static final int DEFAULT_EDIT_MAX_STEPS = 10;
    private static final String VALUE_STATUS_DONE = "done";

    @Context
    private HttpServletRequest _request;

    @Inject
    private AiFeatureService _featureService;
    @Inject
    private AiRateLimitService _rateLimitService;
    @Inject
    private StreamingService _streamingService;
    @Inject
    private WikiEventService _eventService;

    /**
     * Retrieves available AI features, optionally filtered by type.
     *
     * @param strType
     *            optional feature type filter
     * @return response containing the list of features or error details
     */
    @GET
    @Produces( MediaType.APPLICATION_JSON )
    public Response getFeatures( @QueryParam( "type" ) String strType )
    {
        LuteceUser user = authenticate( );
        if ( user == null )
        {
            return createUnauthorizedResponse( );
        }

        try
        {
            List<AiFeature> features;
            if ( strType != null && !strType.trim( ).isEmpty( ) )
            {
                features = _featureService.getFeaturesByType( strType );
            }
            else
            {
                features = _featureService.getAllFeatures( );
            }

            features = features.stream( ).filter( AiFeature::isActive ).collect( Collectors.toList( ) );
            return Response.status( Response.Status.OK ).entity( features ).build( );
        }
        catch( Exception e )
        {
            return handleException( e, LOG_ERROR_AI_FEATURES );
        }
    }

    /**
     * Retrieves the rate limit status for the current user.
     *
     * @return response containing the rate limit status or error details
     */
    @GET
    @Path( "ratelimit" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getRateLimitStatus( )
    {
        LuteceUser user = authenticate( );
        if ( user == null )
        {
            return createUnauthorizedResponse( );
        }

        try
        {
            AiRateLimitStatus status = _rateLimitService.getRateLimitStatus( user.getName( ) );
            return Response.status( Response.Status.OK ).entity( status ).build( );
        }
        catch( Exception e )
        {
            return handleException( e, LOG_ERROR_AI_FEATURES );
        }
    }

    /**
     * Initializes a streaming AI feature execution.
     *
     * @param request
     *            the request containing feature_id and text
     * @return response containing the stream_id or error details
     */
    @POST
    @Path( "stream/init" )
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Response initStreamingFeature( Map<String, Object> request )
    {
        LuteceUser user = authenticate( );
        if ( user == null )
        {
            return createUnauthorizedResponse( );
        }

        RateLimitResult rateLimitResult = _rateLimitService.checkRateLimit( user.getName( ) );
        if ( !rateLimitResult.isAllowed( ) )
        {
            return createErrorResponse( Response.Status.TOO_MANY_REQUESTS, rateLimitResult.getErrorMessage( ) );
        }

        Integer nFeatureId = request.get( KEY_FEATURE_ID ) instanceof Number number ? number.intValue( ) : null;
        String strText = (String) request.get( KEY_TEXT );

        if ( nFeatureId == null )
        {
            return createErrorResponse( Response.Status.BAD_REQUEST, WikiAIRestConstants.ERROR_FEATURE_ID_REQUIRED );
        }

        if ( strText == null || strText.trim( ).isEmpty( ) )
        {
            return createErrorResponse( Response.Status.BAD_REQUEST, WikiAIRestConstants.ERROR_TEXT_REQUIRED );
        }

        return startStream( user, handler -> _featureService.executeFeature( nFeatureId, strText, handler ) );
    }

    /**
     * Initializes a streaming AI edit of a selection: a feature or an instruction applied to the selection, then refined step by step.
     *
     * @param request
     *            the request containing the selected text and the steps, the last one being the request to run
     * @return response containing the stream_id or error details
     */
    @POST
    @Path( "stream/edit" )
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Response initStreamingEdit( Map<String, Object> request )
    {
        LuteceUser user = authenticate( );
        if ( user == null )
        {
            return createUnauthorizedResponse( );
        }

        String strText = request.get( KEY_TEXT ) instanceof String text ? text : null;
        if ( strText == null || strText.isBlank( ) )
        {
            return createErrorResponse( Response.Status.BAD_REQUEST, WikiAIRestConstants.ERROR_TEXT_REQUIRED );
        }
        int nMaxTextLength = AppPropertiesService.getPropertyInt( PROPERTY_EDIT_MAX_TEXT_LENGTH, DEFAULT_EDIT_MAX_TEXT_LENGTH );
        if ( strText.length( ) > nMaxTextLength )
        {
            return createErrorResponse( Response.Status.BAD_REQUEST, WikiAIRestConstants.ERROR_TEXT_TOO_LONG );
        }

        List<EditStep> steps = readSteps( request.get( KEY_STEPS ), nMaxTextLength );
        if ( steps == null )
        {
            return createErrorResponse( Response.Status.BAD_REQUEST, WikiAIRestConstants.ERROR_INVALID_EDIT_STEPS );
        }

        RateLimitResult rateLimitResult = _rateLimitService.checkRateLimit( user.getName( ) );
        if ( !rateLimitResult.isAllowed( ) )
        {
            return createErrorResponse( Response.Status.TOO_MANY_REQUESTS, rateLimitResult.getErrorMessage( ) );
        }
        return startStream( user, handler -> _featureService.editSelection( strText, steps, handler ) );
    }

    /**
     * Prepares a stream that runs a model call, unless the server has too many streams open.
     *
     * @param user
     *            the user
     * @param call
     *            the model call, given the handler of its answer
     * @return response containing the stream_id, or a service unavailable error
     */
    private Response startStream( LuteceUser user, Consumer<StreamingChatResponseHandler> call )
    {
        if ( !_streamingService.canCreateNewStream( ) )
        {
            return createErrorResponse( Response.Status.SERVICE_UNAVAILABLE, WikiAIRestConstants.ERROR_HIGH_TRAFFIC );
        }

        String streamId = UUID.randomUUID( ).toString( );
        _streamingService.prepareStream( streamId, user.getName( ), ( ) -> stream( streamId, call ) );
        return Response.status( Response.Status.OK ).entity( Map.of( WikiAIRestConstants.KEY_STREAM_ID, streamId ) ).build( );
    }

    /**
     * Reads the steps of an AI edit: each one has a feature or an instruction, every step but the last one has its answer.
     *
     * @param value
     *            the steps of the request
     * @param nMaxTextLength
     *            the maximum length of an answer
     * @return the steps, or null when they are invalid
     */
    private static List<EditStep> readSteps( Object value, int nMaxTextLength )
    {
        if ( !( value instanceof List<?> list ) || list.isEmpty( )
                || list.size( ) > AppPropertiesService.getPropertyInt( PROPERTY_EDIT_MAX_STEPS, DEFAULT_EDIT_MAX_STEPS ) )
        {
            return null;
        }
        int nMaxInstructionLength = AppPropertiesService.getPropertyInt( PROPERTY_EDIT_MAX_INSTRUCTION_LENGTH, DEFAULT_EDIT_MAX_INSTRUCTION_LENGTH );
        List<EditStep> steps = new ArrayList<>( );
        for ( int i = 0; i < list.size( ); i++ )
        {
            if ( !( list.get( i ) instanceof Map<?, ?> map ) )
            {
                return null;
            }
            Integer nFeatureId = map.get( KEY_FEATURE_ID ) instanceof Number number ? number.intValue( ) : null;
            String strInstruction = map.get( KEY_INSTRUCTION ) instanceof String instruction && !instruction.isBlank( ) ? instruction.strip( ) : null;
            String strResult = map.get( KEY_RESULT ) instanceof String result ? result : null;
            boolean bLast = i == list.size( ) - 1;
            if ( ( nFeatureId == null ) == ( strInstruction == null ) || ( strResult == null ) != bLast )
            {
                return null;
            }
            if ( ( strInstruction != null && strInstruction.length( ) > nMaxInstructionLength ) || ( strResult != null && strResult.length( ) > nMaxTextLength ) )
            {
                return null;
            }
            steps.add( new EditStep( nFeatureId, strInstruction, strResult ) );
        }
        return steps;
    }

    /**
     * Establishes an SSE connection for receiving streaming feature events.
     *
     * @param streamId
     *            the stream identifier
     * @param eventSink
     *            the SSE event sink
     * @param sse
     *            the SSE context
     */
    @GET
    @Path( "stream/events/{streamId}" )
    @Produces( MediaType.SERVER_SENT_EVENTS )
    public void getStreamingFeatureEvents( @PathParam( "streamId" ) String streamId, @Context SseEventSink eventSink, @Context Sse sse )
    {
        LuteceUser user = authenticate( );
        if ( user == null )
        {
            AppLogService.error( LOG_AUTH_FAILED, streamId );
            closeEventSinkWithError( eventSink, sse, WikiAIRestConstants.ERROR_AUTHENTICATION_FAILED );
            return;
        }

        try
        {
            _streamingService.registerSseStream( streamId, user.getName( ), eventSink, sse );
        }
        catch( Exception e )
        {
            AppLogService.error( LOG_SSE_ERROR, streamId, e );
            closeEventSinkWithError( eventSink, sse, WikiAIRestConstants.ERROR_STREAM_SETUP_FAILED );
        }
    }

    /**
     * Runs a model call and sends its tokens, its end or its error as events of a stream.
     *
     * @param streamId
     *            the stream identifier
     * @param call
     *            the model call, given the handler of its answer
     */
    private void stream( String streamId, Consumer<StreamingChatResponseHandler> call )
    {
        try
        {
            call.accept( new StreamingChatResponseHandler( )
            {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public void onPartialResponse( String token )
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
                 * {@inheritDoc}
                 */
                @Override
                public void onCompleteResponse( ChatResponse response )
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
                 * {@inheritDoc}
                 */
                @Override
                public void onError( Throwable error )
                {
                    AppLogService.error( LOG_ERROR_DURING_STREAMING, error.getMessage( ), error );
                    sendErrorEvent( streamId, error.getMessage( ) );
                }
            } );
        }
        catch( IllegalArgumentException e )
        {
            AppLogService.error( LOG_FEATURE_NOT_FOUND, streamId, e );
            sendErrorEvent( streamId, WikiAIRestConstants.ERROR_FEATURE_NOT_FOUND );
        }
        catch( Exception e )
        {
            AppLogService.error( LOG_ERROR_EXECUTING_STREAMING, e.getMessage( ), e );
            sendErrorEvent( streamId, e.getMessage( ) );
        }
    }

    /**
     * Authenticates the user.
     *
     * @return the authenticated user, or null when no user is signed in
     */
    private LuteceUser authenticate( )
    {
        return SecurityService.getInstance( ).getRegisteredUser( _request );
    }

    /**
     * Creates the response returned when no user is signed in.
     *
     * @return response with the unauthorized status and message
     */
    private Response createUnauthorizedResponse( )
    {
        return createErrorResponse( Response.Status.UNAUTHORIZED, WikiAIRestConstants.ERROR_NOT_AUTHENTICATED );
    }
}
