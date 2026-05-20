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
 * Tool for browsing wiki space structure including categories and books.
 */
public class WikiBrowseSpaceTool extends AbstractWikiTool
{
    private static final String ERROR_SPACE_CODE_REQUIRED = "Error: space code is required";
    private static final String MSG_SPACE_NOT_FOUND = "No space found with code: ";
    private static final String MSG_ACCESS_DENIED = "Access denied to space: ";
    private static final String MSG_NO_CATEGORIES = "This space has no categories yet.";
    private static final String MSG_NO_BOOKS = "- No books in this category\n";
    private static final String MSG_SPACE_HEADER = "# Space: ";
    private static final String MSG_CATEGORIES_HEADER = "## Categories\n\n";
    private static final String MSG_BROWSE_BOOK_HINT = "Use `browseBook` with a book code to see its chapters and pages.\n\n";

    private static final String TOOL_DESCRIPTION = "Browses a wiki space to see its categories and books. "
            + "Use this after listSpaces to explore a specific area of the wiki. " + "Returns the space structure with categories and their books.";
    private static final String PARAM_SPACE_CODE_DESCRIPTION = "The code of the space to browse";

    /**
     * Constructor.
     *
     * @param user
     *            the current Lutece user for access control
     */
    public WikiBrowseSpaceTool( LuteceUser user )
    {
        super( user );
    }

    /**
     * Browses a wiki space to see its categories and books.
     *
     * @param spaceCode
     *            the code of the space to browse
     * @return the formatted space structure with categories and their books
     */
    @Tool( TOOL_DESCRIPTION )
    public String browseSpace( @P( PARAM_SPACE_CODE_DESCRIPTION ) String spaceCode )
    {
        clearSources( );

        if ( spaceCode == null || spaceCode.trim( ).isEmpty( ) )
        {
            return ERROR_SPACE_CODE_REQUIRED;
        }

        AbstractWikiItem space = WikiItemService.findByCode( spaceCode.trim( ) );
        if ( space == null || space.getType( ) != WikiItemType.SPACE )
        {
            return MSG_SPACE_NOT_FOUND + spaceCode;
        }

        if ( !WikiAccessControlService.canView( _user, space ) )
        {
            return MSG_ACCESS_DENIED + spaceCode;
        }

        Revision spaceRevision = RevisionHome.getCurrentRevision( space.getId( ) );
        String spaceTitle = extractTitle( spaceRevision, spaceCode );

        addSource( space, spaceTitle );

        StringBuilder sb = new StringBuilder( );
        sb.append( MSG_SPACE_HEADER ).append( spaceTitle ).append( "\n\n" );

        appendDescription( sb, spaceRevision );

        List<AbstractWikiItem> children = WikiItemService.getItemsByParent( space.getId( ) );

        if ( children.isEmpty( ) )
        {
            sb.append( MSG_NO_CATEGORIES );
            return sb.toString( );
        }

        sb.append( MSG_CATEGORIES_HEADER );
        appendChildrenRecursive( sb, space.getId( ), 3 );

        sb.append( MSG_BROWSE_BOOK_HINT );
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
     * Recursively appends categories, sub-categories and books to the StringBuilder.
     *
     * @param sb
     *            the StringBuilder to append to
     * @param parentId
     *            the parent item ID
     * @param depth
     *            the current heading depth (3 = ###, 4 = ####, etc.)
     */
    private void appendChildrenRecursive( StringBuilder sb, int parentId, int depth )
    {
        List<AbstractWikiItem> children = WikiItemService.getItemsByParent( parentId );

        for ( AbstractWikiItem child : children )
        {
            if ( !WikiAccessControlService.canView( _user, child ) )
            {
                continue;
            }

            if ( child.getType( ) == WikiItemType.CATEGORY )
            {
                Revision catRevision = RevisionHome.getCurrentRevision( child.getId( ) );
                String catTitle = extractTitle( catRevision, child.getCode( ) );

                sb.append( "#".repeat( Math.min( depth, 6 ) ) ).append( " " ).append( catTitle ).append( "\n" );
                appendChildrenRecursive( sb, child.getId( ), depth + 1 );
                sb.append( "\n" );
            }
            else if ( child.getType( ) == WikiItemType.BOOK )
            {
                Revision bookRevision = RevisionHome.getCurrentRevision( child.getId( ) );
                String bookTitle = extractTitle( bookRevision, child.getCode( ) );
                String bookDesc = ( bookRevision != null && bookRevision.getDescription( ) != null ) ? bookRevision.getDescription( ) : "";

                addSource( child, bookTitle );

                sb.append( "- **" ).append( bookTitle ).append( "** (`" ).append( child.getCode( ) ).append( "`)" );
                if ( !bookDesc.isEmpty( ) )
                {
                    sb.append( ": " ).append( bookDesc );
                }
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
