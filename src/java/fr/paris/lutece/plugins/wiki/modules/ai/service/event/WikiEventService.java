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
package fr.paris.lutece.plugins.wiki.modules.ai.service.event;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

import fr.paris.lutece.portal.service.util.AppLogService;

/**
 * Service for managing Wiki AI events and subscriptions
 */
public class WikiEventService
{
    private static final String LOG_SUBSCRIBER_ERROR = "Error in event subscriber for streamId: ";

    private static class SingletonHolder
    {
        static final WikiEventService INSTANCE = new WikiEventService( );
    }

    private final Set<Consumer<WikiEvent>> _subscribers = new CopyOnWriteArraySet<>( );

    /**
     * Private constructor for singleton
     */
    private WikiEventService( )
    {
    }

    /**
     * Gets the singleton instance
     *
     * @return the singleton instance
     */
    public static WikiEventService getInstance( )
    {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Subscribe to wiki events
     *
     * @param consumer
     *            the event consumer
     * @return an AutoCloseable to unsubscribe
     */
    public AutoCloseable subscribe( Consumer<WikiEvent> consumer )
    {
        _subscribers.add( consumer );
        return ( ) -> _subscribers.remove( consumer );
    }

    /**
     * Dispatch an event to all subscribers
     *
     * @param event
     *            the event to dispatch
     */
    public void dispatchEvent( WikiEvent event )
    {
        for ( Consumer<WikiEvent> subscriber : _subscribers )
        {
            try
            {
                subscriber.accept( event );
            }
            catch( Exception e )
            {
                AppLogService.error( LOG_SUBSCRIBER_ERROR + event.getStreamId( ), e );
            }
        }
    }
}
