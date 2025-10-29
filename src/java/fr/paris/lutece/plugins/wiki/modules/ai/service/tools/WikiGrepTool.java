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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import fr.paris.lutece.plugins.wiki.business.item.AbstractWikiItem;
import fr.paris.lutece.plugins.wiki.modules.ai.service.rag.ElasticsearchService;
import fr.paris.lutece.plugins.wiki.service.WikiItemService;
import fr.paris.lutece.plugins.wiki.service.security.WikiAccessControlService;
import fr.paris.lutece.portal.service.security.LuteceUser;
import fr.paris.lutece.portal.service.util.AppPropertiesService;

public class WikiGrepTool extends AbstractWikiTool
{
    private static final String PROPERTY_ELASTICSEARCH_INDEX_PREFIX = "wiki.ai.elasticsearch.index.prefix";
    private static final String DEFAULT_INDEX_PREFIX = "wiki_ai_";
    private static final String INDEX_SUFFIX = "embeddings";
    private static final int MAX_RESULTS = 50;
    private static final int CONTEXT_SIZE = 150;

    private static final String FIELD_TEXT = "text";
    private static final String FIELD_METADATA = "metadata";
    private static final String FIELD_METADATA_SPACE_CODE_KEYWORD = "metadata.space_code.keyword";
    private static final String METADATA_KEY_CHUNK_INDEX = "chunk_index";

    private static final String TOOL_DESCRIPTION = "Searches for exact term matches in wiki content. "
            + "Returns each match with context (like grep). Use readContent(code) to read the full item content.";
    private static final String PARAM_TERM_DESCRIPTION = "The exact term or phrase to search for";
    private static final String PARAM_SPACE_CODE_DESCRIPTION = "Optional: space code to limit search (leave empty for all spaces)";

    private final String _indexName;

    public WikiGrepTool( LuteceUser user )
    {
        super( user );
        String indexPrefix = AppPropertiesService.getProperty( PROPERTY_ELASTICSEARCH_INDEX_PREFIX, DEFAULT_INDEX_PREFIX );
        _indexName = indexPrefix + INDEX_SUFFIX;
    }

    @Tool( TOOL_DESCRIPTION )
    public String grep( @P( PARAM_TERM_DESCRIPTION ) String term, @P( PARAM_SPACE_CODE_DESCRIPTION ) String spaceCode )
    {
        if ( term == null || term.trim( ).isEmpty( ) )
        {
            return "Error: search term is required";
        }

        try
        {
            ElasticsearchClient client = ElasticsearchService.getInstance( ).getClient( );
            String searchTerm = term.trim( );

            BoolQuery.Builder boolQuery = new BoolQuery.Builder( );
            boolQuery.must( m -> m.match( t -> t.field( FIELD_TEXT ).query( searchTerm ) ) );

            boolean hasSpaceFilter = spaceCode != null && !spaceCode.trim( ).isEmpty( );
            if ( hasSpaceFilter )
            {
                boolQuery.filter( f -> f.term( t -> t.field( FIELD_METADATA_SPACE_CODE_KEYWORD ).value( spaceCode.trim( ) ) ) );
            }

            SearchResponse<Map<String, Object>> response = client.search(
                    s -> s.index( _indexName ).query( Query.of( q -> q.bool( boolQuery.build( ) ) ) ).size( MAX_RESULTS ),
                    (Class<Map<String, Object>>) (Class<?>) Map.class );

            if ( response.hits( ).hits( ).isEmpty( ) )
            {
                String scope = hasSpaceFilter ? " in space '" + spaceCode + "'" : "";
                return "No matches found for '" + searchTerm + "'" + scope;
            }

            List<GrepMatch> matches = processHits( response, searchTerm );

            if ( matches.isEmpty( ) )
            {
                return "No accessible matches found for '" + searchTerm + "'";
            }

            return buildResponse( searchTerm, spaceCode, matches );
        }
        catch( ElasticsearchException | IOException e )
        {
            return "Error during grep search: " + e.getMessage( );
        }
    }

    private List<GrepMatch> processHits( SearchResponse<Map<String, Object>> response, String searchTerm )
    {
        List<GrepMatch> matches = new ArrayList<>( );
        Set<String> processedCodes = new HashSet<>( );

        for ( Hit<Map<String, Object>> hit : response.hits( ).hits( ) )
        {
            Map<String, Object> source = hit.source( );
            if ( source == null )
            {
                continue;
            }

            @SuppressWarnings( "unchecked" )
            Map<String, Object> metadata = (Map<String, Object>) source.get( FIELD_METADATA );
            if ( metadata == null )
            {
                continue;
            }

            String code = (String) metadata.get( METADATA_KEY_CODE );
            String title = (String) metadata.get( METADATA_KEY_TITLE );
            String chunkIndex = (String) metadata.get( METADATA_KEY_CHUNK_INDEX );
            String text = (String) source.get( FIELD_TEXT );

            if ( code == null || text == null )
            {
                continue;
            }

            AbstractWikiItem item = WikiItemService.findByCode( code );
            if ( item == null || !WikiAccessControlService.canView( _user, item ) )
            {
                processedCodes.add( code );
                continue;
            }

            String excerpt = extractCenteredExcerpt( text, searchTerm );
            matches.add( new GrepMatch( item, title, chunkIndex, excerpt ) );
            processedCodes.add( code );
        }

        return matches;
    }

    private String extractCenteredExcerpt( String text, String searchTerm )
    {
        int pos = text.toLowerCase( ).indexOf( searchTerm.toLowerCase( ) );
        if ( pos == -1 )
        {
            return text.length( ) > CONTEXT_SIZE * 2 ? text.substring( 0, CONTEXT_SIZE * 2 ) + "..." : text;
        }

        int start = Math.max( 0, pos - CONTEXT_SIZE );
        int end = Math.min( text.length( ), pos + searchTerm.length( ) + CONTEXT_SIZE );

        String excerpt = text.substring( start, end );

        String prefix = start > 0 ? "..." : "";
        String suffix = end < text.length( ) ? "..." : "";

        String highlighted = excerpt.replaceFirst( "(?i)" + Pattern.quote( searchTerm ), "**" + searchTerm + "**" );

        return prefix + highlighted + suffix;
    }

    private String buildResponse( String searchTerm, String spaceCode, List<GrepMatch> matches )
    {
        clearSources( );

        Set<String> uniqueCodes = new HashSet<>( );
        for ( GrepMatch match : matches )
        {
            if ( !uniqueCodes.contains( match.item.getCode( ) ) )
            {
                uniqueCodes.add( match.item.getCode( ) );
                addSource( match.item, match.title );
            }
        }

        StringBuilder sb = new StringBuilder( );
        sb.append( "# Grep Results for '" ).append( searchTerm ).append( "'\n\n" );
        sb.append( "Found " ).append( matches.size( ) ).append( " match(es) in " ).append( uniqueCodes.size( ) ).append( " item(s)." );

        if ( spaceCode != null && !spaceCode.trim( ).isEmpty( ) )
        {
            sb.append( " (space: " ).append( spaceCode ).append( ")" );
        }
        sb.append( "\n" );
        sb.append( "Use readContent(code) to read the full item content.\n\n" );

        for ( GrepMatch match : matches )
        {
            sb.append( match.item.getCode( ) ).append( ":" ).append( match.chunkIndex ).append( ": " ).append( match.excerpt ).append( "\n" );
        }

        return sb.toString( );
    }

    private static class GrepMatch
    {
        final AbstractWikiItem item;
        final String title;
        final String chunkIndex;
        final String excerpt;

        GrepMatch( AbstractWikiItem item, String title, String chunkIndex, String excerpt )
        {
            this.item = item;
            this.title = title;
            this.chunkIndex = chunkIndex;
            this.excerpt = excerpt;
        }
    }
}
