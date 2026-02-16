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
package fr.paris.lutece.plugins.wiki.modules.ai.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.paris.lutece.plugins.wiki.business.item.AbstractWikiItem;
import fr.paris.lutece.plugins.wiki.business.item.WikiItemHome;
import fr.paris.lutece.plugins.wiki.business.item.impl.Book;
import fr.paris.lutece.plugins.wiki.business.item.impl.Page;
import fr.paris.lutece.plugins.wiki.modules.quiz.business.Quiz;
import fr.paris.lutece.plugins.wiki.modules.ai.business.QuizGeneratedAnswerHome;
import fr.paris.lutece.plugins.wiki.modules.ai.business.QuizGeneratedQuestion;
import fr.paris.lutece.plugins.wiki.modules.ai.business.QuizGeneratedQuestionHome;
import fr.paris.lutece.plugins.wiki.modules.ai.business.QuizGenerationWorkflow;
import fr.paris.lutece.plugins.wiki.modules.ai.business.QuizGenerationWorkflowHome;
import fr.paris.lutece.plugins.wiki.modules.ai.service.quiz.QuizGenerationService;
import fr.paris.lutece.plugins.wiki.modules.quiz.service.QuizService;
import fr.paris.lutece.plugins.wiki.service.security.WikiAccessControlService;
import fr.paris.lutece.plugins.wiki.web.AbstractWikiXPage;
import fr.paris.lutece.portal.service.security.ISecurityTokenService;
import fr.paris.lutece.portal.service.security.LuteceUser;
import fr.paris.lutece.portal.service.security.SecurityService;
import fr.paris.lutece.portal.service.security.SecurityTokenService;
import fr.paris.lutece.portal.service.security.UserNotSignedException;
import fr.paris.lutece.portal.util.mvc.commons.annotations.Action;
import fr.paris.lutece.portal.util.mvc.commons.annotations.View;
import fr.paris.lutece.portal.util.mvc.xpage.annotations.Controller;
import fr.paris.lutece.portal.web.cdi.mvc.Models;
import fr.paris.lutece.portal.web.xpages.XPage;

@SessionScoped
@Named( "wiki-ai.xpage.wikiaiQuizGeneration" )
@Controller( xpageName = "wikiaiQuizGeneration", pageTitleI18nKey = "module.wiki.ai.quizGeneration.pageTitle", pagePathI18nKey = "module.wiki.ai.quizGeneration.pageTitle" )
public class QuizGenerationXPage extends AbstractWikiXPage
{
    private static final long serialVersionUID = 1L;
    private static final ObjectMapper _objectMapper = new ObjectMapper( );

    private static final String VIEW_SELECT_PAGES = "selectPages";
    private static final String VIEW_WORKFLOWS = "viewWorkflows";
    private static final String VIEW_REVIEW_QUESTIONS = "reviewQuestions";

    private static final String ACTION_START_GENERATION = "startGeneration";
    private static final String ACTION_IMPORT_QUESTION = "importQuestion";
    private static final String ACTION_IMPORT_ALL = "importAll";
    private static final String ACTION_DISCARD_QUESTION = "discardQuestion";
    private static final String ACTION_DELETE_WORKFLOW = "deleteWorkflow";

    private static final String TEMPLATE_SELECT_PAGES = "skin/plugins/wiki/modules/ai/select_pages.html";
    private static final String TEMPLATE_WORKFLOWS = "skin/plugins/wiki/modules/ai/view_workflows.html";
    private static final String TEMPLATE_REVIEW = "skin/plugins/wiki/modules/ai/review_questions.html";

    private static final String PARAMETER_QUIZ_ID = "quiz_id";
    private static final String PARAMETER_WORKFLOW_ID = "workflow_id";
    private static final String PARAMETER_QUESTION_ID = "question_id";
    private static final String PARAMETER_PAGE_IDS = "page_ids";

    private static final String MARK_QUIZ = "quiz";
    private static final String MARK_WORKFLOWS = "workflows";
    private static final String MARK_WORKFLOW = "workflow";
    private static final String MARK_QUESTIONS = "questions";

    @Inject
    private Models _models;
    @Inject
    private ISecurityTokenService _securityTokenService;
    @Inject
    private QuizGenerationService _quizGenerationService;

    @View( value = VIEW_SELECT_PAGES, defaultView = true )
    public XPage viewSelectPages( HttpServletRequest request ) throws UserNotSignedException
    {
        LuteceUser user = checkAuthentication( request );

        String strQuizId = request.getParameter( PARAMETER_QUIZ_ID );
        if ( strQuizId == null || strQuizId.isEmpty( ) )
        {
            addError( "module.wiki.ai.quizGeneration.error.quizRequired", getLocale( request ) );
            return getXPage( TEMPLATE_SELECT_PAGES, getLocale( request ) );
        }

        int nQuizId = Integer.parseInt( strQuizId );
        Quiz quiz = QuizService.findById( nQuizId );
        if ( quiz == null )
        {
            addError( "module.wiki.ai.quizGeneration.error.quizNotFound", getLocale( request ) );
            return getXPage( TEMPLATE_SELECT_PAGES, getLocale( request ) );
        }

        Optional<AbstractWikiItem> optBook = WikiItemHome.findByPrimaryKey( quiz.getIdBook( ) );
        if ( optBook.isEmpty( ) || !WikiAccessControlService.canEdit( user, optBook.get( ) ) )
        {
            addError( "module.wiki.ai.quizGeneration.error.accessDenied", getLocale( request ) );
            return getXPage( TEMPLATE_SELECT_PAGES, getLocale( request ) );
        }

        _models.put( MARK_QUIZ, quiz );
        _models.put( SecurityTokenService.MARK_TOKEN, _securityTokenService.getToken( request, ACTION_START_GENERATION ) );
        populateBookSidebarModel( _models, user, (Book) optBook.get( ) );

        return getXPage( TEMPLATE_SELECT_PAGES, getLocale( request ) );
    }

    @Action( ACTION_START_GENERATION )
    public XPage doStartGeneration( HttpServletRequest request ) throws UserNotSignedException
    {
        LuteceUser user = checkAuthentication( request );

        String strQuizId = request.getParameter( PARAMETER_QUIZ_ID );
        String [ ] strPageIds = request.getParameterValues( PARAMETER_PAGE_IDS );

        if ( strQuizId == null || strPageIds == null || strPageIds.length == 0 )
        {
            addError( "module.wiki.ai.quizGeneration.error.selectPages", getLocale( request ) );
            Map<String, String> params = new HashMap<>( );
            params.put( PARAMETER_QUIZ_ID, strQuizId );
            return redirect( request, VIEW_SELECT_PAGES, params );
        }

        int nQuizId = Integer.parseInt( strQuizId );
        Quiz quiz = QuizService.findById( nQuizId );
        if ( quiz == null )
        {
            return redirectView( request, VIEW_SELECT_PAGES );
        }

        Optional<AbstractWikiItem> optBook = WikiItemHome.findByPrimaryKey( quiz.getIdBook( ) );
        if ( optBook.isEmpty( ) || !WikiAccessControlService.canEdit( user, optBook.get( ) ) )
        {
            return redirectView( request, VIEW_SELECT_PAGES );
        }

        List<Integer> pageIds = new ArrayList<>( );
        for ( String strPageId : strPageIds )
        {
            pageIds.add( Integer.parseInt( strPageId ) );
        }

        QuizGenerationWorkflow workflow = new QuizGenerationWorkflow( );
        workflow.setIdQuiz( nQuizId );
        workflow.setIdUser( user.getName( ) );
        workflow.setLocale( getLocale( request ).getLanguage( ) );
        try
        {
            workflow.setSelectedPageIds( _objectMapper.writeValueAsString( pageIds ) );
        }
        catch( JsonProcessingException e )
        {
            workflow.setSelectedPageIds( "[]" );
        }

        QuizGenerationWorkflowHome.create( workflow );
        addInfo( "module.wiki.ai.quizGeneration.message.workflowCreated", getLocale( request ) );

        Map<String, String> params = new HashMap<>( );
        params.put( PARAMETER_QUIZ_ID, strQuizId );
        return redirect( request, VIEW_WORKFLOWS, params );
    }

    @View( VIEW_WORKFLOWS )
    public XPage viewWorkflows( HttpServletRequest request ) throws UserNotSignedException
    {
        LuteceUser user = checkAuthentication( request );

        String strQuizId = request.getParameter( PARAMETER_QUIZ_ID );
        if ( strQuizId == null || strQuizId.isEmpty( ) )
        {
            addError( "module.wiki.ai.quizGeneration.error.quizRequired", getLocale( request ) );
            return getXPage( TEMPLATE_WORKFLOWS, getLocale( request ) );
        }

        int nQuizId = Integer.parseInt( strQuizId );
        Quiz quiz = QuizService.findById( nQuizId );
        if ( quiz == null )
        {
            return redirectView( request, VIEW_SELECT_PAGES );
        }

        Optional<AbstractWikiItem> optBook = WikiItemHome.findByPrimaryKey( quiz.getIdBook( ) );
        if ( optBook.isEmpty( ) || !WikiAccessControlService.canEdit( user, optBook.get( ) ) )
        {
            return redirectView( request, VIEW_SELECT_PAGES );
        }

        List<QuizGenerationWorkflow> workflows = QuizGenerationWorkflowHome.getWorkflowsByQuiz( nQuizId );

        _models.put( MARK_QUIZ, quiz );
        _models.put( MARK_WORKFLOWS, workflows );
        _models.put( SecurityTokenService.MARK_TOKEN, _securityTokenService.getToken( request, ACTION_DELETE_WORKFLOW ) );
        populateBookSidebarModel( _models, user, (Book) optBook.get( ) );

        return getXPage( TEMPLATE_WORKFLOWS, getLocale( request ) );
    }

    @View( VIEW_REVIEW_QUESTIONS )
    public XPage viewReviewQuestions( HttpServletRequest request ) throws UserNotSignedException
    {
        LuteceUser user = checkAuthentication( request );

        String strWorkflowId = request.getParameter( PARAMETER_WORKFLOW_ID );
        if ( strWorkflowId == null || strWorkflowId.isEmpty( ) )
        {
            return redirectView( request, VIEW_WORKFLOWS );
        }

        int nWorkflowId = Integer.parseInt( strWorkflowId );
        Optional<QuizGenerationWorkflow> optWorkflow = QuizGenerationWorkflowHome.findByPrimaryKey( nWorkflowId );
        if ( optWorkflow.isEmpty( ) )
        {
            return redirectView( request, VIEW_WORKFLOWS );
        }

        QuizGenerationWorkflow workflow = optWorkflow.get( );
        Quiz quiz = QuizService.findById( workflow.getIdQuiz( ) );
        if ( quiz == null )
        {
            return redirectView( request, VIEW_WORKFLOWS );
        }

        Optional<AbstractWikiItem> optBook = WikiItemHome.findByPrimaryKey( quiz.getIdBook( ) );
        if ( optBook.isEmpty( ) || !WikiAccessControlService.canEdit( user, optBook.get( ) ) )
        {
            return redirectView( request, VIEW_WORKFLOWS );
        }

        List<QuizGeneratedQuestion> questions = QuizGeneratedQuestionHome.getPendingQuestionsByWorkflow( nWorkflowId );
        Map<Integer, Page> sourcePagesMap = new HashMap<>( );
        Map<Integer, Integer> sourceCountMap = new HashMap<>( );

        for ( QuizGeneratedQuestion q : questions )
        {
            q.setAnswers( QuizGeneratedAnswerHome.getAnswersByQuestion( q.getId( ) ) );

            int pageId = q.getIdSourcePage( );
            if ( !sourcePagesMap.containsKey( pageId ) )
            {
                WikiItemHome.findByPrimaryKey( pageId ).filter( item -> item instanceof Page ).map( item -> (Page) item )
                        .ifPresent( page -> sourcePagesMap.put( pageId, page ) );
            }
            if ( sourcePagesMap.containsKey( pageId ) )
            {
                q.getSourcePages( ).add( sourcePagesMap.get( pageId ) );
                sourceCountMap.merge( pageId, 1, Integer::sum );
            }
        }

        List<Map<String, Object>> sourceFilters = new ArrayList<>( );
        for ( Map.Entry<Integer, Page> entry : sourcePagesMap.entrySet( ) )
        {
            Map<String, Object> filter = new HashMap<>( );
            filter.put( "id", entry.getKey( ) );
            filter.put( "page", entry.getValue( ) );
            filter.put( "count", sourceCountMap.getOrDefault( entry.getKey( ), 0 ) );
            sourceFilters.add( filter );
        }

        _models.put( MARK_QUIZ, quiz );
        _models.put( MARK_WORKFLOW, workflow );
        _models.put( MARK_QUESTIONS, questions );
        _models.put( "source_filters", sourceFilters );
        _models.put( SecurityTokenService.MARK_TOKEN, _securityTokenService.getToken( request, ACTION_IMPORT_QUESTION ) );
        populateBookSidebarModel( _models, user, (Book) optBook.get( ) );

        return getXPage( TEMPLATE_REVIEW, getLocale( request ) );
    }

    @Action( ACTION_IMPORT_QUESTION )
    public XPage doImportQuestion( HttpServletRequest request ) throws UserNotSignedException
    {
        checkAuthentication( request );

        String strQuestionId = request.getParameter( PARAMETER_QUESTION_ID );
        String strWorkflowId = request.getParameter( PARAMETER_WORKFLOW_ID );

        if ( strQuestionId == null || strWorkflowId == null )
        {
            return redirectView( request, VIEW_WORKFLOWS );
        }

        int nQuestionId = Integer.parseInt( strQuestionId );
        int nWorkflowId = Integer.parseInt( strWorkflowId );

        Optional<QuizGenerationWorkflow> optWorkflow = QuizGenerationWorkflowHome.findByPrimaryKey( nWorkflowId );
        if ( optWorkflow.isEmpty( ) )
        {
            return redirectView( request, VIEW_WORKFLOWS );
        }

        _quizGenerationService.importQuestion( nQuestionId, optWorkflow.get( ).getIdQuiz( ) );
        addInfo( "module.wiki.ai.quizGeneration.message.questionImported", getLocale( request ) );

        Map<String, String> params = new HashMap<>( );
        params.put( PARAMETER_WORKFLOW_ID, strWorkflowId );
        return redirect( request, VIEW_REVIEW_QUESTIONS, params );
    }

    @Action( ACTION_IMPORT_ALL )
    public XPage doImportAll( HttpServletRequest request ) throws UserNotSignedException
    {
        checkAuthentication( request );

        String strWorkflowId = request.getParameter( PARAMETER_WORKFLOW_ID );
        if ( strWorkflowId == null )
        {
            return redirectView( request, VIEW_WORKFLOWS );
        }

        int nWorkflowId = Integer.parseInt( strWorkflowId );
        Optional<QuizGenerationWorkflow> optWorkflow = QuizGenerationWorkflowHome.findByPrimaryKey( nWorkflowId );
        if ( optWorkflow.isEmpty( ) )
        {
            return redirectView( request, VIEW_WORKFLOWS );
        }

        List<QuizGeneratedQuestion> questions = QuizGeneratedQuestionHome.getPendingQuestionsByWorkflow( nWorkflowId );
        for ( QuizGeneratedQuestion question : questions )
        {
            _quizGenerationService.importQuestion( question.getId( ), optWorkflow.get( ).getIdQuiz( ) );
        }

        addInfo( "module.wiki.ai.quizGeneration.message.allImported", getLocale( request ) );

        Map<String, String> params = new HashMap<>( );
        params.put( PARAMETER_QUIZ_ID, String.valueOf( optWorkflow.get( ).getIdQuiz( ) ) );
        return redirect( request, VIEW_WORKFLOWS, params );
    }

    @Action( ACTION_DISCARD_QUESTION )
    public XPage doDiscardQuestion( HttpServletRequest request ) throws UserNotSignedException
    {
        checkAuthentication( request );

        String strQuestionId = request.getParameter( PARAMETER_QUESTION_ID );
        String strWorkflowId = request.getParameter( PARAMETER_WORKFLOW_ID );

        if ( strQuestionId != null )
        {
            QuizGeneratedQuestionHome.markAsDiscarded( Integer.parseInt( strQuestionId ) );
        }

        Map<String, String> params = new HashMap<>( );
        params.put( PARAMETER_WORKFLOW_ID, strWorkflowId );
        return redirect( request, VIEW_REVIEW_QUESTIONS, params );
    }

    @Action( ACTION_DELETE_WORKFLOW )
    public XPage doDeleteWorkflow( HttpServletRequest request ) throws UserNotSignedException
    {
        checkAuthentication( request );

        String strWorkflowId = request.getParameter( PARAMETER_WORKFLOW_ID );
        String strQuizId = request.getParameter( PARAMETER_QUIZ_ID );

        if ( strWorkflowId != null )
        {
            QuizGenerationWorkflowHome.remove( Integer.parseInt( strWorkflowId ) );
            addInfo( "module.wiki.ai.quizGeneration.message.workflowDeleted", getLocale( request ) );
        }

        Map<String, String> params = new HashMap<>( );
        params.put( PARAMETER_QUIZ_ID, strQuizId );
        return redirect( request, VIEW_WORKFLOWS, params );
    }

    private LuteceUser checkAuthentication( HttpServletRequest request ) throws UserNotSignedException
    {
        LuteceUser user = SecurityService.getInstance( ).getRegisteredUser( request );
        if ( user == null )
        {
            throw new UserNotSignedException( );
        }
        return user;
    }
}
