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

import java.util.List;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import fr.paris.lutece.plugins.wiki.business.item.AbstractWikiItem;
import fr.paris.lutece.plugins.wiki.modules.ai.service.rag.ElasticsearchService;
import fr.paris.lutece.plugins.wiki.modules.ai.service.rag.components.WikiContentRetriever;
import fr.paris.lutece.plugins.wiki.service.WikiItemService;
import fr.paris.lutece.portal.service.security.LuteceUser;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Tool for searching within a specific wiki space using semantic search.
 */
public class WikiSearchInSpaceTool extends AbstractWikiTool
{
    private static final String PROPERTY_RAG_MAX_RESULTS = "wiki.ai.rag.max.results";
    private static final String PROPERTY_RAG_MIN_SCORE = "wiki.ai.rag.min.score";
    private static final int DEFAULT_MAX_RESULTS = 5;
    private static final double DEFAULT_MIN_SCORE = 0.5;

    private static final int MAX_RESULTS = AppPropertiesService.getPropertyInt( PROPERTY_RAG_MAX_RESULTS, DEFAULT_MAX_RESULTS );
    private static final double MIN_SCORE = Double
            .parseDouble( AppPropertiesService.getProperty( PROPERTY_RAG_MIN_SCORE, String.valueOf( DEFAULT_MIN_SCORE ) ) );

    private static final String ERROR_SPACE_CODE_REQUIRED = "Error: space code is required";
    private static final String ERROR_QUERY_REQUIRED = "Error: search query is required";
    private static final String MSG_NO_RESULTS = "No results found in space '%s' for query: %s";
    private static final String MSG_RESULTS_HEADER = "# Search Results in Space '%s'\n\n";
    private static final String MSG_QUERY_LABEL = "Query: %s\n\n";

    private static final String FILTER_KEY_SPACE_CODE = "space_code";

    private static final String TOOL_DESCRIPTION = "Searches for information within a specific wiki space. "
            + "Use this after browseSpace when you want to search only within a particular space. "
            + "More precise than global search when you know which space contains the answer.";
    private static final String PARAM_SPACE_CODE_DESCRIPTION = "The code of the space to search in";
    private static final String PARAM_QUERY_DESCRIPTION = "The search query";

    /**
     * Constructor.
     *
     * @param user
     *            the current Lutece user for access control
     */
    public WikiSearchInSpaceTool( LuteceUser user )
    {
        super( user );
    }

    /**
     * Searches for information within a specific wiki space.
     *
     * @param spaceCode
     *            the code of the space to search in
     * @param query
     *            the search query
     * @return formatted search results with source codes and content excerpts
     */
    @Tool( TOOL_DESCRIPTION )
    public String searchInSpace( @P( PARAM_SPACE_CODE_DESCRIPTION ) String spaceCode, @P( PARAM_QUERY_DESCRIPTION ) String query )
    {
        if ( spaceCode == null || spaceCode.trim( ).isEmpty( ) )
        {
            return ERROR_SPACE_CODE_REQUIRED;
        }
        if ( query == null || query.trim( ).isEmpty( ) )
        {
            return ERROR_QUERY_REQUIRED;
        }

        clearSources( );

        EmbeddingModel embeddingModel = CDI.current( ).select( EmbeddingModel.class, jakarta.enterprise.inject.literal.NamedLiteral.of( "wiki-ai.embeddingModel" ) ).get( );
        EmbeddingStore<TextSegment> embeddingStore = CDI.current( ).select( ElasticsearchService.class ).get( ).createEmbeddingStore( );

        Filter spaceFilter = new IsEqualTo( FILTER_KEY_SPACE_CODE, spaceCode.trim( ) );

        ContentRetriever baseRetriever = EmbeddingStoreContentRetriever.builder( ).embeddingStore( embeddingStore ).embeddingModel( embeddingModel )
                .maxResults( MAX_RESULTS ).minScore( MIN_SCORE ).filter( spaceFilter ).build( );

        ContentRetriever secureRetriever = new WikiContentRetriever( baseRetriever, _user );

        List<Content> results = secureRetriever.retrieve( Query.from( query ) );

        if ( results.isEmpty( ) )
        {
            return String.format( MSG_NO_RESULTS, spaceCode, query );
        }

        StringBuilder sb = new StringBuilder( );
        sb.append( String.format( MSG_RESULTS_HEADER, spaceCode ) );
        sb.append( String.format( MSG_QUERY_LABEL, query ) );
        sb.append( String.format( MSG_FOUND_SOURCES, results.size( ) ) );

        for ( Content content : results )
        {
            TextSegment segment = content.textSegment( );
            String code = extractMetadata( segment, METADATA_KEY_CODE );
            String title = extractMetadata( segment, METADATA_KEY_TITLE );

            if ( code == null )
            {
                continue;
            }

            AbstractWikiItem item = WikiItemService.findByCode( code );
            if ( item != null )
            {
                addSource( item, title );
            }
            appendSourceResult( sb, code, title, segment.text( ) );
        }

        sb.append( MSG_CITATION_INSTRUCTION );
        return sb.toString( );
    }

    /**
     * Extracts a metadata value from a text segment.
     *
     * @param segment
     *            the text segment containing metadata
     * @param key
     *            the metadata key to extract
     * @return the metadata value, or null if not found
     */
    private String extractMetadata( TextSegment segment, String key )
    {
        if ( segment.metadata( ) != null )
        {
            return segment.metadata( ).getString( key );
        }
        return null;
    }
}
