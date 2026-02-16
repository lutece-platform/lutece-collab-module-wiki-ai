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

import java.util.List;

import fr.paris.lutece.portal.service.plugin.Plugin;
import jakarta.enterprise.inject.spi.CDI;

/**
 * WikiAIIndexerActionHome provides management methods for WikiAIIndexerAction objects
 */
public final class WikiAIIndexerActionHome
{
    private static IWikiAIIndexerActionDAO _dao = CDI.current( ).select( IWikiAIIndexerActionDAO.class ).get( );

    /**
     * Private constructor
     */
    private WikiAIIndexerActionHome( )
    {
    }

    /**
     * Creates a new indexer action
     *
     * @param indexerAction the indexer action to create
     * @param plugin the plugin
     */
    public static void create( WikiAIIndexerAction indexerAction, Plugin plugin )
    {
        _dao.insert( indexerAction, plugin );
    }

    /**
     * Removes an indexer action by its identifier
     *
     * @param nId the action identifier
     * @param plugin the plugin
     */
    public static void remove( int nId, Plugin plugin )
    {
        _dao.delete( nId, plugin );
    }

    /**
     * Removes all indexer actions
     *
     * @param plugin the plugin
     */
    public static void removeAll( Plugin plugin )
    {
        _dao.deleteAll( plugin );
    }

    /**
     * Finds an indexer action by its identifier
     *
     * @param nId the action identifier
     * @param plugin the plugin
     * @return the indexer action or null if not found
     */
    public static WikiAIIndexerAction findByPrimaryKey( int nId, Plugin plugin )
    {
        return _dao.load( nId, plugin );
    }

    /**
     * Returns all indexer actions
     *
     * @param plugin the plugin
     * @return the list of indexer actions
     */
    public static List<WikiAIIndexerAction> getList( Plugin plugin )
    {
        return _dao.selectList( plugin );
    }
}
