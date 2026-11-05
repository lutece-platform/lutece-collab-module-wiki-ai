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
package fr.paris.lutece.plugins.wiki.modules.ai.service.chat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.input.PromptTemplate;
import fr.paris.lutece.plugins.wiki.modules.ai.business.AiFeature;
import fr.paris.lutece.plugins.wiki.modules.ai.business.AiFeatureHome;
import fr.paris.lutece.plugins.wiki.modules.ai.service.model.ModelService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;

/**
 * Service for managing AI features.
 */
public class AiFeatureService
{
    private static final String ERROR_FEATURE_NOT_FOUND = "Feature not found with id: ";
    private static final String ERROR_FEATURE_INACTIVE = "Feature is not active: ";
    private static final String VARIABLE_TEXT = "text";
    private static final String PROPERTY_EDIT_SYSTEM_PROMPT = "wiki.ai.edit.systemPrompt";
    private static final String DEFAULT_EDIT_SYSTEM_PROMPT = "You are the writing assistant of a wiki editor. The user selected a Markdown excerpt and makes a request about it. "
            + "Answer only with Markdown: no introduction, no comment, no code fence around the whole answer. "
            + "When the request changes the excerpt, answer with the whole new excerpt. "
            + "When the request creates something new from the excerpt (a diagram, a table, a summary, a title...), start your answer with ADD: "
            + "followed only by that new content, without the excerpt. "
            + "Keep the language of the excerpt unless asked otherwise, and keep its Markdown formatting, links and images. "
            + "When the request cannot be done on this excerpt without inventing information, answer only CANNOT: followed by one short sentence, "
            + "in the language of the excerpt, that tells the user why.";
    private static final String EDIT_FIRST_REQUEST = "<excerpt>\n%s\n</excerpt>\n\nInstruction: %s";
    private static final String EDIT_NEXT_REQUEST = "Instruction: %s\n\nApply it to your last version and answer with the whole new version.";

    private static class SingletonHolder
    {
        static final AiFeatureService INSTANCE = new AiFeatureService( );
    }

    /**
     * Private constructor.
     */
    private AiFeatureService( )
    {
    }

    /**
     * Gets the singleton instance of AiFeatureService.
     *
     * @return the instance of AiFeatureService
     */
    public static AiFeatureService getInstance( )
    {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Retrieves all active features.
     *
     * @return the list of all active AI features
     */
    public List<AiFeature> getAllFeatures( )
    {
        return AiFeatureHome.getActiveFeatures( );
    }

    /**
     * Retrieves features by type.
     *
     * @param strType
     *            the feature type
     * @return the list of AI features matching the type
     */
    public List<AiFeature> getFeaturesByType( String strType )
    {
        return AiFeatureHome.getFeaturesByType( strType );
    }

    /**
     * Executes an AI feature with the provided text using streaming.
     *
     * @param nFeatureId
     *            the ID of the feature to execute
     * @param strText
     *            the text to process
     * @param handler
     *            the streaming response handler
     * @throws IllegalArgumentException
     *             if the feature is not found
     * @throws IllegalStateException
     *             if the feature is not active
     */
    public void executeFeature( int nFeatureId, String strText, StreamingChatResponseHandler handler )
    {
        ModelService.getInstance( ).getStreamingChatModel( ).chat( buildPrompt( findActiveFeature( nFeatureId ).getPromptTemplate( ), strText ), handler );
    }

    /**
     * Rewrites a selection with the model, as a conversation: the first step applies a feature or an instruction to the selection, each
     * next step refines the previous answer.
     *
     * @param strText
     *            the selected Markdown
     * @param steps
     *            the steps, the last one being the request to run
     * @param handler
     *            the streaming response handler
     * @throws IllegalArgumentException
     *             if a feature is not found
     * @throws IllegalStateException
     *             if a feature is not active
     */
    public void editSelection( String strText, List<EditStep> steps, StreamingChatResponseHandler handler )
    {
        List<ChatMessage> messages = new ArrayList<>( );
        messages.add( SystemMessage.from( AppPropertiesService.getProperty( PROPERTY_EDIT_SYSTEM_PROMPT, DEFAULT_EDIT_SYSTEM_PROMPT ) ) );
        for ( int i = 0; i < steps.size( ); i++ )
        {
            EditStep step = steps.get( i );
            messages.add( UserMessage.from( i == 0 ? firstEditRequest( strText, step ) : nextEditRequest( step ) ) );
            if ( step.getResult( ) != null )
            {
                messages.add( AiMessage.from( step.getResult( ) ) );
            }
        }
        ModelService.getInstance( ).getStreamingChatModel( ).chat( messages, handler );
    }

    /**
     * Builds the request of the first step, on the selection.
     *
     * @param strText
     *            the selected Markdown
     * @param step
     *            the first step
     * @return the request
     */
    private String firstEditRequest( String strText, EditStep step )
    {
        if ( step.getFeatureId( ) != null )
        {
            return buildPrompt( findActiveFeature( step.getFeatureId( ) ).getPromptTemplate( ), strText );
        }
        return String.format( EDIT_FIRST_REQUEST, strText, step.getInstruction( ) );
    }

    /**
     * Builds the request of a refining step, on the previous answer.
     *
     * @param step
     *            the step
     * @return the request
     */
    private String nextEditRequest( EditStep step )
    {
        String strInstruction = step.getFeatureId( ) != null ? findActiveFeature( step.getFeatureId( ) ).getName( ) : step.getInstruction( );
        return String.format( EDIT_NEXT_REQUEST, strInstruction );
    }

    /**
     * Finds an active feature.
     *
     * @param nFeatureId
     *            the feature identifier
     * @return the feature
     * @throws IllegalArgumentException
     *             if the feature is not found
     * @throws IllegalStateException
     *             if the feature is not active
     */
    private AiFeature findActiveFeature( int nFeatureId )
    {
        AiFeature feature = AiFeatureHome.findByPrimaryKey( nFeatureId )
                .orElseThrow( ( ) -> new IllegalArgumentException( ERROR_FEATURE_NOT_FOUND + nFeatureId ) );
        if ( !feature.isActive( ) )
        {
            throw new IllegalStateException( ERROR_FEATURE_INACTIVE + nFeatureId );
        }
        return feature;
    }

    /**
     * Builds a prompt from a template and text.
     *
     * @param strTemplate
     *            the prompt template
     * @param strText
     *            the text to inject into the template
     * @return the built prompt
     */
    private String buildPrompt( String strTemplate, String strText )
    {
        if ( strTemplate == null || strTemplate.trim( ).isEmpty( ) )
        {
            return strText;
        }

        Map<String, Object> variables = new HashMap<>( );
        variables.put( VARIABLE_TEXT, strText );

        PromptTemplate template = PromptTemplate.from( strTemplate );
        return template.apply( variables ).text( );
    }
}
