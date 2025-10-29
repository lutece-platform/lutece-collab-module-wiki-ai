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
package fr.paris.lutece.plugins.wiki.modules.ai.service.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import fr.paris.lutece.plugins.wiki.modules.ai.business.QuizGeneratedAnswer;
import fr.paris.lutece.plugins.wiki.modules.ai.business.QuizGeneratedAnswerHome;
import fr.paris.lutece.plugins.wiki.modules.ai.business.QuizGeneratedQuestion;
import fr.paris.lutece.plugins.wiki.modules.ai.business.QuizGeneratedQuestionHome;
import fr.paris.lutece.portal.service.util.AppLogService;

/**
 * Tool for creating quiz questions with their answers. This tool is used by the AI agent to generate quiz questions from page content.
 */
public class CreateQuizQuestionTool
{
    private static final String TOOL_DESCRIPTION = "Create a quiz question with its answers. Call this for each question you want to generate from the page content.";
    private static final String PARAM_QUESTION_TEXT_DESC = "The question text";
    private static final String PARAM_QUESTION_TYPE_DESC = "Question type: MCQ, TRUE_FALSE, MATCHING, or ORDERING";
    private static final String PARAM_EXPLANATION_DESC = "Explanation shown after the user answers";
    private static final String PARAM_ANSWERS_JSON_DESC = "JSON array of answers. Format depends on type: MCQ/TRUE_FALSE: [{\"text\":\"...\",\"correct\":true/false}], MATCHING: [{\"text\":\"...\",\"matchTarget\":\"...\"}], ORDERING: [{\"text\":\"...\",\"correctOrder\":1}]";

    private static final String LOG_CREATE_QUESTION_CALLED = "QuizGeneration Tool: createQuestion called with type={}, question={}...";
    private static final String LOG_QUESTION_CREATED = "QuizGeneration Tool: Question created with ID {} and {} answers";
    private static final String LOG_ERROR_CREATING_QUESTION = "QuizGeneration Tool: Error creating quiz question";
    private static final String LOG_ERROR_PARSING_ANSWERS = "Error parsing answers JSON";

    private static final String MSG_QUESTION_CREATED_SUCCESS = "Question created successfully: ";
    private static final String MSG_ERROR_CREATING_QUESTION = "Error creating question: ";

    private static final String JSON_KEY_TEXT = "text";
    private static final String JSON_KEY_MATCH_TARGET = "matchTarget";
    private static final String JSON_KEY_CORRECT_ORDER = "correctOrder";
    private static final String JSON_KEY_CORRECT = "correct";

    private static final String QUESTION_TYPE_MATCHING = "MATCHING";
    private static final String QUESTION_TYPE_ORDERING = "ORDERING";

    private static final int MAX_LOG_QUESTION_LENGTH = 50;

    private static final ObjectMapper _objectMapper = new ObjectMapper( );

    private final int _nWorkflowId;
    private int _nCurrentPageId;
    private final List<QuizGeneratedQuestion> _listGeneratedQuestions = new ArrayList<>( );

    /**
     * Constructor for CreateQuizQuestionTool.
     *
     * @param nWorkflowId
     *            the workflow identifier
     * @param nCurrentPageId
     *            the current page identifier
     */
    public CreateQuizQuestionTool( int nWorkflowId, int nCurrentPageId )
    {
        _nWorkflowId = nWorkflowId;
        _nCurrentPageId = nCurrentPageId;
    }

    /**
     * Creates a quiz question with its answers.
     *
     * @param questionText
     *            the question text
     * @param questionType
     *            the question type (MCQ, TRUE_FALSE, MATCHING, or ORDERING)
     * @param explanation
     *            the explanation shown after the user answers
     * @param answersJson
     *            the JSON array of answers
     * @return a success or error message
     */
    @Tool( TOOL_DESCRIPTION )
    public String createQuestion( @P( PARAM_QUESTION_TEXT_DESC ) String questionText, @P( PARAM_QUESTION_TYPE_DESC ) String questionType,
            @P( PARAM_EXPLANATION_DESC ) String explanation, @P( PARAM_ANSWERS_JSON_DESC ) String answersJson )
    {
        String truncatedQuestion = questionText.substring( 0, Math.min( MAX_LOG_QUESTION_LENGTH, questionText.length( ) ) );
        AppLogService.info( LOG_CREATE_QUESTION_CALLED, questionType, truncatedQuestion );

        try
        {
            QuizGeneratedQuestion question = new QuizGeneratedQuestion( );
            question.setIdWorkflow( _nWorkflowId );
            question.setIdSourcePage( _nCurrentPageId );
            question.setQuestionText( questionText );
            question.setQuestionType( questionType.toUpperCase( ) );
            question.setExplanation( explanation );

            QuizGeneratedQuestionHome.create( question );

            List<QuizGeneratedAnswer> answers = parseAndCreateAnswers( answersJson, question.getId( ), questionType.toUpperCase( ) );
            question.setAnswers( answers );

            _listGeneratedQuestions.add( question );

            AppLogService.info( LOG_QUESTION_CREATED, question.getId( ), answers.size( ) );

            return MSG_QUESTION_CREATED_SUCCESS + truncatedQuestion + "...";
        }
        catch( Exception e )
        {
            AppLogService.error( LOG_ERROR_CREATING_QUESTION, e );
            return MSG_ERROR_CREATING_QUESTION + e.getMessage( );
        }
    }

    /**
     * Returns the list of generated questions.
     *
     * @return the list of generated questions
     */
    public List<QuizGeneratedQuestion> getGeneratedQuestions( )
    {
        return _listGeneratedQuestions;
    }

    /**
     * Sets the current page identifier.
     *
     * @param nPageId
     *            the page identifier to set
     */
    public void setCurrentPageId( int nPageId )
    {
        _nCurrentPageId = nPageId;
    }

    /**
     * Parses the JSON answers and creates QuizGeneratedAnswer objects.
     *
     * @param answersJson
     *            the JSON string containing the answers
     * @param nQuestionId
     *            the question identifier
     * @param questionType
     *            the question type
     * @return the list of created answers
     */
    private List<QuizGeneratedAnswer> parseAndCreateAnswers( String answersJson, int nQuestionId, String questionType )
    {
        List<QuizGeneratedAnswer> answers = new ArrayList<>( );

        if ( answersJson == null || answersJson.isEmpty( ) )
        {
            return answers;
        }

        try
        {
            List<Map<String, Object>> rawAnswers = _objectMapper.readValue( answersJson, new TypeReference<>( )
            {
            } );

            int displayOrder = 0;
            for ( Map<String, Object> raw : rawAnswers )
            {
                QuizGeneratedAnswer answer = createAnswerFromRawData( raw, nQuestionId, questionType, displayOrder++ );
                QuizGeneratedAnswerHome.create( answer );
                answers.add( answer );
            }
        }
        catch( JsonProcessingException e )
        {
            AppLogService.error( LOG_ERROR_PARSING_ANSWERS, e );
        }

        return answers;
    }

    /**
     * Creates a QuizGeneratedAnswer from raw data map.
     *
     * @param raw
     *            the raw data map
     * @param nQuestionId
     *            the question identifier
     * @param questionType
     *            the question type
     * @param displayOrder
     *            the display order
     * @return the created QuizGeneratedAnswer
     */
    private QuizGeneratedAnswer createAnswerFromRawData( Map<String, Object> raw, int nQuestionId, String questionType, int displayOrder )
    {
        QuizGeneratedAnswer answer = new QuizGeneratedAnswer( );
        answer.setIdQuestion( nQuestionId );
        answer.setAnswerText( (String) raw.get( JSON_KEY_TEXT ) );
        answer.setDisplayOrder( displayOrder );

        if ( QUESTION_TYPE_MATCHING.equals( questionType ) )
        {
            answer.setMatchTarget( (String) raw.get( JSON_KEY_MATCH_TARGET ) );
        }
        else if ( QUESTION_TYPE_ORDERING.equals( questionType ) )
        {
            Object correctOrder = raw.get( JSON_KEY_CORRECT_ORDER );
            if ( correctOrder instanceof Integer )
            {
                answer.setCorrectOrder( (Integer) correctOrder );
            }
            else if ( correctOrder instanceof Number )
            {
                answer.setCorrectOrder( ( (Number) correctOrder ).intValue( ) );
            }
        }
        else
        {
            Object correct = raw.get( JSON_KEY_CORRECT );
            answer.setIsCorrect( Boolean.TRUE.equals( correct ) );
        }

        return answer;
    }
}
