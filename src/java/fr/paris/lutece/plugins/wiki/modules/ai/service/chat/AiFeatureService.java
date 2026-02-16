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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.input.PromptTemplate;
import fr.paris.lutece.plugins.wiki.modules.ai.business.AiFeature;
import fr.paris.lutece.plugins.wiki.modules.ai.business.AiFeatureHome;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Service for managing AI features.
 */
@ApplicationScoped
public class AiFeatureService
{
    private static final String ERROR_FEATURE_NOT_FOUND = "Feature not found with id: ";
    private static final String ERROR_FEATURE_INACTIVE = "Feature is not active: ";
    private static final String VARIABLE_TEXT = "text";

    @Inject
    @Named( "wiki-ai.streamingChatModel" )
    private StreamingChatModel _streamingChatModel;

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
        Optional<AiFeature> optFeature = AiFeatureHome.findByPrimaryKey( nFeatureId );

        if ( !optFeature.isPresent( ) )
        {
            throw new IllegalArgumentException( ERROR_FEATURE_NOT_FOUND + nFeatureId );
        }

        AiFeature feature = optFeature.get( );

        if ( !feature.isActive( ) )
        {
            throw new IllegalStateException( ERROR_FEATURE_INACTIVE + nFeatureId );
        }

        String prompt = buildPrompt( feature.getPromptTemplate( ), strText );
        _streamingChatModel.chat( prompt, handler );
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
