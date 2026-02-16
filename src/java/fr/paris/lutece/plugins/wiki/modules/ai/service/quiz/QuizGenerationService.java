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
package fr.paris.lutece.plugins.wiki.modules.ai.service.quiz;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import fr.paris.lutece.plugins.wiki.business.item.AbstractWikiItem;
import fr.paris.lutece.plugins.wiki.business.item.WikiItemHome;
import fr.paris.lutece.plugins.wiki.business.item.WikiItemType;
import fr.paris.lutece.plugins.wiki.modules.quiz.business.QuestionType;
import fr.paris.lutece.plugins.wiki.modules.quiz.business.QuizAnswer;
import fr.paris.lutece.plugins.wiki.modules.quiz.business.QuizQuestion;
import fr.paris.lutece.plugins.wiki.business.revision.Revision;
import fr.paris.lutece.plugins.wiki.modules.ai.business.QuizGeneratedAnswer;
import fr.paris.lutece.plugins.wiki.modules.ai.business.QuizGeneratedAnswerHome;
import fr.paris.lutece.plugins.wiki.modules.ai.business.QuizGeneratedQuestion;
import fr.paris.lutece.plugins.wiki.service.RevisionService;
import fr.paris.lutece.plugins.wiki.modules.ai.business.QuizGeneratedQuestionHome;
import fr.paris.lutece.plugins.wiki.modules.ai.business.QuizGenerationWorkflow;
import fr.paris.lutece.plugins.wiki.modules.ai.business.QuizGenerationWorkflowHome;
import fr.paris.lutece.plugins.wiki.modules.ai.business.WorkflowStatus;
import fr.paris.lutece.plugins.wiki.modules.ai.service.tools.CreateQuizQuestionTool;

import fr.paris.lutece.plugins.wiki.modules.quiz.service.QuizService;
import fr.paris.lutece.portal.service.util.AppLogService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@ApplicationScoped
public class QuizGenerationService
{
    interface QuizGenerator
    {
        @UserMessage( "{{pageContent}}" )
        String generate( @V( "pageContent" ) String content );
    }

    private static final String SYSTEM_PROMPT = "You are an expert in creating educational quizzes.\n\n"
            + "Analyze the provided wiki page content and generate relevant quiz questions.\n\n" + "RULES:\n" + "- Generate a MAXIMUM of 5 questions per page\n"
            + "- Only generate questions that have real educational value\n" + "- Do NOT generate questions if the content is too poor or too generic\n"
            + "- Prefer comprehension questions over pure memorization\n" + "- Always provide an explanation for the answer\n"
            + "- Vary question types according to the content\n" + "- NEVER use double brace syntax in your responses\n\n" + "AVAILABLE TYPES:\n\n"
            + "1. MCQ (Multiple Choice) - Multiple answers can be correct\n" + "   - 2 to 6 possible answers\n"
            + "   - At least 1 correct answer, can have multiple\n" + "   - JSON: [{\"text\":\"...\",\"correct\":true/false},...]\n\n"
            + "2. TRUE_FALSE (Single Choice) - Only one correct answer\n" + "   - 2 to 4 possible answers\n" + "   - Exactly 1 correct answer\n"
            + "   - JSON: [{\"text\":\"...\",\"correct\":true/false},...]\n\n" + "3. MATCHING (Association) - Match elements together\n"
            + "   - Pairs of elements to associate\n" + "   - JSON: [{\"text\":\"left element\",\"matchTarget\":\"right element\"},...]\n\n"
            + "4. ORDERING (Ordering) - Put in order\n" + "   - Elements to order\n" + "   - JSON: [{\"text\":\"...\",\"correctOrder\":1},...]\n\n"
            + "Use the createQuestion tool for each generated question.\n" + "If the content does not allow creating good questions, do not call the tool.";

    private static final ObjectMapper _objectMapper = new ObjectMapper( );

    @Inject
    @Named( "wiki-ai.chatModel" )
    private ChatModel _chatModel;

    public void processWorkflow( QuizGenerationWorkflow workflow )
    {
        AppLogService.info( "QuizGeneration: Starting workflow {} for quiz {}", workflow.getId( ), workflow.getIdQuiz( ) );

        List<Integer> pageIds = parsePageIds( workflow.getSelectedPageIds( ) );
        workflow.setStatus( WorkflowStatus.PROCESSING );
        workflow.setTotalPages( pageIds.size( ) );
        workflow.setProcessedPages( 0 );
        QuizGenerationWorkflowHome.update( workflow );

        try
        {
            AppLogService.info( "QuizGeneration: Found {} pages to process: {}", pageIds.size( ), pageIds );

            String strLocale = workflow.getLocale( ) != null ? workflow.getLocale( ) : "en";
            Locale locale = Locale.forLanguageTag( strLocale );

            for ( Integer pageId : pageIds )
            {
                processPage( workflow.getId( ), pageId, locale );
                workflow.setProcessedPages( workflow.getProcessedPages( ) + 1 );
                QuizGenerationWorkflowHome.update( workflow );
            }

            workflow.setStatus( WorkflowStatus.COMPLETED );
            workflow.setDateCompletion( new Timestamp( System.currentTimeMillis( ) ) );
            AppLogService.info( "QuizGeneration: Workflow {} completed successfully", workflow.getId( ) );
        }
        catch( Throwable t )
        {
            AppLogService.error( "QuizGeneration: Error processing workflow {}", workflow.getId( ), t );
            workflow.setStatus( WorkflowStatus.ERROR );
            workflow.setErrorMessage( t.getMessage( ) );
            workflow.setDateCompletion( new Timestamp( System.currentTimeMillis( ) ) );
        }

        QuizGenerationWorkflowHome.update( workflow );
    }

    private void processPage( int nWorkflowId, int nPageId, Locale locale )
    {
        AppLogService.info( "QuizGeneration: Processing page {}", nPageId );

        Optional<AbstractWikiItem> optPage = WikiItemHome.findByPrimaryKey( nPageId );
        if ( optPage.isEmpty( ) || optPage.get( ).getType( ) != WikiItemType.PAGE )
        {
            AppLogService.info( "QuizGeneration: Page {} not found or not a PAGE type", nPageId );
            return;
        }

        AbstractWikiItem page = optPage.get( );
        Revision revision = RevisionService.getCurrentRevision( nPageId );
        if ( revision == null )
        {
            AppLogService.info( "QuizGeneration: Page {} has no current revision", nPageId );
            return;
        }

        String content = revision.getContent( );
        if ( content == null || content.trim( ).isEmpty( ) )
        {
            AppLogService.info( "QuizGeneration: Page {} has empty content", nPageId );
            return;
        }

        String pageTitle = revision.getTitle( ) != null ? revision.getTitle( ) : page.getCode( );
        AppLogService.info( "QuizGeneration: Processing page '{}' with {} chars", pageTitle, content.length( ) );

        CreateQuizQuestionTool tool = new CreateQuizQuestionTool( nWorkflowId, nPageId );

        String systemPrompt = buildSystemPrompt( locale );
        QuizGenerator generator = AiServices.builder( QuizGenerator.class ).chatModel( _chatModel ).tools( tool )
                .systemMessageProvider( chatMemoryId -> systemPrompt ).build( );

        String prompt = "Page: " + pageTitle + "\n\nContent:\n" + content;
        AppLogService.info( "QuizGeneration: Calling AI for page {} with prompt length {}", nPageId, prompt.length( ) );

        String response = generator.generate( prompt );
        AppLogService.info( "QuizGeneration: AI response for page {}: {}", nPageId, response );
        AppLogService.info( "QuizGeneration: Tool generated {} questions for page {}", tool.getGeneratedQuestions( ).size( ), nPageId );
    }

    private String buildSystemPrompt( Locale locale )
    {
        StringBuilder prompt = new StringBuilder( SYSTEM_PROMPT );
        prompt.append( "\n\nRESPONSE LANGUAGE:\n" ).append( "The user's detected locale is: " ).append( locale.getDisplayLanguage( Locale.ENGLISH ) )
                .append( " (" ).append( locale.getLanguage( ) ).append( ").\n" ).append( "You MUST generate all questions, answers, and explanations in " )
                .append( locale.getDisplayLanguage( Locale.ENGLISH ) ).append( "." );
        return prompt.toString( );
    }

    public void importQuestion( int nGeneratedQuestionId, int nQuizId )
    {
        Optional<QuizGeneratedQuestion> optGenerated = QuizGeneratedQuestionHome.findByPrimaryKey( nGeneratedQuestionId );
        if ( optGenerated.isEmpty( ) )
        {
            return;
        }

        QuizGeneratedQuestion generated = optGenerated.get( );

        QuizQuestion question = new QuizQuestion( );
        question.setIdQuiz( nQuizId );
        question.setQuestionText( generated.getQuestionText( ) );
        question.setQuestionType( QuestionType.fromCode( generated.getQuestionType( ) ) );
        question.setExplanation( generated.getExplanation( ) );

        QuizService.createQuestion( question );

        List<QuizGeneratedAnswer> generatedAnswers = QuizGeneratedAnswerHome.getAnswersByQuestion( nGeneratedQuestionId );
        List<QuizAnswer> answers = new ArrayList<>( );
        for ( QuizGeneratedAnswer genAnswer : generatedAnswers )
        {
            QuizAnswer answer = new QuizAnswer( );
            answer.setIdQuestion( question.getId( ) );
            answer.setAnswerText( genAnswer.getAnswerText( ) );
            answer.setIsCorrect( genAnswer.getIsCorrect( ) );
            answer.setMatchTarget( genAnswer.getMatchTarget( ) );
            answer.setCorrectOrder( genAnswer.getCorrectOrder( ) );
            answer.setDisplayOrder( genAnswer.getDisplayOrder( ) );
            answers.add( answer );
        }
        QuizService.replaceAnswers( question.getId( ), answers );

        if ( generated.getIdSourcePage( ) > 0 )
        {
            QuizService.setQuestionSourcePages( question.getId( ), List.of( generated.getIdSourcePage( ) ) );
        }

        QuizGeneratedQuestionHome.markAsImported( nGeneratedQuestionId );
    }

    private List<Integer> parsePageIds( String strPageIds )
    {
        List<Integer> ids = new ArrayList<>( );
        if ( strPageIds == null || strPageIds.isEmpty( ) )
        {
            return ids;
        }
        try
        {
            return _objectMapper.readValue( strPageIds, new TypeReference<List<Integer>>( )
            {
            } );
        }
        catch( JsonProcessingException e )
        {
            AppLogService.error( "Error parsing page IDs", e );
            return ids;
        }
    }

}
