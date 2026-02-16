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
import fr.paris.lutece.plugins.wiki.modules.ai.service.chat.StreamingService;
import fr.paris.lutece.plugins.wiki.modules.ai.service.event.WikiEvent;
import fr.paris.lutece.plugins.wiki.modules.ai.service.event.WikiEventService;
import fr.paris.lutece.plugins.wiki.modules.ai.service.event.WikiEventTypes;
import fr.paris.lutece.plugins.wiki.modules.ai.service.rate.AiRateLimitService;
import fr.paris.lutece.plugins.wiki.modules.ai.service.rate.RateLimitResult;
import fr.paris.lutece.plugins.wiki.service.security.WikiAccessControlService;
import fr.paris.lutece.portal.service.security.LuteceUser;
import fr.paris.lutece.portal.service.security.SecurityService;
import fr.paris.lutece.portal.service.util.AppLogService;

/**
 * REST endpoint for AI feature operations including streaming responses.
 */
@RequestScoped
@Path( "wiki/ai/features" )
public class AiFeatureRest extends AbstractRestEndpoint
{
    private static final String LOG_ERROR_AI_FEATURES = "Error in AI features REST API";
    private static final String LOG_FEATURE_NOT_FOUND = "Feature not found: {}";
    private static final String LOG_AUTH_FAILED = "Authentication failed for AI feature: {}";
    private static final String LOG_SSE_ERROR = "Error setting up SSE stream for streamId: {}";
    private static final String LOG_ERROR_SENDING_TOKEN = "Error sending token event";
    private static final String LOG_ERROR_SENDING_COMPLETED = "Error sending completed event";
    private static final String LOG_ERROR_DURING_STREAMING = "Error during streaming: {}";
    private static final String LOG_ERROR_EXECUTING_STREAMING = "Error executing streaming feature: {}";

    private static final String KEY_STATUS = "status";
    private static final String KEY_FEATURE_ID = "feature_id";
    private static final String KEY_TEXT = "text";
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
        LuteceUser user = authenticateAndAuthorize( );
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
        LuteceUser user = authenticateAndAuthorize( );
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
        LuteceUser user = authenticateAndAuthorize( );
        if ( user == null )
        {
            return createUnauthorizedResponse( );
        }

        RateLimitResult rateLimitResult = _rateLimitService.checkRateLimit( user.getName( ) );
        if ( !rateLimitResult.isAllowed( ) )
        {
            return createErrorResponse( Response.Status.TOO_MANY_REQUESTS, rateLimitResult.getErrorMessage( ) );
        }

        Integer nFeatureId = (Integer) request.get( KEY_FEATURE_ID );
        String strText = (String) request.get( KEY_TEXT );

        if ( nFeatureId == null )
        {
            return createErrorResponse( Response.Status.BAD_REQUEST, WikiAIRestConstants.ERROR_FEATURE_ID_REQUIRED );
        }

        if ( strText == null || strText.trim( ).isEmpty( ) )
        {
            return createErrorResponse( Response.Status.BAD_REQUEST, WikiAIRestConstants.ERROR_TEXT_REQUIRED );
        }

        if ( !_streamingService.canCreateNewStream( ) )
        {
            return createErrorResponse( Response.Status.SERVICE_UNAVAILABLE, WikiAIRestConstants.ERROR_HIGH_TRAFFIC );
        }

        String streamId = UUID.randomUUID( ).toString( );
        executeStreamingFeature( nFeatureId, strText, streamId );

        return Response.status( Response.Status.OK ).entity( Map.of( WikiAIRestConstants.KEY_STREAM_ID, streamId ) ).build( );
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
        LuteceUser user = authenticateAndAuthorize( );
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
     * Executes the streaming feature asynchronously.
     *
     * @param nFeatureId
     *            the feature identifier
     * @param strText
     *            the input text
     * @param streamId
     *            the stream identifier
     */
    private void executeStreamingFeature( int nFeatureId, String strText, String streamId )
    {
        CompletableFuture.runAsync( ( ) -> {
            try
            {
                _featureService.executeFeature( nFeatureId, strText, new StreamingChatResponseHandler( )
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
                AppLogService.error( LOG_FEATURE_NOT_FOUND, nFeatureId, e );
                sendErrorEvent( streamId, WikiAIRestConstants.ERROR_FEATURE_NOT_FOUND );
            }
            catch( Exception e )
            {
                AppLogService.error( LOG_ERROR_EXECUTING_STREAMING, e.getMessage( ), e );
                sendErrorEvent( streamId, e.getMessage( ) );
            }
        } );
    }

    /**
     * Authenticates the user and checks authorization.
     *
     * @return the authenticated user, or null if not authorized
     */
    private LuteceUser authenticateAndAuthorize( )
    {
        LuteceUser user = SecurityService.getInstance( ).getRegisteredUser( _request );
        if ( user == null )
        {
            return null;
        }

        if ( !WikiAccessControlService.canCreateBook( user ) )
        {
            return null;
        }

        return user;
    }

    /**
     * Creates an unauthorized response based on the authentication status.
     *
     * @return response with appropriate error status and message
     */
    private Response createUnauthorizedResponse( )
    {
        LuteceUser user = SecurityService.getInstance( ).getRegisteredUser( _request );
        if ( user == null )
        {
            return createErrorResponse( Response.Status.UNAUTHORIZED, WikiAIRestConstants.ERROR_NOT_AUTHENTICATED );
        }
        return createErrorResponse( Response.Status.FORBIDDEN, WikiAIRestConstants.ERROR_NOT_AUTHORIZED );
    }
}
