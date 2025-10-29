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
import fr.paris.lutece.plugins.wiki.business.item.AbstractWikiItem;
import fr.paris.lutece.plugins.wiki.business.item.WikiItemType;
import fr.paris.lutece.plugins.wiki.business.revision.Revision;
import fr.paris.lutece.plugins.wiki.business.revision.RevisionHome;
import fr.paris.lutece.plugins.wiki.service.WikiItemService;
import fr.paris.lutece.plugins.wiki.service.security.WikiAccessControlService;
import fr.paris.lutece.portal.service.security.LuteceUser;

/**
 * Tool for browsing wiki book structure including chapters and pages.
 */
public class WikiBrowseBookTool extends AbstractWikiTool
{
    private static final String ERROR_BOOK_CODE_REQUIRED = "Error: book code is required";
    private static final String MSG_BOOK_NOT_FOUND = "No book found with code: ";
    private static final String MSG_ACCESS_DENIED = "Access denied to book: ";
    private static final String MSG_NO_CHAPTERS = "This book has no chapters yet.";
    private static final String MSG_NO_PAGES = "- No pages in this chapter\n";
    private static final String MSG_BOOK_HEADER = "# Book: ";
    private static final String MSG_CHAPTERS_HEADER = "## Chapters\n\n";
    private static final String MSG_READ_CONTENT_HINT = "Use `readContent` with a page code to read the full content of a page.\n\n";

    private static final String TOOL_DESCRIPTION = "Browses a wiki book to see its chapters and pages. "
            + "Use this after browseSpace to explore a specific book. " + "Returns the book structure with chapters and their pages.";
    private static final String PARAM_BOOK_CODE_DESCRIPTION = "The code of the book to browse";

    /**
     * Constructor.
     *
     * @param user
     *            the current Lutece user for access control
     */
    public WikiBrowseBookTool( LuteceUser user )
    {
        super( user );
    }

    /**
     * Browses a wiki book to see its chapters and pages.
     *
     * @param bookCode
     *            the code of the book to browse
     * @return the formatted book structure with chapters and their pages
     */
    @Tool( TOOL_DESCRIPTION )
    public String browseBook( @P( PARAM_BOOK_CODE_DESCRIPTION ) String bookCode )
    {
        clearSources( );

        if ( bookCode == null || bookCode.trim( ).isEmpty( ) )
        {
            return ERROR_BOOK_CODE_REQUIRED;
        }

        AbstractWikiItem book = WikiItemService.findByCode( bookCode.trim( ) );
        if ( book == null || book.getType( ) != WikiItemType.BOOK )
        {
            return MSG_BOOK_NOT_FOUND + bookCode;
        }

        if ( !WikiAccessControlService.canView( _user, book ) )
        {
            return MSG_ACCESS_DENIED + bookCode;
        }

        Revision bookRevision = RevisionHome.getCurrentRevision( book.getId( ) );
        String bookTitle = extractTitle( bookRevision, bookCode );

        addSource( book, bookTitle );

        StringBuilder sb = new StringBuilder( );
        sb.append( MSG_BOOK_HEADER ).append( bookTitle ).append( "\n\n" );

        appendDescription( sb, bookRevision );

        List<AbstractWikiItem> children = WikiItemService.getItemsByParent( book.getId( ) );

        if ( children.isEmpty( ) )
        {
            sb.append( MSG_NO_CHAPTERS );
            return sb.toString( );
        }

        sb.append( MSG_CHAPTERS_HEADER );
        appendChaptersRecursive( sb, book.getId( ), 3 );

        sb.append( MSG_READ_CONTENT_HINT );
        sb.append( MSG_CITATION_INSTRUCTION );
        return sb.toString( );
    }

    /**
     * Extracts the title from a revision, falling back to default if not available.
     *
     * @param revision
     *            the revision to extract title from
     * @param defaultTitle
     *            the default title if revision title is not available
     * @return the extracted title or default
     */
    private String extractTitle( Revision revision, String defaultTitle )
    {
        return ( revision != null && revision.getTitle( ) != null ) ? revision.getTitle( ) : defaultTitle;
    }

    /**
     * Appends the description from a revision to the StringBuilder if available.
     *
     * @param sb
     *            the StringBuilder to append to
     * @param revision
     *            the revision containing the description
     */
    private void appendDescription( StringBuilder sb, Revision revision )
    {
        if ( revision != null && revision.getDescription( ) != null && !revision.getDescription( ).isEmpty( ) )
        {
            sb.append( revision.getDescription( ) ).append( "\n\n" );
        }
    }

    /**
     * Recursively appends chapters and their children (sub-chapters and pages) to the StringBuilder.
     *
     * @param sb
     *            the StringBuilder to append to
     * @param parentId
     *            the parent item ID
     * @param depth
     *            the current heading depth (3 = ###, 4 = ####, etc.)
     */
    private void appendChaptersRecursive( StringBuilder sb, int parentId, int depth )
    {
        List<AbstractWikiItem> children = WikiItemService.getItemsByParent( parentId );

        for ( AbstractWikiItem child : children )
        {
            if ( !WikiAccessControlService.canView( _user, child ) )
            {
                continue;
            }

            if ( child.getType( ) == WikiItemType.CHAPTER )
            {
                Revision chapterRevision = RevisionHome.getCurrentRevision( child.getId( ) );
                String chapterTitle = extractTitle( chapterRevision, child.getCode( ) );

                sb.append( "#".repeat( Math.min( depth, 6 ) ) ).append( " " ).append( chapterTitle ).append( "\n" );
                appendChaptersRecursive( sb, child.getId( ), depth + 1 );
                sb.append( "\n" );
            }
            else if ( child.getType( ) == WikiItemType.PAGE )
            {
                Revision pageRevision = RevisionHome.getCurrentRevision( child.getId( ) );
                String pageTitle = extractTitle( pageRevision, child.getCode( ) );
                String pageDesc = ( pageRevision != null && pageRevision.getDescription( ) != null ) ? pageRevision.getDescription( ) : "";

                addSource( child, pageTitle );

                sb.append( "- **" ).append( pageTitle ).append( "** (`" ).append( child.getCode( ) ).append( "`)" );
                if ( !pageDesc.isEmpty( ) )
                {
                    sb.append( ": " ).append( pageDesc );
                }
                sb.append( "\n" );
            }
        }
    }

}
