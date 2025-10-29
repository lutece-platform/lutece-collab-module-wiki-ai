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
package fr.paris.lutece.plugins.wiki.modules.ai.service.rag;

import java.io.IOException;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchConfigurationKnn;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;
import fr.paris.lutece.portal.service.init.ShutdownService;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;

/**
 * Service for managing Elasticsearch connections and embedding stores for wiki AI. Implements singleton pattern and ShutdownService for proper resource
 * cleanup.
 */
public class ElasticsearchService implements ShutdownService
{
    private static final String PROPERTY_ES_HOST = "wiki.ai.elasticsearch.host";
    private static final String PROPERTY_ES_PORT = "wiki.ai.elasticsearch.port";
    private static final String PROPERTY_ES_PROTOCOL = "wiki.ai.elasticsearch.protocol";
    private static final String PROPERTY_ES_USERNAME = "wiki.ai.elasticsearch.username";
    private static final String PROPERTY_ES_PASSWORD = "wiki.ai.elasticsearch.password";
    private static final String PROPERTY_ELASTICSEARCH_INDEX_PREFIX = "wiki.ai.elasticsearch.index.prefix";

    private static final String DEFAULT_INDEX_PREFIX = "wiki_ai_";
    private static final int DEFAULT_ES_PORT = 9200;
    private static final String INDEX_SUFFIX = "embeddings";
    private static final String SERVER_URL_FORMAT = "%s://%s:%d";

    private static final String ES_HOST = AppPropertiesService.getProperty( PROPERTY_ES_HOST );
    private static final int ES_PORT = AppPropertiesService.getPropertyInt( PROPERTY_ES_PORT, DEFAULT_ES_PORT );
    private static final String ES_PROTOCOL = AppPropertiesService.getProperty( PROPERTY_ES_PROTOCOL );
    private static final String ES_USERNAME = AppPropertiesService.getProperty( PROPERTY_ES_USERNAME );
    private static final String ES_PASSWORD = AppPropertiesService.getProperty( PROPERTY_ES_PASSWORD );
    private static final String INDEX_PREFIX = AppPropertiesService.getProperty( PROPERTY_ELASTICSEARCH_INDEX_PREFIX, DEFAULT_INDEX_PREFIX );

    private static final String LOG_ELASTICSEARCH_URL = "Elasticsearch URL: ";
    private static final String LOG_CREATED_INDEX = "Created Elasticsearch index: ";
    private static final String LOG_CLIENT_CLOSED = "Elasticsearch clients closed successfully";
    private static final String LOG_ERROR_CLOSING = "Error closing Elasticsearch clients: ";
    private static final String ERROR_CREATING_INDEX = "Error creating Elasticsearch index: ";
    private static final String ERROR_FAILED_CREATE_INDEX = "Failed to create Elasticsearch index";

    private static final int NUM_CANDIDATES = 100;
    private static final int HNSW_M = 16;
    private static final int HNSW_EF_CONSTRUCTION = 100;
    private static final String HNSW_TYPE = "hnsw";
    private static final String MAPPING_PROPERTY_TEXT = "text";
    private static final String MAPPING_PROPERTY_VECTOR = "vector";

    private static class SingletonHolder
    {
        static final ElasticsearchService INSTANCE = new ElasticsearchService( );
    }

    private final RestClient _restClient;
    private final RestClientTransport _transport;
    private final ElasticsearchClient _client;
    private final String _indexName;

    /**
     * Private constructor to ensure singleton pattern.
     */
    private ElasticsearchService( )
    {
        _restClient = createRestClient( );
        _transport = new RestClientTransport( _restClient, new JacksonJsonpMapper( ) );
        _client = new ElasticsearchClient( _transport );
        _indexName = INDEX_PREFIX + INDEX_SUFFIX;

        createIndexIfNotExists( );
    }

    /**
     * Creates and configures the RestClient instance with authentication if needed.
     *
     * @return configured RestClient instance
     */
    private RestClient createRestClient( )
    {
        String serverUrl = String.format( SERVER_URL_FORMAT, ES_PROTOCOL, ES_HOST, ES_PORT );
        AppLogService.info( LOG_ELASTICSEARCH_URL + serverUrl );

        RestClientBuilder builder = RestClient.builder( HttpHost.create( serverUrl ) );

        if ( ES_USERNAME != null && !ES_USERNAME.isEmpty( ) )
        {
            CredentialsProvider creds = new BasicCredentialsProvider( );
            creds.setCredentials( AuthScope.ANY, new UsernamePasswordCredentials( ES_USERNAME, ES_PASSWORD ) );
            builder.setHttpClientConfigCallback( h -> h.setDefaultCredentialsProvider( creds ) );
        }

        return builder.build( );
    }

    /**
     * Returns the singleton instance of ElasticsearchService.
     *
     * @return the singleton instance
     */
    public static ElasticsearchService getInstance( )
    {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Creates an embedding store for wiki content.
     *
     * @return the configured embedding store
     */
    public EmbeddingStore<TextSegment> createEmbeddingStore( )
    {
        ElasticsearchConfigurationKnn configuration = ElasticsearchConfigurationKnn.builder( ).numCandidates( NUM_CANDIDATES ).build( );

        return new ElasticsearchEmbeddingStore( configuration, _restClient, _indexName );
    }

    /**
     * Gets the Elasticsearch client.
     *
     * @return the Elasticsearch client
     */
    public ElasticsearchClient getClient( )
    {
        return _client;
    }

    /**
     * Gets the index name.
     *
     * @return the index name
     */
    public String getIndexName( )
    {
        return _indexName;
    }

    /**
     * Creates the Elasticsearch index if it does not exist.
     */
    public final void createIndexIfNotExists( )
    {
        try
        {
            boolean indexExists = _client.indices( ).exists( c -> c.index( _indexName ) ).value( );

            if ( !indexExists )
            {
                _client.indices( ).create( c -> c.index( _indexName ).mappings( m -> m.properties( MAPPING_PROPERTY_TEXT, p -> p.text( t -> t ) ).properties(
                        MAPPING_PROPERTY_VECTOR,
                        p -> p.denseVector( dv -> dv.indexOptions( dvio -> dvio.m( HNSW_M ).efConstruction( HNSW_EF_CONSTRUCTION ).type( HNSW_TYPE ) ) ) ) ) );

                AppLogService.info( LOG_CREATED_INDEX + _indexName );
            }
        }
        catch( ElasticsearchException | IOException e )
        {
            AppLogService.error( ERROR_CREATING_INDEX + _indexName, e );
            throw new RuntimeException( ERROR_FAILED_CREATE_INDEX, e );
        }
    }

    @Override
    public String getName( )
    {
        return "ElasticsearchService";
    }

    @Override
    public void process( )
    {
        try
        {
            if ( _client != null )
            {
                _client.shutdown( );
            }
            if ( _transport != null )
            {
                _transport.close( );
            }
            if ( _restClient != null )
            {
                _restClient.close( );
            }
            AppLogService.info( LOG_CLIENT_CLOSED );
        }
        catch( IOException e )
        {
            AppLogService.error( LOG_ERROR_CLOSING + e.getMessage( ), e );
        }
    }
}
