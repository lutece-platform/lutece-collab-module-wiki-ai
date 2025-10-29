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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import fr.paris.lutece.portal.service.init.ShutdownService;
import fr.paris.lutece.portal.service.security.LuteceUser;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;

public class MemoryService implements ShutdownService
{
    private static final int DEFAULT_MAX_MESSAGES_PER_CONVERSATION = 20;
    private static final long DEFAULT_CONVERSATION_TTL_MINUTES = 30;
    private static final long DEFAULT_CLEANUP_INTERVAL_MINUTES = 5;
    private static final String CONVERSATION_ID_SEPARATOR = "_";
    private static final String PROPERTY_MAX_MESSAGES = "wiki.ai.chat.memory.max.messages";
    private static final String PROPERTY_CONVERSATION_TTL_MINUTES = "wiki.ai.chat.memory.ttl.minutes";
    private static final String PROPERTY_CLEANUP_INTERVAL_MINUTES = "wiki.ai.chat.memory.cleanup.interval.minutes";
    private static final String ERROR_USER_NULL = "User cannot be null";
    private static final String ERROR_CONVERSATION_ID_NULL = "Conversation ID cannot be null or empty";
    private static final String ERROR_INVALID_CONVERSATION_ID = "Invalid conversation ID";
    private static final String ERROR_ACCESS_DENIED = "Access denied: conversation does not belong to user";
    private static final String ERROR_CONVERSATION_EXPIRED = "Conversation has expired";
    private static final String LOG_ACCESS_NON_EXISTENT = "Attempted access to non-existent conversation: ";
    private static final String LOG_BY_USER = " by user: ";
    private static final String LOG_USER_ACCESS_DENIED = "User ";
    private static final String LOG_ATTEMPTED_ACCESS = " attempted to access conversation belonging to ";
    private static final String LOG_ERROR_CLEANUP = "Error during conversation cleanup";
    private static final String LOG_CLEANUP_STARTED = "Started conversation cleanup task (interval: ";
    private static final String LOG_MINUTES = " minutes)";
    private static final String LOG_CLEANED_UP = "Cleaned up ";
    private static final String LOG_EXPIRED_CONVERSATIONS = " expired conversations";
    private static final String LOG_SHUTTING_DOWN = "Shutting down MemoryService...";
    private static final String LOG_SHUTDOWN_COMPLETE = "MemoryService shutdown complete";
    private static final String SERVICE_NAME = "MemoryService";
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 5;

    private static final int MAX_MESSAGES_PER_CONVERSATION = AppPropertiesService.getPropertyInt( PROPERTY_MAX_MESSAGES,
            DEFAULT_MAX_MESSAGES_PER_CONVERSATION );
    private static final long CONVERSATION_TTL_MINUTES = AppPropertiesService.getPropertyLong( PROPERTY_CONVERSATION_TTL_MINUTES,
            DEFAULT_CONVERSATION_TTL_MINUTES );
    private static final long CLEANUP_INTERVAL_MINUTES = AppPropertiesService.getPropertyLong( PROPERTY_CLEANUP_INTERVAL_MINUTES,
            DEFAULT_CLEANUP_INTERVAL_MINUTES );

    private static class SingletonHolder
    {
        static final MemoryService INSTANCE = new MemoryService( );
    }

    private final InMemoryChatMemoryStore _chatMemoryStore;
    private final Map<String, ConversationMetadata> _conversationMetadata;
    private final ScheduledExecutorService _cleanupScheduler;

    /**
     * Metadata for a conversation including ownership and access tracking
     */
    private class ConversationMetadata
    {
        final String userName;
        long lastAccessTime;

        /**
         * Creates conversation metadata for a user
         *
         * @param userName
         *            The name of the user who owns the conversation
         */
        ConversationMetadata( String userName )
        {
            this.userName = userName;
            this.lastAccessTime = System.currentTimeMillis( );
        }

        /**
         * Updates the last access time to the current time
         */
        void updateAccessTime( )
        {
            this.lastAccessTime = System.currentTimeMillis( );
        }

        /**
         * Checks if the conversation has expired based on TTL
         *
         * @return true if the conversation has expired, false otherwise
         */
        boolean isExpired( )
        {
            long elapsedMinutes = TimeUnit.MILLISECONDS.toMinutes( System.currentTimeMillis( ) - lastAccessTime );
            return elapsedMinutes > CONVERSATION_TTL_MINUTES;
        }
    }

    /**
     * Private constructor initializing the memory service with configuration
     */
    private MemoryService( )
    {
        _chatMemoryStore = new InMemoryChatMemoryStore( );
        _conversationMetadata = new ConcurrentHashMap<>( );
        _cleanupScheduler = Executors.newSingleThreadScheduledExecutor( );

        startCleanupTask( );
    }

    /**
     * Gets the singleton instance of the MemoryService
     *
     * @return The MemoryService instance
     */
    public static MemoryService getInstance( )
    {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Creates a new conversation for the specified user
     *
     * @param user
     *            The user for whom to create the conversation
     * @return The generated conversation ID
     */
    public String createConversation( LuteceUser user )
    {
        if ( user == null )
        {
            throw new IllegalArgumentException( ERROR_USER_NULL );
        }

        String conversationId = generateConversationId( user.getName( ) );
        _conversationMetadata.put( conversationId, new ConversationMetadata( user.getName( ) ) );

        return conversationId;
    }

    /**
     * Retrieves the chat memory for a conversation with ownership validation
     *
     * @param conversationId
     *            The ID of the conversation
     * @param user
     *            The user requesting access to the conversation
     * @return The ChatMemory instance for the conversation
     */
    public ChatMemory getChatMemory( String conversationId, LuteceUser user )
    {
        if ( user == null )
        {
            throw new IllegalArgumentException( ERROR_USER_NULL );
        }

        if ( conversationId == null || conversationId.trim( ).isEmpty( ) )
        {
            throw new IllegalArgumentException( ERROR_CONVERSATION_ID_NULL );
        }

        validateConversationOwnership( conversationId, user );

        ConversationMetadata metadata = _conversationMetadata.get( conversationId );
        if ( metadata != null )
        {
            metadata.updateAccessTime( );
        }

        return MessageWindowChatMemory.builder( ).id( conversationId ).maxMessages( MAX_MESSAGES_PER_CONVERSATION ).chatMemoryStore( _chatMemoryStore )
                .build( );
    }

    /**
     * Deletes a conversation and its associated messages
     *
     * @param conversationId
     *            The ID of the conversation to delete
     */
    public void deleteConversation( String conversationId )
    {
        _conversationMetadata.remove( conversationId );
        _chatMemoryStore.deleteMessages( conversationId );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName( )
    {
        return SERVICE_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process( )
    {
        AppLogService.info( LOG_SHUTTING_DOWN );
        _cleanupScheduler.shutdown( );

        try
        {
            if ( !_cleanupScheduler.awaitTermination( SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS ) )
            {
                _cleanupScheduler.shutdownNow( );
            }
        }
        catch( InterruptedException e )
        {
            _cleanupScheduler.shutdownNow( );
            Thread.currentThread( ).interrupt( );
        }

        AppLogService.info( LOG_SHUTDOWN_COMPLETE );
    }

    /**
     * Validates that the user owns the conversation and it has not expired
     *
     * @param conversationId
     *            The ID of the conversation
     * @param user
     *            The user requesting access
     */
    private void validateConversationOwnership( String conversationId, LuteceUser user )
    {
        ConversationMetadata metadata = _conversationMetadata.get( conversationId );

        if ( metadata == null )
        {
            AppLogService.error( LOG_ACCESS_NON_EXISTENT + conversationId + LOG_BY_USER + user.getName( ) );
            throw new SecurityException( ERROR_INVALID_CONVERSATION_ID );
        }

        if ( !metadata.userName.equals( user.getName( ) ) )
        {
            AppLogService.error( LOG_USER_ACCESS_DENIED + user.getName( ) + LOG_ATTEMPTED_ACCESS + metadata.userName );
            throw new SecurityException( ERROR_ACCESS_DENIED );
        }

        if ( metadata.isExpired( ) )
        {
            deleteConversation( conversationId );
            throw new SecurityException( ERROR_CONVERSATION_EXPIRED );
        }
    }

    /**
     * Generates a unique conversation ID for a user
     *
     * @param userName
     *            The name of the user
     * @return The generated conversation ID
     */
    private String generateConversationId( String userName )
    {
        return userName + CONVERSATION_ID_SEPARATOR + UUID.randomUUID( ).toString( );
    }

    /**
     * Starts the scheduled cleanup task for expired conversations
     */
    private void startCleanupTask( )
    {
        _cleanupScheduler.scheduleAtFixedRate( ( ) -> {
            try
            {
                cleanupExpiredConversations( );
            }
            catch( Exception e )
            {
                AppLogService.error( LOG_ERROR_CLEANUP, e );
            }
        }, CLEANUP_INTERVAL_MINUTES, CLEANUP_INTERVAL_MINUTES, TimeUnit.MINUTES );

        AppLogService.info( LOG_CLEANUP_STARTED + CLEANUP_INTERVAL_MINUTES + LOG_MINUTES );
    }

    /**
     * Removes all expired conversations from memory
     */
    private void cleanupExpiredConversations( )
    {
        int removedCount = 0;

        for ( Map.Entry<String, ConversationMetadata> entry : _conversationMetadata.entrySet( ) )
        {
            if ( entry.getValue( ).isExpired( ) )
            {
                deleteConversation( entry.getKey( ) );
                removedCount++;
            }
        }

        if ( removedCount > 0 )
        {
            AppLogService.info( LOG_CLEANED_UP + removedCount + LOG_EXPIRED_CONVERSATIONS );
        }
    }
}
