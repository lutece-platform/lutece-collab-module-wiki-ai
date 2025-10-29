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
package fr.paris.lutece.plugins.wiki.modules.ai.service.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.paris.lutece.plugins.wiki.modules.ai.service.event.WikiEvent;
import fr.paris.lutece.plugins.wiki.modules.ai.service.event.WikiEventService;
import fr.paris.lutece.plugins.wiki.modules.ai.service.event.WikiEventTypes;
import fr.paris.lutece.portal.service.init.ShutdownService;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Manager for Wiki AI SSE streams
 */
public class StreamingService implements ShutdownService
{
    private static final int CLEANUP_INITIAL_DELAY_SECONDS = 60;
    private static final int CLEANUP_PERIOD_SECONDS = 30;
    private static final String THREAD_NAME = "StreamingService-Cleanup";
    private static final String SERVICE_NAME = "StreamingService";
    private static final int DEFAULT_MAX_CONCURRENT_STREAMS = 50;
    private static final int DEFAULT_STREAM_EXPIRY_SECONDS = 300;
    private static final String PROPERTY_MAX_CONCURRENT_STREAMS = "wiki.ai.sse.max.concurrent.streams";
    private static final String PROPERTY_STREAM_EXPIRY_SECONDS = "wiki.ai.sse.stream.expiry.seconds";
    private static final String LOG_INITIALIZED = "StreamingService initialized with max concurrent streams: ";
    private static final String LOG_SHUTTING_DOWN = "StreamingService shutting down...";
    private static final String LOG_SHUTDOWN_COMPLETE = "StreamingService shutdown complete.";
    private static final String ERROR_REGISTER_SSE_STREAM = "Error registering SSE stream for streamId: %s";
    private static final String ERROR_SENDING_SSE_EVENT = "Error sending SSE event for stream: %s";
    private static final String ERROR_PROCESSING_EVENT = "Error processing wiki event for SSE stream: %s";
    private static final String ERROR_CLOSING_EVENT_SINK = "Error closing SSE event sink";
    private static final String EXCEPTION_FAILED_REGISTER = "Failed to register SSE stream";
    private static final long EXPIRY_TIME_MS_FACTOR = 1000;

    private static final int MAX_CONCURRENT_STREAMS = AppPropertiesService.getPropertyInt( PROPERTY_MAX_CONCURRENT_STREAMS, DEFAULT_MAX_CONCURRENT_STREAMS );
    private static final int STREAM_EXPIRY_SECONDS = AppPropertiesService.getPropertyInt( PROPERTY_STREAM_EXPIRY_SECONDS, DEFAULT_STREAM_EXPIRY_SECONDS );

    private static class SingletonHolder
    {
        static final StreamingService INSTANCE = new StreamingService( );
    }

    private final Map<String, StreamSubscription> _activeStreams = new ConcurrentHashMap<>( );
    private final ScheduledExecutorService _cleanupService = Executors.newSingleThreadScheduledExecutor( r -> {
        Thread t = new Thread( r, THREAD_NAME );
        t.setDaemon( true );
        return t;
    } );
    private final WikiEventService _eventService;
    private final ObjectMapper _objectMapper = new ObjectMapper( );

    /**
     * Private constructor for singleton
     */
    private StreamingService( )
    {
        _eventService = WikiEventService.getInstance( );
        scheduleCleanupTask( );
        AppLogService.info( LOG_INITIALIZED + MAX_CONCURRENT_STREAMS );
    }

    /**
     * Gets the singleton instance
     *
     * @return the singleton instance
     */
    public static StreamingService getInstance( )
    {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Registers an SSE stream for a given stream ID
     *
     * @param strStreamId
     *            the stream identifier
     * @param strUserId
     *            the user identifier
     * @param eventSink
     *            the SSE event sink
     * @param sse
     *            the SSE instance
     * @return an AutoCloseable to unregister the stream
     */
    public AutoCloseable registerSseStream( String strStreamId, String strUserId, SseEventSink eventSink, Sse sse )
    {
        try
        {
            Consumer<WikiEvent> eventConsumer = createEventConsumer( strStreamId, eventSink, sse );
            AutoCloseable eventSubscription = _eventService.subscribe( eventConsumer );

            StreamSubscription subscription = new StreamSubscription( strStreamId, strUserId, eventSink, eventSubscription );
            _activeStreams.put( strStreamId, subscription );

            return ( ) -> unregisterSseStream( strStreamId );
        }
        catch( Exception e )
        {
            AppLogService.error( String.format( ERROR_REGISTER_SSE_STREAM, strStreamId ), e );
            closeEventSink( eventSink );
            throw new RuntimeException( EXCEPTION_FAILED_REGISTER, e );
        }
    }

    /**
     * Checks if a new stream can be created
     *
     * @return true if a new stream can be created
     */
    public boolean canCreateNewStream( )
    {
        return _activeStreams.size( ) < MAX_CONCURRENT_STREAMS;
    }

    @Override
    public String getName( )
    {
        return SERVICE_NAME;
    }

    @Override
    public void process( )
    {
        AppLogService.info( LOG_SHUTTING_DOWN );
        _activeStreams.values( ).forEach( StreamSubscription::close );
        _activeStreams.clear( );
        _cleanupService.shutdown( );
        AppLogService.info( LOG_SHUTDOWN_COMPLETE );
    }

    /**
     * Schedules the cleanup task for expired streams
     */
    private void scheduleCleanupTask( )
    {
        _cleanupService.scheduleAtFixedRate( this::cleanupExpiredStreams, CLEANUP_INITIAL_DELAY_SECONDS, CLEANUP_PERIOD_SECONDS, TimeUnit.SECONDS );
    }

    /**
     * Creates an event consumer for wiki events
     *
     * @param strStreamId
     *            the stream identifier
     * @param eventSink
     *            the SSE event sink
     * @param sse
     *            the SSE instance
     * @return the event consumer
     */
    private Consumer<WikiEvent> createEventConsumer( String strStreamId, SseEventSink eventSink, Sse sse )
    {
        return event -> {
            if ( strStreamId.equals( event.getStreamId( ) ) )
            {
                handleWikiEvent( strStreamId, eventSink, sse, event );
            }
        };
    }

    /**
     * Handles a wiki event
     *
     * @param strStreamId
     *            the stream identifier
     * @param eventSink
     *            the SSE event sink
     * @param sse
     *            the SSE instance
     * @param event
     *            the wiki event
     */
    private void handleWikiEvent( String strStreamId, SseEventSink eventSink, Sse sse, WikiEvent event )
    {
        StreamSubscription subscription = _activeStreams.get( strStreamId );
        if ( subscription == null || eventSink.isClosed( ) )
        {
            return;
        }

        try
        {
            OutboundSseEvent sseEvent = createSseEvent( sse, event );
            sendSseEvent( strStreamId, eventSink, subscription, event, sseEvent );
        }
        catch( Exception e )
        {
            AppLogService.error( String.format( ERROR_PROCESSING_EVENT, strStreamId ), e );
            unregisterSseStream( strStreamId );
        }
    }

    /**
     * Creates an SSE event from a wiki event
     *
     * @param sse
     *            the SSE instance
     * @param event
     *            the wiki event
     * @return the outbound SSE event
     * @throws Exception
     *             if serialization fails
     */
    private OutboundSseEvent createSseEvent( Sse sse, WikiEvent event ) throws Exception
    {
        String eventData = _objectMapper.writeValueAsString( event.getPayload( ) );
        return sse.newEventBuilder( ).name( event.getEventType( ) ).data( eventData ).mediaType( MediaType.APPLICATION_JSON_TYPE ).build( );
    }

    /**
     * Sends an SSE event
     *
     * @param strStreamId
     *            the stream identifier
     * @param eventSink
     *            the SSE event sink
     * @param subscription
     *            the stream subscription
     * @param event
     *            the wiki event
     * @param sseEvent
     *            the outbound SSE event
     */
    private void sendSseEvent( String strStreamId, SseEventSink eventSink, StreamSubscription subscription, WikiEvent event, OutboundSseEvent sseEvent )
    {
        eventSink.send( sseEvent ).thenRun( ( ) -> handleSuccessfulSend( strStreamId, subscription, event ) )
                .exceptionally( throwable -> handleFailedSend( strStreamId, throwable ) );
    }

    /**
     * Handles successful event send
     *
     * @param strStreamId
     *            the stream identifier
     * @param subscription
     *            the stream subscription
     * @param event
     *            the wiki event
     */
    private void handleSuccessfulSend( String strStreamId, StreamSubscription subscription, WikiEvent event )
    {
        subscription.updateLastAccessed( );
        if ( isTerminalEvent( event ) )
        {
            unregisterSseStream( strStreamId );
        }
    }

    /**
     * Handles failed event send
     *
     * @param strStreamId
     *            the stream identifier
     * @param throwable
     *            the exception that occurred
     * @return null
     */
    private Void handleFailedSend( String strStreamId, Throwable throwable )
    {
        AppLogService.error( String.format( ERROR_SENDING_SSE_EVENT, strStreamId ), throwable );
        unregisterSseStream( strStreamId );
        return null;
    }

    /**
     * Checks if an event is a terminal event
     *
     * @param event
     *            the wiki event
     * @return true if the event is terminal
     */
    private boolean isTerminalEvent( WikiEvent event )
    {
        String eventType = event.getEventType( );
        return WikiEventTypes.STREAM_COMPLETED.equals( eventType ) || WikiEventTypes.STREAM_ERROR.equals( eventType );
    }

    /**
     * Unregisters an SSE stream
     *
     * @param strStreamId
     *            the stream identifier
     */
    private void unregisterSseStream( String strStreamId )
    {
        StreamSubscription subscription = _activeStreams.remove( strStreamId );
        if ( subscription != null )
        {
            subscription.close( );
        }
    }

    /**
     * Closes an event sink
     *
     * @param eventSink
     *            the event sink to close
     */
    private void closeEventSink( SseEventSink eventSink )
    {
        if ( eventSink != null && !eventSink.isClosed( ) )
        {
            try
            {
                eventSink.close( );
            }
            catch( Exception e )
            {
                AppLogService.error( ERROR_CLOSING_EVENT_SINK, e );
            }
        }
    }

    /**
     * Cleans up expired streams
     */
    private void cleanupExpiredStreams( )
    {
        long now = System.currentTimeMillis( );
        long expiryTimeMs = STREAM_EXPIRY_SECONDS * EXPIRY_TIME_MS_FACTOR;

        _activeStreams.entrySet( ).removeIf( entry -> {
            StreamSubscription subscription = entry.getValue( );
            boolean expired = ( now - subscription.getLastAccessed( ) ) > expiryTimeMs;

            if ( expired )
            {
                subscription.close( );
            }
            return expired;
        } );
    }

    /**
     * Inner class representing a stream subscription
     */
    private static class StreamSubscription
    {
        private final SseEventSink _eventSink;
        private final AutoCloseable _eventSubscription;
        private long _lLastAccessed;

        StreamSubscription( String strStreamId, String strUserId, SseEventSink eventSink, AutoCloseable eventSubscription )
        {
            _eventSink = eventSink;
            _eventSubscription = eventSubscription;
            _lLastAccessed = System.currentTimeMillis( );
        }

        long getLastAccessed( )
        {
            return _lLastAccessed;
        }

        void updateLastAccessed( )
        {
            _lLastAccessed = System.currentTimeMillis( );
        }

        void close( )
        {
            try
            {
                if ( _eventSubscription != null )
                {
                    _eventSubscription.close( );
                }
            }
            catch( Exception e )
            {
                AppLogService.error( "Error closing event subscription", e );
            }

            try
            {
                if ( _eventSink != null && !_eventSink.isClosed( ) )
                {
                    _eventSink.close( );
                }
            }
            catch( Exception e )
            {
                AppLogService.error( "Error closing event sink", e );
            }
        }
    }
}
