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

import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.util.sql.DAOUtil;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * WikiAIIndexerActionDAO provides data access methods for WikiAIIndexerAction objects
 */
@ApplicationScoped
public class WikiAIIndexerActionDAO implements IWikiAIIndexerActionDAO
{
    private static final String SQL_QUERY_INSERT = "INSERT INTO wiki_ai_indexer_action ( id_document, id_task ) VALUES ( ?, ? )";
    private static final String SQL_QUERY_SELECT = "SELECT id_action, id_document, id_task FROM wiki_ai_indexer_action WHERE id_action = ?";
    private static final String SQL_QUERY_DELETE = "DELETE FROM wiki_ai_indexer_action WHERE id_action = ?";
    private static final String SQL_QUERY_DELETE_ALL = "DELETE FROM wiki_ai_indexer_action";
    private static final String SQL_QUERY_SELECT_ALL = "SELECT id_action, id_document, id_task FROM wiki_ai_indexer_action";

    /**
     * {@inheritDoc}
     */
    @Override
    public void insert( WikiAIIndexerAction indexerAction, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_INSERT, Statement.RETURN_GENERATED_KEYS, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setString( nIndex++, indexerAction.getIdDocument( ) );
            daoUtil.setInt( nIndex, indexerAction.getIdTask( ) );
            daoUtil.executeUpdate( );

            if ( daoUtil.nextGeneratedKey( ) )
            {
                indexerAction.setIdAction( daoUtil.getGeneratedKeyInt( 1 ) );
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WikiAIIndexerAction load( int nId, Plugin plugin )
    {
        WikiAIIndexerAction indexerAction = null;

        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT, plugin ) )
        {
            daoUtil.setInt( 1, nId );
            daoUtil.executeQuery( );

            if ( daoUtil.next( ) )
            {
                indexerAction = dataToObject( daoUtil );
            }
        }

        return indexerAction;
    }

    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
     */
    @Override
    public void deleteAll( Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_DELETE_ALL, plugin ) )
        {
            daoUtil.executeUpdate( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<WikiAIIndexerAction> selectList( Plugin plugin )
    {
        List<WikiAIIndexerAction> listActions = new ArrayList<>( );

        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_ALL, plugin ) )
        {
            daoUtil.executeQuery( );

            while ( daoUtil.next( ) )
            {
                listActions.add( dataToObject( daoUtil ) );
            }
        }

        return listActions;
    }

    /**
     * Converts database row to WikiAIIndexerAction object
     *
     * @param daoUtil the DAO utility
     * @return the WikiAIIndexerAction object
     */
    private WikiAIIndexerAction dataToObject( DAOUtil daoUtil )
    {
        WikiAIIndexerAction indexerAction = new WikiAIIndexerAction( );
        indexerAction.setIdAction( daoUtil.getInt( "id_action" ) );
        indexerAction.setIdDocument( daoUtil.getString( "id_document" ) );
        indexerAction.setIdTask( daoUtil.getInt( "id_task" ) );
        return indexerAction;
    }
}
