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

import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.util.sql.DAOUtil;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * AiFeatureDAO implementation for database operations
 */
@ApplicationScoped
public class AiFeatureDAO implements IAiFeatureDAO
{
    private static final String SQL_QUERY_INSERT = "INSERT INTO wiki_ai_feature (name, type, prompt_template, is_active, order_num, display_mode) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String SQL_QUERY_SELECT = "SELECT id, name, type, prompt_template, is_active, order_num, display_mode FROM wiki_ai_feature WHERE id = ?";
    private static final String SQL_QUERY_UPDATE = "UPDATE wiki_ai_feature SET name = ?, type = ?, prompt_template = ?, is_active = ?, order_num = ?, display_mode = ? WHERE id = ?";
    private static final String SQL_QUERY_DELETE = "DELETE FROM wiki_ai_feature WHERE id = ?";
    private static final String SQL_QUERY_SELECT_ALL = "SELECT id, name, type, prompt_template, is_active, order_num, display_mode FROM wiki_ai_feature ORDER BY order_num, id";
    private static final String SQL_QUERY_SELECT_ACTIVE = "SELECT id, name, type, prompt_template, is_active, order_num, display_mode FROM wiki_ai_feature WHERE is_active = 1 ORDER BY order_num, id";
    private static final String SQL_QUERY_SELECT_BY_TYPE = "SELECT id, name, type, prompt_template, is_active, order_num, display_mode FROM wiki_ai_feature WHERE type = ? AND is_active = 1 ORDER BY order_num, id";
    private static final String SQL_QUERY_SHIFT_UP = "UPDATE wiki_ai_feature SET order_num = order_num + 1 WHERE order_num >= ? AND id != ?";
    private static final String SQL_QUERY_SHIFT_DOWN = "UPDATE wiki_ai_feature SET order_num = order_num - 1 WHERE order_num > ? AND id != ?";

    /**
     * Inserts a new AI feature into the database
     *
     * @param aiFeature
     *            the AI feature to insert
     * @param plugin
     *            the plugin
     */
    @Override
    public void insert( AiFeature aiFeature, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_INSERT, Statement.RETURN_GENERATED_KEYS, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setString( nIndex++, aiFeature.getName( ) );
            daoUtil.setString( nIndex++, aiFeature.getType( ) );
            daoUtil.setString( nIndex++, aiFeature.getPromptTemplate( ) );
            daoUtil.setBoolean( nIndex++, aiFeature.isActive( ) );
            daoUtil.setInt( nIndex++, aiFeature.getOrder( ) );
            daoUtil.setString( nIndex++, aiFeature.getDisplayMode( ) );
            daoUtil.executeUpdate( );
            if ( daoUtil.nextGeneratedKey( ) )
            {
                aiFeature.setId( daoUtil.getGeneratedKeyInt( 1 ) );
            }
        }
    }

    /**
     * Loads an AI feature by identifier
     *
     * @param nId
     *            the feature identifier
     * @param plugin
     *            the plugin
     * @return an Optional containing the AI feature if found, empty otherwise
     */
    @Override
    public Optional<AiFeature> load( int nId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT, plugin ) )
        {
            daoUtil.setInt( 1, nId );
            daoUtil.executeQuery( );
            if ( daoUtil.next( ) )
            {
                return Optional.of( dataToObject( daoUtil ) );
            }
        }
        return Optional.empty( );
    }

    /**
     * Updates an existing AI feature
     *
     * @param aiFeature
     *            the AI feature to update
     * @param plugin
     *            the plugin
     */
    @Override
    public void store( AiFeature aiFeature, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_UPDATE, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setString( nIndex++, aiFeature.getName( ) );
            daoUtil.setString( nIndex++, aiFeature.getType( ) );
            daoUtil.setString( nIndex++, aiFeature.getPromptTemplate( ) );
            daoUtil.setBoolean( nIndex++, aiFeature.isActive( ) );
            daoUtil.setInt( nIndex++, aiFeature.getOrder( ) );
            daoUtil.setString( nIndex++, aiFeature.getDisplayMode( ) );
            daoUtil.setInt( nIndex++, aiFeature.getId( ) );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * Deletes an AI feature by identifier
     *
     * @param nId
     *            the feature identifier
     * @param plugin
     *            the plugin
     */
    @Override
    public void delete( int nId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_DELETE, plugin ) )
        {
            daoUtil.setInt( 1, nId );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * Retrieves all AI features
     *
     * @param plugin
     *            the plugin
     * @return a list of all AI features
     */
    @Override
    public List<AiFeature> selectAll( Plugin plugin )
    {
        List<AiFeature> listFeatures = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_ALL, plugin ) )
        {
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                listFeatures.add( dataToObject( daoUtil ) );
            }
        }
        return listFeatures;
    }

    /**
     * Retrieves all active AI features
     *
     * @param plugin
     *            the plugin
     * @return a list of active AI features
     */
    @Override
    public List<AiFeature> selectActiveFeatures( Plugin plugin )
    {
        List<AiFeature> listFeatures = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_ACTIVE, plugin ) )
        {
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                listFeatures.add( dataToObject( daoUtil ) );
            }
        }
        return listFeatures;
    }

    /**
     * Retrieves active AI features by type
     *
     * @param strType
     *            the feature type
     * @param plugin
     *            the plugin
     * @return a list of active AI features matching the specified type
     */
    @Override
    public List<AiFeature> selectFeaturesByType( String strType, Plugin plugin )
    {
        List<AiFeature> listFeatures = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_TYPE, plugin ) )
        {
            daoUtil.setString( 1, strType );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                listFeatures.add( dataToObject( daoUtil ) );
            }
        }
        return listFeatures;
    }

    /**
     * Converts database row data to AiFeature object
     *
     * @param daoUtil
     *            the DAO utility containing the result set
     * @return the AiFeature object
     */
    private AiFeature dataToObject( DAOUtil daoUtil )
    {
        AiFeature aiFeature = new AiFeature( );
        int nIndex = 1;
        aiFeature.setId( daoUtil.getInt( nIndex++ ) );
        aiFeature.setName( daoUtil.getString( nIndex++ ) );
        aiFeature.setType( daoUtil.getString( nIndex++ ) );
        aiFeature.setPromptTemplate( daoUtil.getString( nIndex++ ) );
        aiFeature.setActive( daoUtil.getBoolean( nIndex++ ) );
        aiFeature.setOrder( daoUtil.getInt( nIndex++ ) );
        aiFeature.setDisplayMode( daoUtil.getString( nIndex++ ) );
        return aiFeature;
    }

    @Override
    public void shiftOrdersUp( int nOrder, int nExcludeId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SHIFT_UP, plugin ) )
        {
            daoUtil.setInt( 1, nOrder );
            daoUtil.setInt( 2, nExcludeId );
            daoUtil.executeUpdate( );
        }
    }

    @Override
    public void shiftOrdersDown( int nOrder, int nExcludeId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SHIFT_DOWN, plugin ) )
        {
            daoUtil.setInt( 1, nOrder );
            daoUtil.setInt( 2, nExcludeId );
            daoUtil.executeUpdate( );
        }
    }
}
