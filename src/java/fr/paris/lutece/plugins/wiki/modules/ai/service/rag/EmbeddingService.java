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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.CountResponse;
import co.elastic.clients.elasticsearch.core.DeleteByQueryResponse;
import dev.langchain4j.data.document.DefaultDocument;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import fr.paris.lutece.plugins.wiki.business.item.AbstractWikiItem;
import fr.paris.lutece.plugins.wiki.business.item.WikiItemType;
import fr.paris.lutece.plugins.wiki.business.item.impl.Book;
import fr.paris.lutece.plugins.wiki.business.item.impl.Chapter;
import fr.paris.lutece.plugins.wiki.business.item.impl.Page;
import fr.paris.lutece.plugins.wiki.business.item.impl.Space;
import fr.paris.lutece.plugins.wiki.business.revision.Revision;
import fr.paris.lutece.plugins.wiki.business.revision.RevisionHome;
import fr.paris.lutece.plugins.wiki.modules.ai.service.rag.util.IndexingStatus;
import fr.paris.lutece.plugins.wiki.service.WikiItemService;
import fr.paris.lutece.plugins.wiki.service.WikiUrlService;
import fr.paris.lutece.portal.service.util.AppLogService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@ApplicationScoped
public class EmbeddingService
{
    private static final int CHUNK_SIZE = 1000;
    private static final int CHUNK_OVERLAP = 200;

    private static final String FIELD_ID = "id";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_CODE = "code";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_URL = "url";
    private static final String FIELD_BOOK_ID = "book_id";
    private static final String FIELD_BOOK_CODE = "book_code";
    private static final String FIELD_BOOK_NAME = "book_name";
    private static final String FIELD_CHAPTER_ID = "chapter_id";
    private static final String FIELD_CHAPTER_CODE = "chapter_code";
    private static final String FIELD_CHAPTER_NAME = "chapter_name";
    private static final String FIELD_PAGE_ID = "page_id";
    private static final String FIELD_SPACE_ID = "space_id";
    private static final String FIELD_SPACE_CODE = "space_code";
    private static final String FIELD_SPACE_NAME = "space_name";
    private static final String FIELD_DATE_MODIFIED = "date_modified";
    private static final String FIELD_CHUNK_INDEX = "chunk_index";

    private static final String LOG_INDEXED_BOOK = "Indexed book {} {} items";
    private static final String LOG_INDEXED_SPACE = "Indexed space {} ({} chunks)";
    private static final String LOG_REINDEX_COMPLETED = "Reindexing completed successfully";
    private static final String LOG_REMOVED_BOOK = "Removed book {} {} items";
    private static final String LOG_REMOVED_PAGE = "Removed page {} {} items";
    private static final String LOG_REMOVED_SPACE = "Removed space {} {} items";
    private static final String LOG_INDEX_CLEARED = "Elasticsearch index cleared successfully";
    private static final String LOG_CLEARING_INDEX = "Clearing Elasticsearch index...";
    private static final String LOG_INDEX_CLEARED_SUCCESS = "Index cleared successfully";
    private static final String LOG_STARTING_REINDEX = "Starting full reindexation - clearing existing index...";
    private static final String LOG_RECREATING_INDEX = "Recreating Elasticsearch index...";
    private static final String LOG_INDEX_RECREATED = "Index recreated successfully";
    private static final String LOG_FOUND_BOOKS = "Found ";
    private static final String LOG_BOOKS_TO_INDEX = " books to index";
    private static final String LOG_INDEXING_BOOK = "Indexing book: ";
    private static final String LOG_FULL_INDEXATION_COMPLETED = "Full indexation completed successfully";

    private static final String ERROR_INDEXING_BOOK = "Error indexing book {}";
    private static final String ERROR_INDEXING_SPACE = "Error indexing space {}";
    private static final String ERROR_REMOVING_BOOK = "Error removing book {}";
    private static final String ERROR_REMOVING_PAGE = "Error removing page {}";
    private static final String ERROR_REMOVING_SPACE = "Error removing space {}";
    private static final String ERROR_CLEARING_INDEX = "Error clearing Elasticsearch index";
    private static final String ERROR_DURING_REINDEXING = "Error during reindexing";
    private static final String ERROR_CLEARING_INDEX_MSG = "Error clearing index: ";
    private static final String ERROR_DURING_REINDEXING_MSG = "Error during reindexing: ";
    private static final String ERROR_GETTING_STATS = "Error getting index statistics";

    private static final String STATS_KEY_INDEX_NAME = "indexName";
    private static final String STATS_KEY_DOCUMENT_COUNT = "documentCount";
    private static final String STATS_KEY_INDEX_EXISTS = "indexExists";
    private static final String STATS_KEY_ERROR = "error";

    private static final String NEWLINE = "\n";
    private static final String ID_SEPARATOR = "_";
    private static final String SPACE = " ";
    private static final String EMPTY_STRING = "";
    private static final String REGEX_WHITESPACE = "\\s+";

    @Inject
    @Named( "wiki-ai.embeddingModel" )
    private EmbeddingModel _embeddingModel;

    @Inject
    private ElasticsearchService _elasticsearchService;

    private final IndexingStatus _indexingStatus = new IndexingStatus( );

    /**
     * Gets the current indexing status
     *
     * @return The indexing status
     */
    public IndexingStatus getIndexingStatus( )
    {
        return _indexingStatus;
    }

    /**
     * Finds the parent space of a wiki item by traversing up the hierarchy
     *
     * @param item
     *            The wiki item
     * @return The parent space or null if not found
     */
    private Space findParentSpace( AbstractWikiItem item )
    {
        AbstractWikiItem current = item;
        while ( current != null )
        {
            if ( current instanceof Space )
            {
                return (Space) current;
            }
            current = current.getParent( );
        }
        return null;
    }

    /**
     * Indexes a book and all its chapters and pages
     *
     * @param book
     *            The book to index
     */
    public void indexBook( Book book )
    {
        try
        {
            int totalItems = 0;

            indexBookItem( book );
            totalItems++;

            List<AbstractWikiItem> children = WikiItemService.getItemsByParent( book.getId( ) );
            for ( AbstractWikiItem child : children )
            {
                if ( child instanceof Chapter )
                {
                    List<AbstractWikiItem> pages = WikiItemService.getItemsByParent( child.getId( ) );
                    for ( AbstractWikiItem page : pages )
                    {
                        if ( page instanceof Page )
                        {
                            indexPageItem( (Page) page );
                            totalItems++;
                        }
                    }
                }
                else if ( child instanceof Page )
                {
                    indexPageItem( (Page) child );
                    totalItems++;
                }
            }

            AppLogService.info( LOG_INDEXED_BOOK, book.getCode( ), totalItems );
        }
        catch( Exception e )
        {
            AppLogService.error( ERROR_INDEXING_BOOK, book.getId( ), e );
        }
    }

    /**
     * Indexes a single item (Book or Page)
     *
     * @param itemId
     *            The item ID
     */
    public void indexItem( int itemId )
    {
        AbstractWikiItem item = WikiItemService.findById( itemId );

        if ( item == null )
        {
            return;
        }

        if ( item instanceof Space )
        {
            indexSpaceItem( (Space) item );
        }
        else if ( item instanceof Book )
        {
            indexBookItem( (Book) item );
        }
        else if ( item instanceof Page )
        {
            indexPageItem( (Page) item );
        }
    }

    /**
     * Indexes a single book item
     *
     * @param book
     *            The book
     */
    private void indexBookItem( Book book )
    {
        try
        {
            Revision revision = RevisionHome.getCurrentRevision( book.getId( ) );
            if ( revision == null )
            {
                return;
            }

            EmbeddingStore<TextSegment> embeddingStore = _elasticsearchService.createEmbeddingStore( );

            String cleanedDescription = cleanMarkdownContent( revision.getDescription( ) );
            String cleanedContent = cleanMarkdownContent( revision.getContent( ) );
            String fullContent = revision.getTitle( ) + SPACE + cleanedDescription + SPACE + cleanedContent;

            removeAllBookChunks( book.getId( ) );

            if ( !book.isPublished( ) )
            {
                return;
            }

            Map<String, String> baseMetadata = new HashMap<>( );
            baseMetadata.put( FIELD_TYPE, Book.RESOURCE_TYPE );
            baseMetadata.put( FIELD_CODE, book.getCode( ) );
            baseMetadata.put( FIELD_TITLE, revision.getTitle( ) );
            baseMetadata.put( FIELD_URL, WikiUrlService.buildViewUrl( book ) );
            baseMetadata.put( FIELD_BOOK_ID, String.valueOf( book.getId( ) ) );
            baseMetadata.put( FIELD_BOOK_CODE, book.getCode( ) );
            baseMetadata.put( FIELD_BOOK_NAME, revision.getTitle( ) );

            Space parentSpace = findParentSpace( book );
            if ( parentSpace != null )
            {
                baseMetadata.put( FIELD_SPACE_ID, String.valueOf( parentSpace.getId( ) ) );
                baseMetadata.put( FIELD_SPACE_CODE, parentSpace.getCode( ) );
                Revision spaceRevision = RevisionHome.getCurrentRevision( parentSpace.getId( ) );
                if ( spaceRevision != null && spaceRevision.getTitle( ) != null )
                {
                    baseMetadata.put( FIELD_SPACE_NAME, spaceRevision.getTitle( ) );
                }
            }

            if ( revision.getDateCreation( ) != null )
            {
                baseMetadata.put( FIELD_DATE_MODIFIED, revision.getDateCreation( ).toString( ) );
            }

            Document document = new DefaultDocument( fullContent, Metadata.from( baseMetadata ) );
            DocumentSplitter splitter = DocumentSplitters.recursive( CHUNK_SIZE, CHUNK_OVERLAP );
            List<TextSegment> segments = splitter.split( document );

            List<String> ids = new ArrayList<>( );
            List<TextSegment> segmentsWithMetadata = new ArrayList<>( );

            for ( int i = 0; i < segments.size( ); i++ )
            {
                TextSegment segment = segments.get( i );
                Map<String, String> metadata = new HashMap<>( baseMetadata );
                metadata.put( FIELD_ID, buildDocumentId( Book.RESOURCE_TYPE, book.getId( ) ) + "_" + i );
                metadata.put( FIELD_CHUNK_INDEX, String.valueOf( i ) );

                TextSegment segmentWithMeta = TextSegment.from( segment.text( ), Metadata.from( metadata ) );
                segmentsWithMetadata.add( segmentWithMeta );
                ids.add( buildDocumentId( Book.RESOURCE_TYPE, book.getId( ) ) + "_" + i );
            }

            if ( !segmentsWithMetadata.isEmpty( ) )
            {
                Response<List<Embedding>> embeddingsResponse = _embeddingModel.embedAll( segmentsWithMetadata );
                List<Embedding> embeddings = embeddingsResponse.content( );
                embeddingStore.addAll( ids, embeddings, segmentsWithMetadata );

                AppLogService.info( "Indexed book {} ({} chunks)", book.getCode( ), segments.size( ) );
            }
        }
        catch( Exception e )
        {
            AppLogService.error( "Error indexing book {}", book.getId( ), e );
        }
    }

    private void indexSpaceItem( Space space )
    {
        try
        {
            Revision revision = RevisionHome.getCurrentRevision( space.getId( ) );
            if ( revision == null )
            {
                return;
            }

            EmbeddingStore<TextSegment> embeddingStore = _elasticsearchService.createEmbeddingStore( );

            String cleanedDescription = cleanMarkdownContent( revision.getDescription( ) );
            String cleanedContent = cleanMarkdownContent( revision.getContent( ) );
            String fullContent = revision.getTitle( ) + SPACE + cleanedDescription + SPACE + cleanedContent;

            removeAllSpaceChunks( space.getId( ) );

            if ( !space.isPublished( ) )
            {
                return;
            }

            Map<String, String> baseMetadata = new HashMap<>( );
            baseMetadata.put( FIELD_TYPE, Space.RESOURCE_TYPE );
            baseMetadata.put( FIELD_CODE, space.getCode( ) );
            baseMetadata.put( FIELD_TITLE, revision.getTitle( ) );
            baseMetadata.put( FIELD_URL, WikiUrlService.buildViewUrl( space ) );
            baseMetadata.put( FIELD_SPACE_ID, String.valueOf( space.getId( ) ) );
            baseMetadata.put( FIELD_SPACE_CODE, space.getCode( ) );
            baseMetadata.put( FIELD_SPACE_NAME, revision.getTitle( ) );

            if ( revision.getDateCreation( ) != null )
            {
                baseMetadata.put( FIELD_DATE_MODIFIED, revision.getDateCreation( ).toString( ) );
            }

            Document document = new DefaultDocument( fullContent, Metadata.from( baseMetadata ) );
            DocumentSplitter splitter = DocumentSplitters.recursive( CHUNK_SIZE, CHUNK_OVERLAP );
            List<TextSegment> segments = splitter.split( document );

            List<String> ids = new ArrayList<>( );
            List<TextSegment> segmentsWithMetadata = new ArrayList<>( );

            for ( int i = 0; i < segments.size( ); i++ )
            {
                TextSegment segment = segments.get( i );
                Map<String, String> metadata = new HashMap<>( baseMetadata );
                metadata.put( FIELD_ID, buildDocumentId( Space.RESOURCE_TYPE, space.getId( ) ) + "_" + i );
                metadata.put( FIELD_CHUNK_INDEX, String.valueOf( i ) );

                TextSegment segmentWithMeta = TextSegment.from( segment.text( ), Metadata.from( metadata ) );
                segmentsWithMetadata.add( segmentWithMeta );
                ids.add( buildDocumentId( Space.RESOURCE_TYPE, space.getId( ) ) + "_" + i );
            }

            if ( !segmentsWithMetadata.isEmpty( ) )
            {
                Response<List<Embedding>> embeddingsResponse = _embeddingModel.embedAll( segmentsWithMetadata );
                List<Embedding> embeddings = embeddingsResponse.content( );
                embeddingStore.addAll( ids, embeddings, segmentsWithMetadata );

                AppLogService.info( LOG_INDEXED_SPACE, space.getCode( ), segments.size( ) );
            }
        }
        catch( Exception e )
        {
            AppLogService.error( ERROR_INDEXING_SPACE, space.getId( ), e );
        }
    }

    /**
     * Indexes a single page item
     *
     * @param page
     *            The page
     */
    private void indexPageItem( Page page )
    {
        try
        {
            Revision revision = RevisionHome.getCurrentRevision( page.getId( ) );
            if ( revision == null )
            {
                return;
            }

            EmbeddingStore<TextSegment> embeddingStore = _elasticsearchService.createEmbeddingStore( );

            String cleanedDescription = cleanMarkdownContent( revision.getDescription( ) );
            String cleanedContent = cleanMarkdownContent( revision.getContent( ) );
            String fullContent = revision.getTitle( ) + SPACE + cleanedDescription + SPACE + cleanedContent;

            removeAllPageChunks( page.getId( ) );

            if ( !page.isPublished( ) || !isHierarchyPublished( page ) )
            {
                return;
            }

            Map<String, String> baseMetadata = new HashMap<>( );
            baseMetadata.put( FIELD_TYPE, Page.RESOURCE_TYPE );
            baseMetadata.put( FIELD_CODE, page.getCode( ) );
            baseMetadata.put( FIELD_TITLE, revision.getTitle( ) );
            baseMetadata.put( FIELD_URL, WikiUrlService.buildViewUrl( page ) );
            baseMetadata.put( FIELD_PAGE_ID, String.valueOf( page.getId( ) ) );

            addHierarchyMetadata( baseMetadata, page );

            if ( revision.getDateCreation( ) != null )
            {
                baseMetadata.put( FIELD_DATE_MODIFIED, revision.getDateCreation( ).toString( ) );
            }

            Document document = new DefaultDocument( fullContent, Metadata.from( baseMetadata ) );
            DocumentSplitter splitter = DocumentSplitters.recursive( CHUNK_SIZE, CHUNK_OVERLAP );
            List<TextSegment> segments = splitter.split( document );

            List<String> ids = new ArrayList<>( );
            List<TextSegment> segmentsWithMetadata = new ArrayList<>( );

            for ( int i = 0; i < segments.size( ); i++ )
            {
                TextSegment segment = segments.get( i );
                Map<String, String> metadata = new HashMap<>( baseMetadata );
                metadata.put( FIELD_ID, buildDocumentId( Page.RESOURCE_TYPE, page.getId( ) ) + "_" + i );
                metadata.put( FIELD_CHUNK_INDEX, String.valueOf( i ) );

                TextSegment segmentWithMeta = TextSegment.from( segment.text( ), Metadata.from( metadata ) );
                segmentsWithMetadata.add( segmentWithMeta );
                ids.add( buildDocumentId( Page.RESOURCE_TYPE, page.getId( ) ) + "_" + i );
            }

            if ( !segmentsWithMetadata.isEmpty( ) )
            {
                Response<List<Embedding>> embeddingsResponse = _embeddingModel.embedAll( segmentsWithMetadata );
                List<Embedding> embeddings = embeddingsResponse.content( );
                embeddingStore.addAll( ids, embeddings, segmentsWithMetadata );

                AppLogService.info( "Indexed page {} ({} chunks)", page.getCode( ), segments.size( ) );
            }
        }
        catch( Exception e )
        {
            AppLogService.error( "Error indexing page {}", page.getId( ), e );
        }
    }

    /**
     * Checks if the entire hierarchy of an item is published
     *
     * @param item
     *            the wiki item
     * @return true if all parents are published
     */
    private boolean isHierarchyPublished( AbstractWikiItem item )
    {
        AbstractWikiItem current = item.getParent( );

        while ( current != null )
        {
            if ( !current.isPublished( ) )
            {
                return false;
            }
            current = current.getParent( );
        }

        return true;
    }

    /**
     * Adds hierarchy metadata (book, chapter, space) based on the page's position in hierarchy
     *
     * @param metadata
     *            the metadata map to populate
     * @param page
     *            the page item
     */
    private void addHierarchyMetadata( Map<String, String> metadata, Page page )
    {
        AbstractWikiItem current = page.getParent( );

        while ( current != null )
        {
            Revision currentRevision = RevisionHome.getCurrentRevision( current.getId( ) );
            String title = currentRevision != null ? currentRevision.getTitle( ) : current.getCode( );

            if ( current instanceof Chapter )
            {
                metadata.put( FIELD_CHAPTER_ID, String.valueOf( current.getId( ) ) );
                metadata.put( FIELD_CHAPTER_CODE, current.getCode( ) );
                metadata.put( FIELD_CHAPTER_NAME, title );
            }
            else if ( current instanceof Book )
            {
                metadata.put( FIELD_BOOK_ID, String.valueOf( current.getId( ) ) );
                metadata.put( FIELD_BOOK_CODE, current.getCode( ) );
                metadata.put( FIELD_BOOK_NAME, title );
            }
            else if ( current instanceof Space )
            {
                metadata.put( FIELD_SPACE_ID, String.valueOf( current.getId( ) ) );
                metadata.put( FIELD_SPACE_CODE, current.getCode( ) );
                metadata.put( FIELD_SPACE_NAME, title );
            }

            current = current.getParent( );
        }
    }

    /**
     * Removes a book and all its content from the index using Elasticsearch query
     *
     * @param bookId
     *            The ID of the book to remove
     */
    public void removeBook( int bookId )
    {
        try
        {
            String indexName = _elasticsearchService.getIndexName( );

            DeleteByQueryResponse response = _elasticsearchService.getClient( ).deleteByQuery(
                    delete -> delete.index( indexName ).query( q -> q.term( t -> t.field( "metadata.book_id.keyword" ).value( String.valueOf( bookId ) ) ) ) );

            long deletedCount = response.deleted( );
            AppLogService.info( LOG_REMOVED_BOOK, bookId, deletedCount );
        }
        catch( ElasticsearchException | IOException e )
        {
            AppLogService.error( ERROR_REMOVING_BOOK, bookId, e );
        }
    }

    /**
     * Removes a page from the index using Elasticsearch query
     *
     * @param pageId
     *            The ID of the page to remove
     */
    public void removePage( int pageId )
    {
        try
        {
            String indexName = _elasticsearchService.getIndexName( );

            DeleteByQueryResponse response = _elasticsearchService.getClient( ).deleteByQuery(
                    delete -> delete.index( indexName ).query( q -> q.term( t -> t.field( "metadata.page_id.keyword" ).value( String.valueOf( pageId ) ) ) ) );

            AppLogService.info( LOG_REMOVED_PAGE, pageId, response.deleted( ) );
        }
        catch( ElasticsearchException | IOException e )
        {
            AppLogService.error( ERROR_REMOVING_PAGE, pageId, e );
        }
    }

    public void removeSpace( int spaceId )
    {
        try
        {
            String indexName = _elasticsearchService.getIndexName( );

            DeleteByQueryResponse response = _elasticsearchService.getClient( ).deleteByQuery( delete -> delete.index( indexName )
                    .query( q -> q.term( t -> t.field( "metadata.space_id.keyword" ).value( String.valueOf( spaceId ) ) ) ) );

            long deletedCount = response.deleted( );
            AppLogService.info( LOG_REMOVED_SPACE, spaceId, deletedCount );
        }
        catch( ElasticsearchException | IOException e )
        {
            AppLogService.error( ERROR_REMOVING_SPACE, spaceId, e );
        }
    }

    /**
     * Removes all chunks for a book from the index
     *
     * @param bookId
     *            The book ID
     */
    private void removeAllBookChunks( int bookId )
    {
        try
        {
            String indexName = _elasticsearchService.getIndexName( );

            DeleteByQueryResponse response = _elasticsearchService.getClient( )
                    .deleteByQuery( delete -> delete.index( indexName )
                            .query( q -> q.bool( b -> b.must( m -> m.term( t -> t.field( "metadata.type.keyword" ).value( Book.RESOURCE_TYPE ) ) )
                                    .must( m -> m.term( t -> t.field( "metadata.book_id.keyword" ).value( String.valueOf( bookId ) ) ) ) ) ) );

            AppLogService.debug( "Removed {} chunks for book {}", response.deleted( ), bookId );
        }
        catch( ElasticsearchException | IOException e )
        {
            AppLogService.error( "Error removing book chunks {}", bookId, e );
        }
    }

    /**
     * Removes all chunks for a page from the index
     *
     * @param pageId
     *            The page ID
     */
    private void removeAllPageChunks( int pageId )
    {
        try
        {
            String indexName = _elasticsearchService.getIndexName( );

            DeleteByQueryResponse response = _elasticsearchService.getClient( )
                    .deleteByQuery( delete -> delete.index( indexName )
                            .query( q -> q.bool( b -> b.must( m -> m.term( t -> t.field( "metadata.type.keyword" ).value( Page.RESOURCE_TYPE ) ) )
                                    .must( m -> m.term( t -> t.field( "metadata.page_id.keyword" ).value( String.valueOf( pageId ) ) ) ) ) ) );

            AppLogService.debug( "Removed {} chunks for page {}", response.deleted( ), pageId );
        }
        catch( ElasticsearchException | IOException e )
        {
            AppLogService.error( "Error removing page chunks {}", pageId, e );
        }
    }

    private void removeAllSpaceChunks( int spaceId )
    {
        try
        {
            String indexName = _elasticsearchService.getIndexName( );

            DeleteByQueryResponse response = _elasticsearchService.getClient( )
                    .deleteByQuery( delete -> delete.index( indexName )
                            .query( q -> q.bool( b -> b.must( m -> m.term( t -> t.field( "metadata.type.keyword" ).value( Space.RESOURCE_TYPE ) ) )
                                    .must( m -> m.term( t -> t.field( "metadata.space_id.keyword" ).value( String.valueOf( spaceId ) ) ) ) ) ) );

            AppLogService.debug( "Removed {} chunks for space {}", response.deleted( ), spaceId );
        }
        catch( ElasticsearchException | IOException e )
        {
            AppLogService.error( "Error removing space chunks {}", spaceId, e );
        }
    }

    /**
     * Clears the entire Elasticsearch index
     */
    public void clearIndex( )
    {
        try
        {
            EmbeddingStore<TextSegment> embeddingStore = _elasticsearchService.createEmbeddingStore( );
            embeddingStore.removeAll( );
            AppLogService.info( LOG_INDEX_CLEARED );
        }
        catch( Exception e )
        {
            AppLogService.error( ERROR_CLEARING_INDEX, e );
        }
    }

    /**
     * Clears the index asynchronously
     */
    public void clearIndexAsync( )
    {
        if ( _indexingStatus.getIsRunning( ).compareAndSet( false, true ) )
        {
            new Thread( )
            {
                @Override
                public void run( )
                {
                    try
                    {
                        _indexingStatus.reset( );
                        _indexingStatus.getSbLogs( ).append( LOG_CLEARING_INDEX ).append( NEWLINE );
                        AppLogService.info( LOG_CLEARING_INDEX );

                        clearIndex( );

                        _indexingStatus.getSbLogs( ).append( LOG_INDEX_CLEARED_SUCCESS ).append( NEWLINE );
                        AppLogService.info( LOG_INDEX_CLEARED_SUCCESS );
                    }
                    catch( Exception e )
                    {
                        String errorMsg = ERROR_CLEARING_INDEX_MSG + e.getMessage( );
                        _indexingStatus.getSbLogs( ).append( errorMsg ).append( NEWLINE );
                        AppLogService.error( "Error clearing index: {}", e.getMessage( ), e );
                    }
                    finally
                    {
                        _indexingStatus.getIsRunning( ).set( false );
                    }
                }
            }.start( );
        }
    }

    /**
     * Reindexes all wiki content asynchronously
     */
    public void reindexAll( )
    {
        if ( _indexingStatus.getIsRunning( ).compareAndSet( false, true ) )
        {
            new Thread( )
            {
                @Override
                public void run( )
                {
                    processReindexAll( );
                }
            }.start( );
        }
    }

    /**
     * Retrieves statistics about the current index
     *
     * @return A map containing index statistics
     */
    public Map<String, Object> getIndexStatistics( )
    {
        Map<String, Object> stats = new HashMap<>( );

        try
        {
            String indexName = _elasticsearchService.getIndexName( );

            CountResponse countResponse = _elasticsearchService.getClient( ).count( c -> c.index( indexName ) );
            long documentCount = countResponse.count( );

            stats.put( STATS_KEY_INDEX_NAME, indexName );
            stats.put( STATS_KEY_DOCUMENT_COUNT, documentCount );
            stats.put( STATS_KEY_INDEX_EXISTS, true );
        }
        catch( ElasticsearchException | IOException e )
        {
            AppLogService.error( ERROR_GETTING_STATS, e );
            stats.put( STATS_KEY_INDEX_EXISTS, false );
            stats.put( STATS_KEY_DOCUMENT_COUNT, 0L );
            stats.put( STATS_KEY_ERROR, e.getMessage( ) );
        }

        return stats;
    }

    /**
     * Processes the full reindexation of all content
     */
    private void processReindexAll( )
    {
        try
        {
            _indexingStatus.reset( );
            _indexingStatus.getSbLogs( ).append( LOG_STARTING_REINDEX ).append( NEWLINE );
            AppLogService.info( LOG_STARTING_REINDEX );

            clearIndex( );
            _indexingStatus.getSbLogs( ).append( LOG_INDEX_CLEARED_SUCCESS ).append( NEWLINE );

            _indexingStatus.getSbLogs( ).append( LOG_RECREATING_INDEX ).append( NEWLINE );
            AppLogService.info( LOG_RECREATING_INDEX );

            _elasticsearchService.createIndexIfNotExists( );
            _indexingStatus.getSbLogs( ).append( LOG_INDEX_RECREATED ).append( NEWLINE );

            List<AbstractWikiItem> spaces = WikiItemService.getPublishedItemsByType( WikiItemType.SPACE );
            List<AbstractWikiItem> books = WikiItemService.getPublishedItemsByType( WikiItemType.BOOK );
            List<AbstractWikiItem> pages = WikiItemService.getPublishedItemsByType( WikiItemType.PAGE );
            int totalItems = spaces.size( ) + books.size( ) + pages.size( );

            _indexingStatus.setNbTotalObj( totalItems );
            _indexingStatus.getSbLogs( ).append( "Found " ).append( spaces.size( ) ).append( " spaces to index" ).append( NEWLINE );
            _indexingStatus.getSbLogs( ).append( LOG_FOUND_BOOKS ).append( books.size( ) ).append( LOG_BOOKS_TO_INDEX ).append( NEWLINE );
            _indexingStatus.getSbLogs( ).append( "Found " ).append( pages.size( ) ).append( " pages to index" ).append( NEWLINE );

            int currentItem = 0;

            for ( AbstractWikiItem space : spaces )
            {
                if ( space instanceof Space )
                {
                    _indexingStatus.getSbLogs( ).append( "Indexing space: " ).append( space.getCode( ) ).append( NEWLINE );

                    indexSpaceItem( (Space) space );
                    currentItem++;
                    _indexingStatus.setCurrentNbIndexedObj( currentItem );
                }
            }

            for ( AbstractWikiItem book : books )
            {
                if ( book instanceof Book )
                {
                    _indexingStatus.getSbLogs( ).append( LOG_INDEXING_BOOK ).append( book.getCode( ) ).append( NEWLINE );

                    indexBookItem( (Book) book );
                    currentItem++;
                    _indexingStatus.setCurrentNbIndexedObj( currentItem );
                }
            }

            for ( AbstractWikiItem page : pages )
            {
                if ( page instanceof Page )
                {
                    indexPageItem( (Page) page );
                    currentItem++;
                    _indexingStatus.setCurrentNbIndexedObj( currentItem );
                }
            }

            _indexingStatus.getSbLogs( ).append( LOG_FULL_INDEXATION_COMPLETED ).append( NEWLINE );
            AppLogService.info( LOG_REINDEX_COMPLETED );
        }
        catch( Exception e )
        {
            _indexingStatus.getSbLogs( ).append( ERROR_DURING_REINDEXING_MSG ).append( e.getMessage( ) ).append( NEWLINE );
            AppLogService.error( ERROR_DURING_REINDEXING, e );
        }
        finally
        {
            _indexingStatus.getIsRunning( ).set( false );
        }
    }

    /**
     * Builds a document ID from resource type and item ID
     *
     * @param resourceType
     *            The type of resource
     * @param itemId
     *            The item ID
     * @return The formatted document ID
     */
    private String buildDocumentId( String resourceType, int itemId )
    {
        return resourceType + ID_SEPARATOR + itemId;
    }

    /**
     * Cleans markdown content by removing base64 images and normalizing whitespace
     *
     * @param content
     *            The content to clean
     * @return The cleaned content
     */
    private String cleanMarkdownContent( String content )
    {
        if ( content == null )
        {
            return EMPTY_STRING;
        }

        Pattern patternBase64Image = Pattern.compile( "!\\[.*?\\]\\(data:image/[^;]+;base64,[^)]+\\)", Pattern.DOTALL );
        Pattern patternImageTag = Pattern.compile( "<img[^>]*src=[\"']data:image/[^;]+;base64,[^\"']+[\"'][^>]*>", Pattern.DOTALL );

        String cleaned = patternBase64Image.matcher( content ).replaceAll( EMPTY_STRING );
        cleaned = patternImageTag.matcher( cleaned ).replaceAll( EMPTY_STRING );
        cleaned = cleaned.replaceAll( REGEX_WHITESPACE, SPACE ).trim( );

        return cleaned;
    }
}
