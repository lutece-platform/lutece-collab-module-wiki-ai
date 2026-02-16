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

@ApplicationScoped
public class QuizGeneratedQuestionDAO implements IQuizGeneratedQuestionDAO
{
    private static final String SQL_QUERY_INSERT = "INSERT INTO wiki_ai_quiz_generated_question (id_workflow, id_source_page, question_text, question_type, explanation) VALUES (?, ?, ?, ?, ?)";
    private static final String SQL_QUERY_SELECT = "SELECT id_question, id_workflow, id_source_page, question_text, question_type, explanation, is_imported, is_discarded FROM wiki_ai_quiz_generated_question WHERE id_question = ?";
    private static final String SQL_QUERY_UPDATE = "UPDATE wiki_ai_quiz_generated_question SET id_workflow = ?, id_source_page = ?, question_text = ?, question_type = ?, explanation = ?, is_imported = ?, is_discarded = ? WHERE id_question = ?";
    private static final String SQL_QUERY_DELETE = "DELETE FROM wiki_ai_quiz_generated_question WHERE id_question = ?";
    private static final String SQL_QUERY_SELECT_BY_WORKFLOW = "SELECT id_question, id_workflow, id_source_page, question_text, question_type, explanation, is_imported, is_discarded FROM wiki_ai_quiz_generated_question WHERE id_workflow = ?";
    private static final String SQL_QUERY_SELECT_PENDING = "SELECT id_question, id_workflow, id_source_page, question_text, question_type, explanation, is_imported, is_discarded FROM wiki_ai_quiz_generated_question WHERE id_workflow = ? AND is_imported = 0 AND is_discarded = 0";
    private static final String SQL_QUERY_MARK_IMPORTED = "UPDATE wiki_ai_quiz_generated_question SET is_imported = 1 WHERE id_question = ?";
    private static final String SQL_QUERY_MARK_DISCARDED = "UPDATE wiki_ai_quiz_generated_question SET is_discarded = 1 WHERE id_question = ?";

    @Override
    public void insert( QuizGeneratedQuestion question, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_INSERT, Statement.RETURN_GENERATED_KEYS, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setInt( nIndex++, question.getIdWorkflow( ) );
            daoUtil.setInt( nIndex++, question.getIdSourcePage( ) );
            daoUtil.setString( nIndex++, question.getQuestionText( ) );
            daoUtil.setString( nIndex++, question.getQuestionType( ) );
            daoUtil.setString( nIndex++, question.getExplanation( ) );
            daoUtil.executeUpdate( );
            if ( daoUtil.nextGeneratedKey( ) )
            {
                question.setId( daoUtil.getGeneratedKeyInt( 1 ) );
            }
        }
    }

    @Override
    public Optional<QuizGeneratedQuestion> load( int nId, Plugin plugin )
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
    public void store( QuizGeneratedQuestion question, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_UPDATE, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setInt( nIndex++, question.getIdWorkflow( ) );
            daoUtil.setInt( nIndex++, question.getIdSourcePage( ) );
            daoUtil.setString( nIndex++, question.getQuestionText( ) );
            daoUtil.setString( nIndex++, question.getQuestionType( ) );
            daoUtil.setString( nIndex++, question.getExplanation( ) );
            daoUtil.setBoolean( nIndex++, question.isImported( ) );
            daoUtil.setBoolean( nIndex++, question.isDiscarded( ) );
            daoUtil.setInt( nIndex++, question.getId( ) );
            daoUtil.executeUpdate( );
        }
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
    public List<QuizGeneratedQuestion> selectByWorkflow( int nIdWorkflow, Plugin plugin )
    {
        List<QuizGeneratedQuestion> list = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_WORKFLOW, plugin ) )
        {
            daoUtil.setInt( 1, nIdWorkflow );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                list.add( dataToObject( daoUtil ) );
            }
        }
        return list;
    }

    @Override
    public List<QuizGeneratedQuestion> selectPendingByWorkflow( int nIdWorkflow, Plugin plugin )
    {
        List<QuizGeneratedQuestion> list = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_PENDING, plugin ) )
        {
            daoUtil.setInt( 1, nIdWorkflow );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                list.add( dataToObject( daoUtil ) );
            }
        }
        return list;
    }

    @Override
    public void markAsImported( int nId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_MARK_IMPORTED, plugin ) )
        {
            daoUtil.setInt( 1, nId );
            daoUtil.executeUpdate( );
        }
    }

    @Override
    public void markAsDiscarded( int nId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_MARK_DISCARDED, plugin ) )
        {
            daoUtil.setInt( 1, nId );
            daoUtil.executeUpdate( );
        }
    }

    private QuizGeneratedQuestion dataToObject( DAOUtil daoUtil )
    {
        QuizGeneratedQuestion question = new QuizGeneratedQuestion( );
        int nIndex = 1;
        question.setId( daoUtil.getInt( nIndex++ ) );
        question.setIdWorkflow( daoUtil.getInt( nIndex++ ) );
        question.setIdSourcePage( daoUtil.getInt( nIndex++ ) );
        question.setQuestionText( daoUtil.getString( nIndex++ ) );
        question.setQuestionType( daoUtil.getString( nIndex++ ) );
        question.setExplanation( daoUtil.getString( nIndex++ ) );
        question.setImported( daoUtil.getBoolean( nIndex++ ) );
        question.setDiscarded( daoUtil.getBoolean( nIndex++ ) );
        return question;
    }
}
