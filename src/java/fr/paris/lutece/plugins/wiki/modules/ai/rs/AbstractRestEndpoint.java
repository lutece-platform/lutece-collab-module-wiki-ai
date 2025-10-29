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

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.paris.lutece.plugins.wiki.modules.ai.service.event.WikiEvent;
import fr.paris.lutece.plugins.wiki.modules.ai.service.event.WikiEventService;
import fr.paris.lutece.plugins.wiki.modules.ai.service.event.WikiEventTypes;
import fr.paris.lutece.portal.service.util.AppLogService;

/**
 * Abstract base class for REST endpoints providing common utility methods.
 */
public abstract class AbstractRestEndpoint
{
    private static final String LOG_ERROR_SENDING_ERROR_EVENT = "Error sending error event";
    private static final String LOG_ERROR_CLOSING_SINK = "Error closing event sink";

    private final WikiEventService _eventService = WikiEventService.getInstance( );

    /**
     * Creates an error response with the specified status and message.
     *
     * @param status
     *            the HTTP response status
     * @param error
     *            the error message
     * @return the error response
     */
    protected Response createErrorResponse( Response.Status status, String error )
    {
        Map<String, String> errorMap = new HashMap<>( );
        errorMap.put( WikiAIRestConstants.KEY_ERROR, error );
        return Response.status( status ).entity( errorMap ).build( );
    }

    /**
     * Creates a success response with the specified message.
     *
     * @param message
     *            the success message
     * @return the success response with OK status
     */
    protected Response createSuccessResponse( String message )
    {
        Map<String, String> resultMap = new HashMap<>( );
        resultMap.put( WikiAIRestConstants.KEY_MESSAGE, message );
        return Response.status( Response.Status.OK ).entity( resultMap ).build( );
    }

    /**
     * Handles an exception by logging it and returning an internal server error response.
     *
     * @param e
     *            the exception to handle
     * @param logContext
     *            the context message for logging
     * @return the internal server error response
     */
    protected Response handleException( Exception e, String logContext )
    {
        AppLogService.error( logContext + e.getMessage( ), e );
        return createErrorResponse( Response.Status.INTERNAL_SERVER_ERROR, WikiAIRestConstants.ERROR_INTERNAL );
    }

    /**
     * Sends an error event to the event service for a specific stream.
     *
     * @param streamId
     *            the stream identifier
     * @param errorMessage
     *            the error message to send
     */
    protected void sendErrorEvent( String streamId, String errorMessage )
    {
        WikiEvent event = new WikiEvent( streamId, WikiEventTypes.STREAM_ERROR, Map.of( WikiAIRestConstants.KEY_ERROR, errorMessage ) );
        _eventService.dispatchEvent( event );
    }

    /**
     * Closes an SSE event sink with an error message.
     *
     * @param eventSink
     *            the SSE event sink to close
     * @param sse
     *            the SSE context
     * @param errorMessage
     *            the error message to send before closing
     */
    protected void closeEventSinkWithError( SseEventSink eventSink, Sse sse, String errorMessage )
    {
        if ( eventSink != null && !eventSink.isClosed( ) )
        {
            try
            {
                String errorJson = new ObjectMapper( ).writeValueAsString( Map.of( WikiAIRestConstants.KEY_ERROR, errorMessage ) );
                OutboundSseEvent errorEvent = sse.newEventBuilder( ).name( WikiEventTypes.STREAM_ERROR ).data( errorJson )
                        .mediaType( MediaType.APPLICATION_JSON_TYPE ).build( );
                eventSink.send( errorEvent ).thenRun( eventSink::close );
            }
            catch( JsonProcessingException e )
            {
                AppLogService.error( LOG_ERROR_SENDING_ERROR_EVENT, e );
                closeEventSinkSafely( eventSink );
            }
        }
    }

    /**
     * Safely closes an event sink, catching any exceptions.
     *
     * @param eventSink
     *            the event sink to close
     */
    private void closeEventSinkSafely( SseEventSink eventSink )
    {
        try
        {
            eventSink.close( );
        }
        catch( Exception ex )
        {
            AppLogService.error( LOG_ERROR_CLOSING_SINK, ex );
        }
    }
}
