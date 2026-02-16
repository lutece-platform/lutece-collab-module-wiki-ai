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
package fr.paris.lutece.plugins.wiki.modules.ai.rs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.RequestScoped;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import fr.paris.lutece.plugins.wiki.business.item.AbstractWikiItem;
import fr.paris.lutece.plugins.wiki.business.item.WikiItemHome;
import fr.paris.lutece.plugins.wiki.modules.quiz.business.Quiz;
import fr.paris.lutece.plugins.wiki.modules.ai.business.QuizGeneratedQuestionHome;
import fr.paris.lutece.plugins.wiki.modules.ai.business.QuizGenerationWorkflow;
import fr.paris.lutece.plugins.wiki.modules.ai.business.QuizGenerationWorkflowHome;
import fr.paris.lutece.plugins.wiki.modules.quiz.service.QuizService;
import fr.paris.lutece.plugins.wiki.service.security.WikiAccessControlService;
import fr.paris.lutece.portal.service.security.LuteceUser;
import fr.paris.lutece.portal.service.security.SecurityService;

/**
 * REST endpoint for quiz generation workflow operations.
 */
@RequestScoped
@Path( "wiki/ai/quiz-generation" )
public class QuizGenerationRest extends AbstractRestEndpoint
{
    private static final String ERROR_NOT_AUTHENTICATED = "Not authenticated";
    private static final String ERROR_WORKFLOW_NOT_FOUND = "Workflow not found";
    private static final String ERROR_ACCESS_DENIED = "Access denied";

    private static final String KEY_ID = "id";
    private static final String KEY_STATUS = "status";
    private static final String KEY_QUESTIONS_GENERATED = "questionsGenerated";
    private static final String KEY_QUESTIONS_PENDING = "questionsPending";
    private static final String KEY_TOTAL_PAGES = "totalPages";
    private static final String KEY_PROCESSED_PAGES = "processedPages";
    private static final String KEY_ERROR_MESSAGE = "errorMessage";
    private static final String KEY_DATE_CREATION = "dateCreation";
    private static final String KEY_DATE_COMPLETION = "dateCompletion";

    @Context
    private HttpServletRequest _request;

    /**
     * Retrieves the status of a quiz generation workflow.
     *
     * @param workflowId
     *            the identifier of the workflow to retrieve
     * @return a {@link Response} containing the workflow status details or an error response
     */
    @GET
    @Path( "workflow/{workflowId}" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getWorkflowStatus( @PathParam( "workflowId" ) int workflowId )
    {
        LuteceUser user = SecurityService.getInstance( ).getRegisteredUser( _request );

        if ( user == null )
        {
            return createErrorResponse( Response.Status.UNAUTHORIZED, ERROR_NOT_AUTHENTICATED );
        }

        Optional<QuizGenerationWorkflow> optWorkflow = QuizGenerationWorkflowHome.findByPrimaryKey( workflowId );

        if ( optWorkflow.isEmpty( ) )
        {
            return createErrorResponse( Response.Status.NOT_FOUND, ERROR_WORKFLOW_NOT_FOUND );
        }

        QuizGenerationWorkflow workflow = optWorkflow.get( );

        if ( !canEditQuiz( user, workflow.getIdQuiz( ) ) )
        {
            return createErrorResponse( Response.Status.FORBIDDEN, ERROR_ACCESS_DENIED );
        }

        int pendingCount = QuizGeneratedQuestionHome.getPendingQuestionsByWorkflow( workflowId ).size( );
        int totalCount = QuizGeneratedQuestionHome.getQuestionsByWorkflow( workflowId ).size( );

        Map<String, Object> result = new HashMap<>( );
        result.put( KEY_ID, workflow.getId( ) );
        result.put( KEY_STATUS, workflow.getStatus( ).getCode( ) );
        result.put( KEY_QUESTIONS_GENERATED, totalCount );
        result.put( KEY_QUESTIONS_PENDING, pendingCount );
        result.put( KEY_TOTAL_PAGES, workflow.getTotalPages( ) );
        result.put( KEY_PROCESSED_PAGES, workflow.getProcessedPages( ) );
        result.put( KEY_ERROR_MESSAGE, workflow.getErrorMessage( ) );

        return Response.ok( result ).build( );
    }

    /**
     * Retrieves all workflows associated with a specific quiz.
     *
     * @param quizId
     *            the identifier of the quiz
     * @return a {@link Response} containing a list of workflow details or an error response
     */
    @GET
    @Path( "quiz/{quizId}/workflows" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getWorkflowsByQuiz( @PathParam( "quizId" ) int quizId )
    {
        LuteceUser user = SecurityService.getInstance( ).getRegisteredUser( _request );

        if ( user == null )
        {
            return createErrorResponse( Response.Status.UNAUTHORIZED, ERROR_NOT_AUTHENTICATED );
        }

        if ( !canEditQuiz( user, quizId ) )
        {
            return createErrorResponse( Response.Status.FORBIDDEN, ERROR_ACCESS_DENIED );
        }

        List<QuizGenerationWorkflow> workflows = QuizGenerationWorkflowHome.getWorkflowsByQuiz( quizId );

        List<Map<String, Object>> result = workflows.stream( ).map( this::buildWorkflowMap ).collect( Collectors.toList( ) );

        return Response.ok( result ).build( );
    }

    /**
     * Builds a map representation of a workflow for JSON serialization.
     *
     * @param workflow
     *            the workflow to convert
     * @return a {@link Map} containing the workflow properties
     */
    private Map<String, Object> buildWorkflowMap( QuizGenerationWorkflow workflow )
    {
        Map<String, Object> map = new HashMap<>( );
        map.put( KEY_ID, workflow.getId( ) );
        map.put( KEY_STATUS, workflow.getStatus( ).getCode( ) );
        map.put( KEY_DATE_CREATION, workflow.getDateCreation( ) );
        map.put( KEY_DATE_COMPLETION, workflow.getDateCompletion( ) );
        map.put( KEY_TOTAL_PAGES, workflow.getTotalPages( ) );
        map.put( KEY_PROCESSED_PAGES, workflow.getProcessedPages( ) );
        map.put( KEY_QUESTIONS_GENERATED, QuizGeneratedQuestionHome.getQuestionsByWorkflow( workflow.getId( ) ).size( ) );
        map.put( KEY_ERROR_MESSAGE, workflow.getErrorMessage( ) );
        return map;
    }

    /**
     * Checks if the user has permission to edit the specified quiz.
     *
     * @param user
     *            the user to check permissions for
     * @param nQuizId
     *            the identifier of the quiz
     * @return {@code true} if the user can edit the quiz, {@code false} otherwise
     */
    private boolean canEditQuiz( LuteceUser user, int nQuizId )
    {
        Quiz quiz = QuizService.findById( nQuizId );

        if ( quiz == null )
        {
            return false;
        }

        Optional<AbstractWikiItem> optBook = WikiItemHome.findByPrimaryKey( quiz.getIdBook( ) );

        if ( optBook.isEmpty( ) )
        {
            return false;
        }

        return WikiAccessControlService.canEdit( user, optBook.get( ) );
    }
}
