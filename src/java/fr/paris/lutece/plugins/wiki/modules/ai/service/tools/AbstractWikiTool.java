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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.paris.lutece.plugins.wiki.business.item.AbstractWikiItem;
import fr.paris.lutece.plugins.wiki.service.WikiUrlService;
import fr.paris.lutece.portal.service.security.LuteceUser;

/**
 * Abstract base class for wiki AI tools. Provides common functionality for source tracking and user context.
 */
public abstract class AbstractWikiTool
{
    protected static final String MSG_CITATION_INSTRUCTION = "IMPORTANT: Use [Source code] format to cite sources (e.g., [Source fastdeploy]). You can cite multiple sources: [Source code1, code2].";
    protected static final String MSG_SOURCE_SEPARATOR = "---\n";
    protected static final String MSG_SOURCE_HEADER = "**[Source %s]**\n";
    protected static final String MSG_TITLE_LABEL = "Title: ";
    protected static final String MSG_CODE_LABEL = "Code: ";
    protected static final String MSG_CONTENT_LABEL = "\nContent:\n";
    protected static final String MSG_FOUND_SOURCES = "Found %d sources:\n\n";

    protected static final String METADATA_KEY_CODE = "code";
    protected static final String METADATA_KEY_TITLE = "title";

    private static final String KEY_CODE = "code";
    private static final String KEY_TITLE = "title";
    private static final String KEY_URL = "url";

    protected final LuteceUser _user;
    private final List<Map<String, Object>> _lastSources = new ArrayList<>( );

    /**
     * Constructor.
     *
     * @param user
     *            the current Lutece user for access control
     */
    protected AbstractWikiTool( LuteceUser user )
    {
        _user = user;
    }

    /**
     * Returns the list of sources collected during the last tool execution.
     *
     * @return the list of source metadata maps
     */
    public List<Map<String, Object>> getLastSources( )
    {
        return _lastSources;
    }

    /**
     * Clears all collected sources.
     */
    protected void clearSources( )
    {
        _lastSources.clear( );
    }

    /**
     * Adds a source to the collection with its metadata.
     *
     * @param item
     *            the wiki item
     * @param title
     *            the title of the wiki item, or null to use code as title
     */
    protected void addSource( AbstractWikiItem item, String title )
    {
        Map<String, Object> sourceMetadata = new HashMap<>( );
        sourceMetadata.put( KEY_CODE, item.getCode( ) );
        sourceMetadata.put( KEY_TITLE, title != null ? title : item.getCode( ) );
        sourceMetadata.put( KEY_URL, WikiUrlService.buildViewUrl( item ) );
        _lastSources.add( sourceMetadata );
    }

    /**
     * Appends a formatted source result to the StringBuilder.
     *
     * @param sb
     *            the StringBuilder to append to
     * @param code
     *            the source code
     * @param title
     *            the source title
     * @param text
     *            the content text
     */
    protected void appendSourceResult( StringBuilder sb, String code, String title, String text )
    {
        sb.append( MSG_SOURCE_SEPARATOR );
        sb.append( String.format( MSG_SOURCE_HEADER, code ) );
        sb.append( MSG_TITLE_LABEL ).append( title != null ? title : code ).append( "\n" );
        sb.append( MSG_CODE_LABEL ).append( code ).append( "\n" );
        sb.append( MSG_CONTENT_LABEL );
        sb.append( text ).append( "\n\n" );
    }
}
