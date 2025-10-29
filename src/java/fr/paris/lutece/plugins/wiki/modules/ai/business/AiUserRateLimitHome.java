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
package fr.paris.lutece.plugins.wiki.modules.ai.business;

import fr.paris.lutece.plugins.wiki.modules.ai.service.WikiAIPlugin;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import fr.paris.lutece.portal.service.spring.SpringContextService;

import java.sql.Timestamp;
import java.util.Optional;

/**
 * This class provides instances management methods (create, find, ...) for AiUserRateLimit objects
 */
public final class AiUserRateLimitHome
{
    private static IAiUserRateLimitDAO _dao = SpringContextService.getBean( "wiki-ai.aiUserRateLimitDAO" );
    private static Plugin _plugin = PluginService.getPlugin( WikiAIPlugin.PLUGIN_NAME );

    /**
     * Private constructor
     */
    private AiUserRateLimitHome( )
    {
    }

    /**
     * Create an instance of the aiUserRateLimit class
     *
     * @param aiUserRateLimit
     *            The instance of the AiUserRateLimit which contains the informations to store
     * @return The instance of aiUserRateLimit which has been created with its primary key
     */
    public static AiUserRateLimit create( AiUserRateLimit aiUserRateLimit )
    {
        _dao.insert( aiUserRateLimit, _plugin );

        return aiUserRateLimit;
    }

    /**
     * Update of the aiUserRateLimit which is specified in parameter
     *
     * @param aiUserRateLimit
     *            The instance of the AiUserRateLimit which contains the data to store
     * @return The instance of the aiUserRateLimit which has been updated
     */
    public static AiUserRateLimit update( AiUserRateLimit aiUserRateLimit )
    {
        _dao.store( aiUserRateLimit, _plugin );

        return aiUserRateLimit;
    }

    /**
     * Remove the aiUserRateLimit whose identifier is specified in parameter
     *
     * @param nKey
     *            The aiUserRateLimit Id
     */
    public static void remove( int nKey )
    {
        _dao.delete( nKey, _plugin );
    }

    /**
     * Find the rate limit by user ID
     *
     * @param strUserId
     *            The user ID
     * @return An instance of AiUserRateLimit
     */
    public static Optional<AiUserRateLimit> findByUserId( String strUserId )
    {
        return _dao.findByUserId( strUserId, _plugin );
    }

    /**
     * Remove expired rate limit entries
     *
     * @param cutoffTimestamp
     *            The cutoff timestamp
     */
    public static void removeExpiredEntries( Timestamp cutoffTimestamp )
    {
        _dao.removeExpiredEntries( cutoffTimestamp, _plugin );
    }
}
