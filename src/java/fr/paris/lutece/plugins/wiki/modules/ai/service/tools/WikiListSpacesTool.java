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

import dev.langchain4j.agent.tool.Tool;
import fr.paris.lutece.plugins.wiki.business.item.AbstractWikiItem;
import fr.paris.lutece.plugins.wiki.business.item.WikiItemHome;
import fr.paris.lutece.plugins.wiki.business.item.WikiItemType;
import fr.paris.lutece.plugins.wiki.business.revision.Revision;
import fr.paris.lutece.plugins.wiki.business.revision.RevisionHome;
import fr.paris.lutece.plugins.wiki.service.security.WikiAccessControlService;
import fr.paris.lutece.portal.service.security.LuteceUser;

/**
 * Tool for listing all available wiki spaces.
 */
public class WikiListSpacesTool extends AbstractWikiTool
{
    private static final String MSG_NO_SPACES = "No wiki spaces found.";
    private static final String MSG_NO_ACCESSIBLE_SPACES = "No wiki spaces accessible.";
    private static final String MSG_SPACES_HEADER = "# Available Wiki Spaces\n\n";
    private static final String MSG_CODE_LABEL_FORMATTED = "- **Code:** `";
    private static final String MSG_DESCRIPTION_LABEL = "- **Description:** ";
    private static final String MSG_BROWSE_SPACE_HINT = "Use `browseSpace` with a space code to explore its categories and books.\n\n";

    private static final String TOOL_DESCRIPTION = "Lists all available wiki spaces. Use this tool to discover the main topics/areas covered by the wiki. "
            + "Returns a list of spaces with their code, title and description.";

    /**
     * Constructor.
     *
     * @param user
     *            the current Lutece user for access control
     */
    public WikiListSpacesTool( LuteceUser user )
    {
        super( user );
    }

    /**
     * Lists all available wiki spaces.
     *
     * @return the formatted list of wiki spaces with their codes, titles and descriptions
     */
    @Tool( TOOL_DESCRIPTION )
    public String listSpaces( )
    {
        clearSources( );
        List<AbstractWikiItem> spaces = WikiItemHome.getWikiItemsByType( WikiItemType.SPACE );

        if ( spaces.isEmpty( ) )
        {
            return MSG_NO_SPACES;
        }

        StringBuilder sb = new StringBuilder( );
        sb.append( MSG_SPACES_HEADER );

        int count = 0;
        for ( AbstractWikiItem space : spaces )
        {
            if ( !WikiAccessControlService.canView( _user, space ) )
            {
                continue;
            }

            count++;
            Revision revision = RevisionHome.getCurrentRevision( space.getId( ) );
            String title = extractTitle( revision, space.getCode( ) );

            addSource( space, title );
            appendSpaceEntry( sb, count, space, revision, title );
        }

        if ( count == 0 )
        {
            return MSG_NO_ACCESSIBLE_SPACES;
        }

        sb.append( MSG_BROWSE_SPACE_HINT );
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
     * Appends a formatted space entry to the StringBuilder.
     *
     * @param sb
     *            the StringBuilder to append to
     * @param index
     *            the space index number
     * @param space
     *            the space item
     * @param revision
     *            the space revision
     * @param title
     *            the space title
     */
    private void appendSpaceEntry( StringBuilder sb, int index, AbstractWikiItem space, Revision revision, String title )
    {
        sb.append( "## " ).append( index ).append( ". " ).append( title ).append( "\n" );
        sb.append( MSG_CODE_LABEL_FORMATTED ).append( space.getCode( ) ).append( "`\n" );

        if ( revision != null && revision.getDescription( ) != null && !revision.getDescription( ).isEmpty( ) )
        {
            sb.append( MSG_DESCRIPTION_LABEL ).append( revision.getDescription( ) ).append( "\n" );
        }
        sb.append( "\n" );
    }
}
