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
package fr.paris.lutece.plugins.wiki.modules.ai.service.rate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;

import fr.paris.lutece.plugins.wiki.modules.ai.business.AiRateLimitStatus;
import fr.paris.lutece.plugins.wiki.modules.ai.business.AiUserRateLimit;
import fr.paris.lutece.plugins.wiki.modules.ai.business.AiUserRateLimitHome;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AiRateLimitService
{
    private static final String PROPERTY_MESSAGES_PER_DAY = "wiki.ai.rateLimit.messagesPerDay";
    private static final int DEFAULT_MESSAGES_PER_DAY = 100;
    private static final String RATE_LIMIT_ERROR_MESSAGE = "Vous avez atteint la limite de %d messages IA par jour. Votre quota sera réinitialisé le %s";
    private static final String CLEANUP_COMPLETED_MESSAGE = "Nettoyage des entrées de rate limit expirées terminé";
    private static final String RATE_LIMIT_CHECK_ERROR_MESSAGE = "Erreur lors de la vérification du rate limit";
    private static final String CLEANUP_ERROR_MESSAGE = "Erreur lors du nettoyage des entrées de rate limit";
    private static final String INTERNAL_SERVER_ERROR_MESSAGE = "Erreur interne du serveur";
    private static final String LOG_ERROR_GETTING_STATUS = "Error getting rate limit status";
    private static final String DATE_PATTERN = "dd/MM/yyyy à HH:mm";
    private static final int HOURS_IN_DAY = 24;
    private static final int INITIAL_MESSAGE_COUNT = 1;

    private static final int DAILY_LIMIT = AppPropertiesService.getPropertyInt( PROPERTY_MESSAGES_PER_DAY, DEFAULT_MESSAGES_PER_DAY );

    /**
     * Checks if a user has exceeded their rate limit
     * 
     * @param userId
     *            The ID of the user to check
     * @return A RateLimitResult indicating if the request is allowed
     */
    public RateLimitResult checkRateLimit( String userId )
    {
        try
        {
            if ( DAILY_LIMIT <= 0 )
            {
                return new RateLimitResult( true, null );
            }

            return processRateLimitCheck( userId, DAILY_LIMIT );
        }
        catch( Exception e )
        {
            AppLogService.error( RATE_LIMIT_CHECK_ERROR_MESSAGE, e );
            return new RateLimitResult( false, INTERNAL_SERVER_ERROR_MESSAGE );
        }
    }

    /**
     * Retrieves the current rate limit status for a user
     * 
     * @param userId
     *            The ID of the user
     * @return The rate limit status containing limit, remaining, and reset time
     */
    public AiRateLimitStatus getRateLimitStatus( String userId )
    {
        try
        {
            if ( DAILY_LIMIT <= 0 )
            {
                return new AiRateLimitStatus( Integer.MAX_VALUE, 0, null );
            }

            Optional<AiUserRateLimit> optRateLimit = AiUserRateLimitHome.findByUserId( userId );

            if ( optRateLimit.isEmpty( ) )
            {
                return new AiRateLimitStatus( DAILY_LIMIT, DAILY_LIMIT, null );
            }

            AiUserRateLimit rateLimit = optRateLimit.get( );
            LocalDateTime firstMessageTime = rateLimit.getDateFirstMessage( ).toLocalDateTime( );
            LocalDateTime currentTime = LocalDateTime.now( );
            long hoursDiff = ChronoUnit.HOURS.between( firstMessageTime, currentTime );

            if ( hoursDiff >= HOURS_IN_DAY )
            {
                AiUserRateLimitHome.remove( rateLimit.getId( ) );
                return new AiRateLimitStatus( DAILY_LIMIT, DAILY_LIMIT, null );
            }

            int remaining = Math.max( 0, DAILY_LIMIT - rateLimit.getMessageCount( ) );
            LocalDateTime resetTime = firstMessageTime.plusHours( HOURS_IN_DAY );

            return new AiRateLimitStatus( DAILY_LIMIT, remaining, Timestamp.valueOf( resetTime ) );
        }
        catch( Exception e )
        {
            AppLogService.error( LOG_ERROR_GETTING_STATUS, e );
            return new AiRateLimitStatus( DEFAULT_MESSAGES_PER_DAY, DEFAULT_MESSAGES_PER_DAY, null );
        }
    }

    /**
     * Cleans up expired rate limit entries older than 24 hours
     */
    public void cleanExpiredEntries( )
    {
        try
        {
            LocalDateTime cutoffTime = LocalDateTime.now( ).minusHours( HOURS_IN_DAY );
            Timestamp cutoffTimestamp = Timestamp.valueOf( cutoffTime );

            AiUserRateLimitHome.removeExpiredEntries( cutoffTimestamp );
            AppLogService.info( CLEANUP_COMPLETED_MESSAGE );
        }
        catch( Exception e )
        {
            AppLogService.error( CLEANUP_ERROR_MESSAGE, e );
        }
    }

    /**
     * Processes the rate limit check for a user
     * 
     * @param userId
     *            The ID of the user
     * @param dailyLimit
     *            The daily message limit
     * @return A RateLimitResult indicating if the request is allowed
     */
    private RateLimitResult processRateLimitCheck( String userId, int dailyLimit )
    {
        Optional<AiUserRateLimit> optRateLimit = AiUserRateLimitHome.findByUserId( userId );
        Timestamp now = Timestamp.valueOf( LocalDateTime.now( ) );

        if ( optRateLimit.isEmpty( ) )
        {
            return createNewRateLimit( userId, now );
        }

        return processExistingRateLimit( optRateLimit.get( ), dailyLimit, now );
    }

    /**
     * Creates a new rate limit entry for a user
     * 
     * @param userId
     *            The ID of the user
     * @param now
     *            The current timestamp
     * @return A RateLimitResult allowing the request
     */
    private RateLimitResult createNewRateLimit( String userId, Timestamp now )
    {
        AiUserRateLimit rateLimit = new AiUserRateLimit( );
        rateLimit.setUserId( userId );
        rateLimit.setMessageCount( INITIAL_MESSAGE_COUNT );
        rateLimit.setDateFirstMessage( now );

        AiUserRateLimitHome.create( rateLimit );

        return new RateLimitResult( true, null );
    }

    /**
     * Processes an existing rate limit entry
     * 
     * @param rateLimit
     *            The existing rate limit
     * @param dailyLimit
     *            The daily message limit
     * @param now
     *            The current timestamp
     * @return A RateLimitResult indicating if the request is allowed
     */
    private RateLimitResult processExistingRateLimit( AiUserRateLimit rateLimit, int dailyLimit, Timestamp now )
    {
        LocalDateTime firstMessageTime = rateLimit.getDateFirstMessage( ).toLocalDateTime( );
        LocalDateTime currentTime = now.toLocalDateTime( );
        long hoursDiff = ChronoUnit.HOURS.between( firstMessageTime, currentTime );

        if ( hoursDiff >= HOURS_IN_DAY )
        {
            return resetRateLimit( rateLimit, now );
        }

        if ( rateLimit.getMessageCount( ) >= dailyLimit )
        {
            return createRateLimitExceededResult( firstMessageTime, dailyLimit );
        }

        return incrementMessageCount( rateLimit );
    }

    /**
     * Resets a user's rate limit after 24 hours
     * 
     * @param rateLimit
     *            The rate limit to reset
     * @param now
     *            The current timestamp
     * @return A RateLimitResult allowing the request
     */
    private RateLimitResult resetRateLimit( AiUserRateLimit rateLimit, Timestamp now )
    {
        rateLimit.setMessageCount( INITIAL_MESSAGE_COUNT );
        rateLimit.setDateFirstMessage( now );
        AiUserRateLimitHome.update( rateLimit );

        return new RateLimitResult( true, null );
    }

    /**
     * Creates a result indicating the rate limit has been exceeded
     * 
     * @param firstMessageTime
     *            The time of the first message in the current period
     * @param dailyLimit
     *            The daily message limit
     * @return A RateLimitResult denying the request with an error message
     */
    private RateLimitResult createRateLimitExceededResult( LocalDateTime firstMessageTime, int dailyLimit )
    {
        LocalDateTime resetTime = firstMessageTime.plusHours( HOURS_IN_DAY );
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern( DATE_PATTERN, Locale.FRANCE );
        String formattedResetTime = resetTime.format( formatter );
        String errorMessage = String.format( RATE_LIMIT_ERROR_MESSAGE, dailyLimit, formattedResetTime );

        return new RateLimitResult( false, errorMessage );
    }

    /**
     * Increments the message count for a user's rate limit
     * 
     * @param rateLimit
     *            The rate limit to increment
     * @return A RateLimitResult allowing the request
     */
    private RateLimitResult incrementMessageCount( AiUserRateLimit rateLimit )
    {
        rateLimit.setMessageCount( rateLimit.getMessageCount( ) + 1 );
        AiUserRateLimitHome.update( rateLimit );

        return new RateLimitResult( true, null );
    }
}
