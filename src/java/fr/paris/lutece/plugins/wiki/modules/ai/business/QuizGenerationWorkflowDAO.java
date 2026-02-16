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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.util.sql.DAOUtil;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class QuizGenerationWorkflowDAO implements IQuizGenerationWorkflowDAO
{
    private static final String SQL_QUERY_INSERT = "INSERT INTO wiki_ai_quiz_generation_workflow (id_quiz, id_user, status, selected_page_ids, total_pages, processed_pages, date_creation, locale) VALUES (?, ?, ?, ?, ?, 0, ?, ?)";
    private static final String SQL_QUERY_SELECT = "SELECT id_workflow, id_quiz, id_user, status, selected_page_ids, error_message, total_pages, processed_pages, date_creation, date_completion, locale FROM wiki_ai_quiz_generation_workflow WHERE id_workflow = ?";
    private static final String SQL_QUERY_UPDATE = "UPDATE wiki_ai_quiz_generation_workflow SET id_quiz = ?, id_user = ?, status = ?, selected_page_ids = ?, error_message = ?, total_pages = ?, processed_pages = ?, date_completion = ? WHERE id_workflow = ?";
    private static final String SQL_QUERY_DELETE = "DELETE FROM wiki_ai_quiz_generation_workflow WHERE id_workflow = ?";
    private static final String SQL_QUERY_SELECT_BY_QUIZ = "SELECT id_workflow, id_quiz, id_user, status, selected_page_ids, error_message, total_pages, processed_pages, date_creation, date_completion, locale FROM wiki_ai_quiz_generation_workflow WHERE id_quiz = ? ORDER BY date_creation DESC";
    private static final String SQL_QUERY_SELECT_BY_STATUS = "SELECT id_workflow, id_quiz, id_user, status, selected_page_ids, error_message, total_pages, processed_pages, date_creation, date_completion, locale FROM wiki_ai_quiz_generation_workflow WHERE status = ? ORDER BY date_creation ASC";
    private static final String SQL_QUERY_SELECT_FIRST_PENDING = "SELECT id_workflow, id_quiz, id_user, status, selected_page_ids, error_message, total_pages, processed_pages, date_creation, date_completion, locale FROM wiki_ai_quiz_generation_workflow WHERE status = 'PENDING' ORDER BY date_creation ASC LIMIT 1";

    @Override
    public void insert( QuizGenerationWorkflow workflow, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_INSERT, Statement.RETURN_GENERATED_KEYS, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setInt( nIndex++, workflow.getIdQuiz( ) );
            daoUtil.setString( nIndex++, workflow.getIdUser( ) );
            daoUtil.setString( nIndex++, workflow.getStatus( ).getCode( ) );
            daoUtil.setString( nIndex++, workflow.getSelectedPageIds( ) );
            daoUtil.setInt( nIndex++, workflow.getTotalPages( ) );
            daoUtil.setTimestamp( nIndex++, new Timestamp( System.currentTimeMillis( ) ) );
            daoUtil.setString( nIndex++, workflow.getLocale( ) );
            daoUtil.executeUpdate( );
            if ( daoUtil.nextGeneratedKey( ) )
            {
                workflow.setId( daoUtil.getGeneratedKeyInt( 1 ) );
            }
        }
    }

    @Override
    public Optional<QuizGenerationWorkflow> load( int nId, Plugin plugin )
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
    public void store( QuizGenerationWorkflow workflow, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_UPDATE, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setInt( nIndex++, workflow.getIdQuiz( ) );
            daoUtil.setString( nIndex++, workflow.getIdUser( ) );
            daoUtil.setString( nIndex++, workflow.getStatus( ).getCode( ) );
            daoUtil.setString( nIndex++, workflow.getSelectedPageIds( ) );
            daoUtil.setString( nIndex++, workflow.getErrorMessage( ) );
            daoUtil.setInt( nIndex++, workflow.getTotalPages( ) );
            daoUtil.setInt( nIndex++, workflow.getProcessedPages( ) );
            daoUtil.setTimestamp( nIndex++, workflow.getDateCompletion( ) );
            daoUtil.setInt( nIndex++, workflow.getId( ) );
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
    public List<QuizGenerationWorkflow> selectByQuiz( int nIdQuiz, Plugin plugin )
    {
        List<QuizGenerationWorkflow> list = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_QUIZ, plugin ) )
        {
            daoUtil.setInt( 1, nIdQuiz );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                list.add( dataToObject( daoUtil ) );
            }
        }
        return list;
    }

    @Override
    public List<QuizGenerationWorkflow> selectByStatus( WorkflowStatus status, Plugin plugin )
    {
        List<QuizGenerationWorkflow> list = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_STATUS, plugin ) )
        {
            daoUtil.setString( 1, status.getCode( ) );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                list.add( dataToObject( daoUtil ) );
            }
        }
        return list;
    }

    @Override
    public Optional<QuizGenerationWorkflow> selectFirstPending( Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_FIRST_PENDING, plugin ) )
        {
            daoUtil.executeQuery( );
            if ( daoUtil.next( ) )
            {
                return Optional.of( dataToObject( daoUtil ) );
            }
        }
        return Optional.empty( );
    }

    private QuizGenerationWorkflow dataToObject( DAOUtil daoUtil )
    {
        QuizGenerationWorkflow workflow = new QuizGenerationWorkflow( );
        int nIndex = 1;
        workflow.setId( daoUtil.getInt( nIndex++ ) );
        workflow.setIdQuiz( daoUtil.getInt( nIndex++ ) );
        workflow.setIdUser( daoUtil.getString( nIndex++ ) );
        workflow.setStatusCode( daoUtil.getString( nIndex++ ) );
        workflow.setSelectedPageIds( daoUtil.getString( nIndex++ ) );
        workflow.setErrorMessage( daoUtil.getString( nIndex++ ) );
        workflow.setTotalPages( daoUtil.getInt( nIndex++ ) );
        workflow.setProcessedPages( daoUtil.getInt( nIndex++ ) );
        workflow.setDateCreation( daoUtil.getTimestamp( nIndex++ ) );
        workflow.setDateCompletion( daoUtil.getTimestamp( nIndex++ ) );
        workflow.setLocale( daoUtil.getString( nIndex++ ) );
        return workflow;
    }
}
