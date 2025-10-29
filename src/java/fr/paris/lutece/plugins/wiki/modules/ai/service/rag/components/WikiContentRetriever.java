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
package fr.paris.lutece.plugins.wiki.modules.ai.service.rag.components;

import java.util.List;
import java.util.stream.Collectors;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import fr.paris.lutece.plugins.wiki.business.item.AbstractWikiItem;
import fr.paris.lutece.plugins.wiki.service.WikiItemService;
import fr.paris.lutece.plugins.wiki.service.security.WikiAccessControlService;
import fr.paris.lutece.portal.service.security.LuteceUser;

/**
 * Secure content retriever that applies hierarchical access control. Wraps another ContentRetriever and filters results based on Wiki permissions.
 */
public class WikiContentRetriever implements ContentRetriever
{
    private static final String FIELD_CODE = "code";

    private final ContentRetriever _delegate;
    private final LuteceUser _user;

    /**
     * Constructor
     *
     * @param delegate
     *            the underlying content retriever
     * @param user
     *            the Lutece user for access control
     */
    public WikiContentRetriever( ContentRetriever delegate, LuteceUser user )
    {
        _delegate = delegate;
        _user = user;
    }

    @Override
    public List<Content> retrieve( Query query )
    {
        List<Content> contents = _delegate.retrieve( query );

        return contents.stream( ).filter( this::hasAccess ).collect( Collectors.toList( ) );
    }

    /**
     * Checks if the user has access to the content based on hierarchical Wiki permissions.
     *
     * @param content
     *            the content to check
     * @return true if the user has access
     */
    private boolean hasAccess( Content content )
    {
        TextSegment segment = content.textSegment( );
        if ( segment == null || segment.metadata( ) == null )
        {
            return false;
        }

        String code = segment.metadata( ).getString( FIELD_CODE );

        if ( code == null )
        {
            return false;
        }

        AbstractWikiItem item = WikiItemService.findByCode( code );
        if ( item == null )
        {
            return false;
        }

        return WikiAccessControlService.canView( _user, item );
    }
}
