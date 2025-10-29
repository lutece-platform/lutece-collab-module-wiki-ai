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

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import fr.paris.lutece.plugins.wiki.business.item.AbstractWikiItem;
import fr.paris.lutece.plugins.wiki.business.revision.Revision;
import fr.paris.lutece.plugins.wiki.business.revision.RevisionHome;
import fr.paris.lutece.plugins.wiki.service.WikiItemService;
import fr.paris.lutece.plugins.wiki.service.security.WikiAccessControlService;
import fr.paris.lutece.portal.service.security.LuteceUser;

/**
 * Tool for reading the full content of a wiki item by its code.
 */
public class WikiReadContentTool extends AbstractWikiTool
{
    private static final String ERROR_CODE_REQUIRED = "Error: code parameter is required";
    private static final String MSG_ITEM_NOT_FOUND = "No wiki item found with code: ";
    private static final String MSG_ACCESS_DENIED = "Access denied to wiki item: ";
    private static final String MSG_NO_CONTENT = "No content available for wiki item: ";
    private static final String MSG_DESCRIPTION_LABEL = "**Description:** ";
    private static final String MSG_TYPE_LABEL = "**Type:** ";
    private static final String MSG_CODE_LABEL_BOLD = "**Code:** ";
    private static final String MSG_CONTENT_HEADER = "## Content\n\n";

    private static final String TOOL_DESCRIPTION = "Reads the full content of a wiki item (book, chapter, page, or space) by its unique code. "
            + "Use this tool when you need to access the complete content of a specific wiki item that you know the code of. "
            + "Returns the title, description and full content of the item.";
    private static final String PARAM_CODE_DESCRIPTION = "The unique code identifier of the wiki item (e.g., 'getting-started', 'user-guide')";

    /**
     * Constructor.
     *
     * @param user
     *            the current Lutece user for access control
     */
    public WikiReadContentTool( LuteceUser user )
    {
        super( user );
    }

    /**
     * Reads the full content of a wiki item by its unique code.
     *
     * @param code
     *            the unique code identifier of the wiki item
     * @return the formatted wiki item content including title, description and content
     */
    @Tool( TOOL_DESCRIPTION )
    public String readContent( @P( PARAM_CODE_DESCRIPTION ) String code )
    {
        clearSources( );

        if ( code == null || code.trim( ).isEmpty( ) )
        {
            return ERROR_CODE_REQUIRED;
        }

        AbstractWikiItem item = WikiItemService.findByCode( code.trim( ) );
        if ( item == null )
        {
            return MSG_ITEM_NOT_FOUND + code;
        }

        if ( !WikiAccessControlService.canView( _user, item ) )
        {
            return MSG_ACCESS_DENIED + code;
        }

        Revision revision = RevisionHome.getCurrentRevision( item.getId( ) );
        if ( revision == null )
        {
            return MSG_NO_CONTENT + code;
        }

        String title = revision.getTitle( ) != null ? revision.getTitle( ) : item.getCode( );

        addSource( item, title );

        return buildContentResponse( item, revision, title );
    }

    /**
     * Builds the formatted content response string.
     *
     * @param item
     *            the wiki item
     * @param revision
     *            the current revision
     * @param title
     *            the item title
     * @return the formatted content response
     */
    private String buildContentResponse( AbstractWikiItem item, Revision revision, String title )
    {
        StringBuilder sb = new StringBuilder( );
        sb.append( "# " ).append( title ).append( "\n\n" );

        if ( revision.getDescription( ) != null && !revision.getDescription( ).isEmpty( ) )
        {
            sb.append( MSG_DESCRIPTION_LABEL ).append( revision.getDescription( ) ).append( "\n\n" );
        }

        sb.append( MSG_TYPE_LABEL ).append( item.getType( ).getCode( ) ).append( "\n" );
        sb.append( MSG_CODE_LABEL_BOLD ).append( item.getCode( ) ).append( "\n\n" );

        if ( revision.getContent( ) != null && !revision.getContent( ).isEmpty( ) )
        {
            sb.append( MSG_CONTENT_HEADER );
            sb.append( revision.getContent( ) );
        }

        return sb.toString( );
    }
}
