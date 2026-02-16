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
package fr.paris.lutece.plugins.wiki.modules.ai.service.model;

import java.util.HashMap;
import java.util.Map;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

/**
 * Service for managing LangChain4j models. Provides CDI producers for embedding, chat, and streaming chat models.
 */
@ApplicationScoped
public class ModelService
{
    private static final String PROPERTY_PREFIX = "wiki.ai.model.";
    private static final String PROPERTY_EMBEDDING_MODEL_CLASS = PROPERTY_PREFIX + "embedding.class";
    private static final String PROPERTY_CHAT_MODEL_CLASS = PROPERTY_PREFIX + "chat.class";
    private static final String PROPERTY_STREAMING_CHAT_MODEL_CLASS = PROPERTY_PREFIX + "streaming.class";
    private static final String PROPERTY_EMBEDDING_PREFIX = PROPERTY_PREFIX + "embedding.";
    private static final String PROPERTY_CHAT_PREFIX = PROPERTY_PREFIX + "chat.";
    private static final String PROPERTY_STREAMING_PREFIX = PROPERTY_PREFIX + "streaming.";

    private static final String DEFAULT_EMBEDDING_CLASS = "dev.langchain4j.model.azure.AzureOpenAiEmbeddingModel";
    private static final String DEFAULT_CHAT_CLASS = "dev.langchain4j.model.azure.AzureOpenAiChatModel";
    private static final String DEFAULT_STREAMING_CLASS = "dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel";

    private static final String ERROR_CREATING_MODEL = "Error creating model: ";
    private static final String ERROR_CREATING_MODEL_PARAMETERIZED = "Error creating model: {}";

    private EmbeddingModel _embeddingModel;
    private ChatModel _chatModel;
    private StreamingChatModel _streamingChatModel;

    /**
     * Retrieves the embedding model
     *
     * @return The embedding model
     */
    @Produces
    @Named( "wiki-ai.embeddingModel" )
    @ApplicationScoped
    public EmbeddingModel getEmbeddingModel( )
    {
        if ( _embeddingModel == null )
        {
            _embeddingModel = createModel( PROPERTY_EMBEDDING_MODEL_CLASS, DEFAULT_EMBEDDING_CLASS, PROPERTY_EMBEDDING_PREFIX );
        }
        return _embeddingModel;
    }

    /**
     * Retrieves the chat model
     *
     * @return The chat model
     */
    @Produces
    @Named( "wiki-ai.chatModel" )
    @ApplicationScoped
    public ChatModel getChatModel( )
    {
        if ( _chatModel == null )
        {
            _chatModel = createModel( PROPERTY_CHAT_MODEL_CLASS, DEFAULT_CHAT_CLASS, PROPERTY_CHAT_PREFIX );
        }
        return _chatModel;
    }

    /**
     * Retrieves the streaming chat model
     *
     * @return The streaming chat model
     */
    @Produces
    @Named( "wiki-ai.streamingChatModel" )
    @ApplicationScoped
    public StreamingChatModel getStreamingChatModel( )
    {
        if ( _streamingChatModel == null )
        {
            _streamingChatModel = createModel( PROPERTY_STREAMING_CHAT_MODEL_CLASS, DEFAULT_STREAMING_CLASS, PROPERTY_STREAMING_PREFIX );
        }
        return _streamingChatModel;
    }

    /**
     * Creates a model using LangChain4jModelFactory with properties from AppPropertiesService
     *
     * @param classProperty
     *            the property key for the model class name
     * @param defaultClass
     *            the default class name
     * @param propertyPrefix
     *            the prefix for model properties (endpoint, apiKey, etc.)
     * @return the created model instance
     */
    @SuppressWarnings( "unchecked" )
    private <T> T createModel( String classProperty, String defaultClass, String propertyPrefix )
    {
        try
        {
            String strModelClass = AppPropertiesService.getProperty( classProperty, defaultClass );
            Map<String, Object> properties = new HashMap<>( );

            String strEndpoint = AppPropertiesService.getProperty( propertyPrefix + "endpoint" );
            if ( strEndpoint != null )
            {
                properties.put( "endpoint", strEndpoint );
            }

            String strApiKey = AppPropertiesService.getProperty( propertyPrefix + "apiKey" );
            if ( strApiKey != null )
            {
                properties.put( "apiKey", strApiKey );
            }

            String strDeploymentName = AppPropertiesService.getProperty( propertyPrefix + "deploymentName" );
            if ( strDeploymentName != null )
            {
                properties.put( "deploymentName", strDeploymentName );
            }

            String strTemperature = AppPropertiesService.getProperty( propertyPrefix + "temperature" );
            if ( strTemperature != null )
            {
                properties.put( "temperature", strTemperature );
            }

            return (T) LangChain4jModelFactory.createModel( strModelClass, properties );
        }
        catch( Exception e )
        {
            AppLogService.error( ERROR_CREATING_MODEL_PARAMETERIZED, classProperty, e );
            throw new RuntimeException( ERROR_CREATING_MODEL + classProperty, e );
        }
    }
}
