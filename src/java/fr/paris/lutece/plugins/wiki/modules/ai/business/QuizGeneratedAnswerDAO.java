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

public class QuizGeneratedAnswerDAO implements IQuizGeneratedAnswerDAO
{
    private static final String SQL_QUERY_INSERT = "INSERT INTO wiki_ai_quiz_generated_answer (id_question, answer_text, is_correct, match_target, correct_order, display_order) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String SQL_QUERY_SELECT = "SELECT id_answer, id_question, answer_text, is_correct, match_target, correct_order, display_order FROM wiki_ai_quiz_generated_answer WHERE id_answer = ?";
    private static final String SQL_QUERY_DELETE = "DELETE FROM wiki_ai_quiz_generated_answer WHERE id_answer = ?";
    private static final String SQL_QUERY_DELETE_BY_QUESTION = "DELETE FROM wiki_ai_quiz_generated_answer WHERE id_question = ?";
    private static final String SQL_QUERY_SELECT_BY_QUESTION = "SELECT id_answer, id_question, answer_text, is_correct, match_target, correct_order, display_order FROM wiki_ai_quiz_generated_answer WHERE id_question = ? ORDER BY display_order";

    @Override
    public void insert( QuizGeneratedAnswer answer, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_INSERT, Statement.RETURN_GENERATED_KEYS, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setInt( nIndex++, answer.getIdQuestion( ) );
            daoUtil.setString( nIndex++, answer.getAnswerText( ) );
            daoUtil.setBoolean( nIndex++, answer.getIsCorrect( ) );
            daoUtil.setString( nIndex++, answer.getMatchTarget( ) );
            if ( answer.getCorrectOrder( ) != null )
            {
                daoUtil.setInt( nIndex++, answer.getCorrectOrder( ) );
            }
            else
            {
                daoUtil.setIntNull( nIndex++ );
            }
            daoUtil.setInt( nIndex++, answer.getDisplayOrder( ) );
            daoUtil.executeUpdate( );
            if ( daoUtil.nextGeneratedKey( ) )
            {
                answer.setId( daoUtil.getGeneratedKeyInt( 1 ) );
            }
        }
    }

    @Override
    public Optional<QuizGeneratedAnswer> load( int nId, Plugin plugin )
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

    @Override
    public void delete( int nId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_DELETE, plugin ) )
        {
            daoUtil.setInt( 1, nId );
            daoUtil.executeUpdate( );
        }
    }

    @Override
    public void deleteByQuestion( int nIdQuestion, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_DELETE_BY_QUESTION, plugin ) )
        {
            daoUtil.setInt( 1, nIdQuestion );
            daoUtil.executeUpdate( );
        }
    }

    @Override
    public List<QuizGeneratedAnswer> selectByQuestion( int nIdQuestion, Plugin plugin )
    {
        List<QuizGeneratedAnswer> list = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_QUESTION, plugin ) )
        {
            daoUtil.setInt( 1, nIdQuestion );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                list.add( dataToObject( daoUtil ) );
            }
        }
        return list;
    }

    private QuizGeneratedAnswer dataToObject( DAOUtil daoUtil )
    {
        QuizGeneratedAnswer answer = new QuizGeneratedAnswer( );
        int nIndex = 1;
        answer.setId( daoUtil.getInt( nIndex++ ) );
        answer.setIdQuestion( daoUtil.getInt( nIndex++ ) );
        answer.setAnswerText( daoUtil.getString( nIndex++ ) );
        answer.setIsCorrect( daoUtil.getBoolean( nIndex++ ) );
        answer.setMatchTarget( daoUtil.getString( nIndex++ ) );
        answer.setCorrectOrder( daoUtil.getObject( nIndex++, Integer.class ) );
        answer.setDisplayOrder( daoUtil.getInt( nIndex++ ) );
        return answer;
    }
}
