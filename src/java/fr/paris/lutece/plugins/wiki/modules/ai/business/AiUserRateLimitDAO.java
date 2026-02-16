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

import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.util.sql.DAOUtil;
import jakarta.enterprise.context.ApplicationScoped;

import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Optional;

/**
 * This class provides Data Access methods for AiUserRateLimit objects
 */
@ApplicationScoped
public class AiUserRateLimitDAO implements IAiUserRateLimitDAO
{
    private static final String SQL_QUERY_INSERT = "INSERT INTO wiki_ai_user_rate_limit (user_id, message_count, date_first_message) VALUES (?, ?, ?)";
    private static final String SQL_QUERY_UPDATE = "UPDATE wiki_ai_user_rate_limit SET user_id = ?, message_count = ?, date_first_message = ? WHERE id = ?";
    private static final String SQL_QUERY_DELETE = "DELETE FROM wiki_ai_user_rate_limit WHERE id = ?";
    private static final String SQL_QUERY_SELECT = "SELECT id, user_id, message_count, date_first_message FROM wiki_ai_user_rate_limit WHERE id = ?";
    private static final String SQL_QUERY_SELECT_BY_USER_ID = "SELECT id, user_id, message_count, date_first_message FROM wiki_ai_user_rate_limit WHERE user_id = ?";
    private static final String SQL_QUERY_REMOVE_EXPIRED = "DELETE FROM wiki_ai_user_rate_limit WHERE date_first_message < ?";

    /**
     * {@inheritDoc }
     */
    @Override
    public void insert( AiUserRateLimit aiUserRateLimit, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_INSERT, Statement.RETURN_GENERATED_KEYS, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setString( nIndex++, aiUserRateLimit.getUserId( ) );
            daoUtil.setInt( nIndex++, aiUserRateLimit.getMessageCount( ) );
            daoUtil.setTimestamp( nIndex++, aiUserRateLimit.getDateFirstMessage( ) );

            daoUtil.executeUpdate( );

            if ( daoUtil.nextGeneratedKey( ) )
            {
                aiUserRateLimit.setId( daoUtil.getGeneratedKeyInt( 1 ) );
            }
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void store( AiUserRateLimit aiUserRateLimit, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_UPDATE, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setString( nIndex++, aiUserRateLimit.getUserId( ) );
            daoUtil.setInt( nIndex++, aiUserRateLimit.getMessageCount( ) );
            daoUtil.setTimestamp( nIndex++, aiUserRateLimit.getDateFirstMessage( ) );
            daoUtil.setInt( nIndex++, aiUserRateLimit.getId( ) );

            daoUtil.executeUpdate( );
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void delete( int nKey, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_DELETE, plugin ) )
        {
            daoUtil.setInt( 1, nKey );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Optional<AiUserRateLimit> load( int nKey, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT, plugin ) )
        {
            daoUtil.setInt( 1, nKey );
            daoUtil.executeQuery( );

            if ( daoUtil.next( ) )
            {
                return Optional.of( dataToObject( daoUtil ) );
            }
        }
        return Optional.empty( );
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Optional<AiUserRateLimit> findByUserId( String strUserId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_USER_ID, plugin ) )
        {
            daoUtil.setString( 1, strUserId );
            daoUtil.executeQuery( );

            if ( daoUtil.next( ) )
            {
                return Optional.of( dataToObject( daoUtil ) );
            }
        }
        return Optional.empty( );
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void removeExpiredEntries( Timestamp cutoffTimestamp, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_REMOVE_EXPIRED, plugin ) )
        {
            daoUtil.setTimestamp( 1, cutoffTimestamp );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * Converts database row to object
     *
     * @param daoUtil
     *            The DAOUtil
     * @return The AiUserRateLimit object
     */
    private AiUserRateLimit dataToObject( DAOUtil daoUtil )
    {
        AiUserRateLimit aiUserRateLimit = new AiUserRateLimit( );
        int nIndex = 1;

        aiUserRateLimit.setId( daoUtil.getInt( nIndex++ ) );
        aiUserRateLimit.setUserId( daoUtil.getString( nIndex++ ) );
        aiUserRateLimit.setMessageCount( daoUtil.getInt( nIndex++ ) );
        aiUserRateLimit.setDateFirstMessage( daoUtil.getTimestamp( nIndex++ ) );

        return aiUserRateLimit;
    }
}
