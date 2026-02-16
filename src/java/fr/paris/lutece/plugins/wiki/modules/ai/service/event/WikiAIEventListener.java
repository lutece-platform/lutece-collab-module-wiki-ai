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
package fr.paris.lutece.plugins.wiki.modules.ai.service.event;

import java.util.List;

import fr.paris.lutece.plugins.wiki.business.item.AbstractWikiItem;
import fr.paris.lutece.plugins.wiki.business.item.impl.Book;
import fr.paris.lutece.plugins.wiki.business.item.impl.Page;
import fr.paris.lutece.plugins.wiki.business.item.impl.Space;
import fr.paris.lutece.plugins.wiki.business.revision.Revision;
import fr.paris.lutece.plugins.wiki.business.revision.RevisionHome;
import fr.paris.lutece.plugins.wiki.modules.ai.business.WikiAIIndexerAction;
import fr.paris.lutece.plugins.wiki.modules.ai.business.WikiAIIndexerActionHome;
import fr.paris.lutece.plugins.wiki.modules.ai.service.WikiAIPlugin;
import fr.paris.lutece.plugins.wiki.service.WikiItemService;
import fr.paris.lutece.portal.business.event.ResourceEvent;
import fr.paris.lutece.portal.service.event.EventAction;
import fr.paris.lutece.portal.service.event.Type;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * Event listener for Wiki AI module that handles indexing operations when wiki resources are created, updated or deleted.
 */
@ApplicationScoped
public class WikiAIEventListener
{
    private static final String DOCUMENT_ID_SEPARATOR = "_";

    /**
     * Handles resource creation events
     *
     * @param event
     *            the resource event
     */
    public void addedResource( @Observes @Type( EventAction.CREATE ) ResourceEvent event )
    {
        String strResourceType = event.getTypeResource( );
        if ( Revision.RESOURCE_TYPE.equals( strResourceType ) )
        {
            indexResource( event );
        }
    }

    /**
     * Handles resource deletion events
     *
     * @param event
     *            the resource event
     */
    public void deletedResource( @Observes @Type( EventAction.REMOVE ) ResourceEvent event )
    {
        removeFromIndex( event );
    }

    /**
     * Indexes a resource based on the resource event.
     *
     * @param event
     *            the resource event
     */
    private void indexResource( ResourceEvent event )
    {
        String strResourceType = event.getTypeResource( );
        String strIdResource = event.getIdResource( );

        if ( Revision.RESOURCE_TYPE.equals( strResourceType ) )
        {
            indexRevision( strIdResource );
        }
        else
        {
            indexWikiItem( strIdResource );
        }
    }

    /**
     * Indexes a revision if it is current and its associated item is published.
     *
     * @param strIdResource
     *            the revision ID as string
     */
    private void indexRevision( String strIdResource )
    {
        int nIdRevision = Integer.parseInt( strIdResource );
        Revision revision = RevisionHome.findByPrimaryKey( nIdRevision );

        if ( revision != null && revision.getIsCurrent( ) )
        {
            AbstractWikiItem item = WikiItemService.findById( revision.getEntityId( ) );

            if ( item != null && item.isPublished( ) && isHierarchyPublished( item ) )
            {
                addToIndex( item );
            }
        }
    }

    /**
     * Indexes a wiki item.
     *
     * @param strIdResource
     *            the item ID as string
     */
    private void indexWikiItem( String strIdResource )
    {
        int nId = Integer.parseInt( strIdResource );
        AbstractWikiItem item = WikiItemService.findById( nId );

        if ( item != null && item.isPublished( ) && isHierarchyPublished( item ) )
        {
            Revision currentRevision = RevisionHome.getCurrentRevision( nId );
            if ( currentRevision != null )
            {
                addToIndex( item );
            }
        }
    }

    /**
     * Removes a resource from the index based on the resource event.
     *
     * @param event
     *            the resource event
     */
    private void removeFromIndex( ResourceEvent event )
    {
        String strIdResource = event.getIdResource( );
        String strResourceType = event.getTypeResource( );
        removeWikiItem( strIdResource, strResourceType );
    }

    /**
     * Removes a wiki item and its children from the index.
     *
     * @param strIdResource
     *            the item ID as string
     * @param strResourceType
     *            the resource type
     */
    private void removeWikiItem( String strIdResource, String strResourceType )
    {
        int nId = Integer.parseInt( strIdResource );
        String documentId = strResourceType + DOCUMENT_ID_SEPARATOR + nId;

        WikiAIIndexerAction action = new WikiAIIndexerAction( );
        action.setIdDocument( documentId );
        action.setIdTask( WikiAIIndexerAction.TASK_DELETE );
        WikiAIIndexerActionHome.create( action, WikiAIPlugin.getPlugin( ) );

        List<AbstractWikiItem> children = WikiItemService.getItemsByParent( nId );
        for ( AbstractWikiItem child : children )
        {
            removeWikiItem( String.valueOf( child.getId( ) ), child.getResourceType( ) );
        }
    }

    /**
     * Adds a wiki item to the embedding index via IndexerAction.
     *
     * @param item
     *            the wiki item to add
     */
    private void addToIndex( AbstractWikiItem item )
    {
        if ( !Space.RESOURCE_TYPE.equals( item.getResourceType( ) ) && !Book.RESOURCE_TYPE.equals( item.getResourceType( ) )
                && !Page.RESOURCE_TYPE.equals( item.getResourceType( ) ) )
        {
            return;
        }

        String strId = item.getResourceType( ) + DOCUMENT_ID_SEPARATOR + item.getId( );

        WikiAIIndexerAction action = new WikiAIIndexerAction( );
        action.setIdDocument( strId );
        action.setIdTask( WikiAIIndexerAction.TASK_MODIFY );
        WikiAIIndexerActionHome.create( action, WikiAIPlugin.getPlugin( ) );
    }

    /**
     * Checks if all parent items in the hierarchy are published.
     *
     * @param item
     *            the wiki item
     * @return true if the entire hierarchy is published, false otherwise
     */
    private boolean isHierarchyPublished( AbstractWikiItem item )
    {
        AbstractWikiItem current = item;
        while ( current.getIdParent( ) != null )
        {
            AbstractWikiItem parent = WikiItemService.findById( current.getIdParent( ) );
            if ( parent == null || !parent.isPublished( ) )
            {
                return false;
            }
            current = parent;
        }
        return true;
    }
}
