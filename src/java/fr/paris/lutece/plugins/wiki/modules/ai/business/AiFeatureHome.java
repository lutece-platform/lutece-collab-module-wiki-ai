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

import java.util.List;
import java.util.Optional;

/**
 * AiFeatureHome factory class for AI feature business operations
 */
public final class AiFeatureHome
{
    private static final IAiFeatureDAO _dao = SpringContextService.getBean( "wiki-ai.aiFeatureDAO" );
    private static Plugin _plugin;

    /**
     * Private constructor to prevent instantiation
     */
    private AiFeatureHome( )
    {
    }

    /**
     * Gets the plugin instance
     *
     * @return the plugin
     */
    private static Plugin getPlugin( )
    {
        if ( _plugin == null )
        {
            _plugin = PluginService.getPlugin( WikiAIPlugin.PLUGIN_NAME );
        }
        return _plugin;
    }

    /**
     * Creates a new AI feature
     *
     * @param aiFeature
     *            the AI feature to create
     * @return the created AI feature
     */
    public static AiFeature create( AiFeature aiFeature )
    {
        _dao.shiftOrdersUp( aiFeature.getOrder( ), -1, getPlugin( ) );
        _dao.insert( aiFeature, getPlugin( ) );
        return aiFeature;
    }

    /**
     * Finds an AI feature by primary key
     *
     * @param nId
     *            the feature identifier
     * @return an Optional containing the AI feature if found, empty otherwise
     */
    public static Optional<AiFeature> findByPrimaryKey( int nId )
    {
        return _dao.load( nId, getPlugin( ) );
    }

    /**
     * Updates an existing AI feature
     *
     * @param aiFeature
     *            the AI feature to update
     */
    public static void update( AiFeature aiFeature )
    {
        Optional<AiFeature> optExisting = _dao.load( aiFeature.getId( ), getPlugin( ) );
        if ( optExisting.isPresent( ) )
        {
            int nOldOrder = optExisting.get( ).getOrder( );
            if ( nOldOrder != aiFeature.getOrder( ) )
            {
                _dao.shiftOrdersDown( nOldOrder, aiFeature.getId( ), getPlugin( ) );
                _dao.shiftOrdersUp( aiFeature.getOrder( ), aiFeature.getId( ), getPlugin( ) );
            }
        }
        _dao.store( aiFeature, getPlugin( ) );
    }

    /**
     * Removes an AI feature
     *
     * @param nId
     *            the feature identifier
     */
    public static void remove( int nId )
    {
        _dao.delete( nId, getPlugin( ) );
    }

    /**
     * Retrieves all AI features
     *
     * @return a list of all AI features
     */
    public static List<AiFeature> getAiFeaturesList( )
    {
        return _dao.selectAll( getPlugin( ) );
    }

    /**
     * Retrieves all active AI features
     *
     * @return a list of active AI features
     */
    public static List<AiFeature> getActiveFeatures( )
    {
        return _dao.selectActiveFeatures( getPlugin( ) );
    }

    /**
     * Retrieves active AI features by type
     *
     * @param strType
     *            the feature type
     * @return a list of active AI features matching the specified type
     */
    public static List<AiFeature> getFeaturesByType( String strType )
    {
        return _dao.selectFeaturesByType( strType, getPlugin( ) );
    }
}
