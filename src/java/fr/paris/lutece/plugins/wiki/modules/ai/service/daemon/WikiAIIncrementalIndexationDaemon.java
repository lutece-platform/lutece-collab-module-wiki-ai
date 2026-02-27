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
package fr.paris.lutece.plugins.wiki.modules.ai.service.daemon;

import java.util.List;

import fr.paris.lutece.plugins.wiki.business.item.impl.Book;
import fr.paris.lutece.plugins.wiki.business.item.impl.Page;
import fr.paris.lutece.plugins.wiki.business.item.impl.Space;
import fr.paris.lutece.plugins.wiki.modules.ai.business.WikiAIIndexerAction;
import fr.paris.lutece.plugins.wiki.modules.ai.business.WikiAIIndexerActionHome;
import fr.paris.lutece.plugins.wiki.modules.ai.service.WikiAIPlugin;
import fr.paris.lutece.plugins.wiki.modules.ai.service.rag.EmbeddingService;
import fr.paris.lutece.portal.service.daemon.Daemon;
import fr.paris.lutece.portal.service.util.AppLogService;
import jakarta.enterprise.inject.spi.CDI;

/**
 * WikiAIIncrementalIndexationDaemon Daemon for processing incremental indexation of Wiki AI items
 */
public class WikiAIIncrementalIndexationDaemon extends Daemon
{
    private static final String UNDERSCORE_SEPARATOR = "_";
    private static final String LOG_START = "WikiAIIncrementalIndexationDaemon: Starting incremental indexation...";
    private static final String LOG_PROCESSING = "Processing action #";
    private static final String LOG_TASK_CREATE = " - Task: CREATE - Document: ";
    private static final String LOG_TASK_MODIFY = " - Task: MODIFY - Document: ";
    private static final String LOG_TASK_DELETE = " - Task: DELETE - Document: ";
    private static final String LOG_ERROR = "Error processing action #";
    private static final String LOG_ERROR_PARAMETERIZED = "Error processing action #{}: {}";
    private static final String LOG_COMPLETED = "WikiAIIncrementalIndexationDaemon: Completed. Processed ";
    private static final String LOG_ACTIONS = " actions.";
    private static final String ERROR_INVALID_DOCUMENT_ID = "Invalid document ID format: {}";
    private static final String NEWLINE = "\r\n";

    /**
     * {@inheritDoc}
     */
    @Override
    public void run( )
    {
        StringBuilder sbLogs = new StringBuilder( );
        sbLogs.append( LOG_START ).append( NEWLINE );

        List<WikiAIIndexerAction> actions = WikiAIIndexerActionHome.getList( WikiAIPlugin.getPlugin( ) );

        int nProcessed = 0;

        for ( WikiAIIndexerAction action : actions )
        {
            try
            {
                sbLogs.append( LOG_PROCESSING ).append( action.getIdAction( ) );

                switch( action.getIdTask( ) )
                {
                    case WikiAIIndexerAction.TASK_CREATE:
                        sbLogs.append( LOG_TASK_CREATE ).append( action.getIdDocument( ) ).append( NEWLINE );
                        processIndexAction( action );
                        break;

                    case WikiAIIndexerAction.TASK_MODIFY:
                        sbLogs.append( LOG_TASK_MODIFY ).append( action.getIdDocument( ) ).append( NEWLINE );
                        processIndexAction( action );
                        break;

                    case WikiAIIndexerAction.TASK_DELETE:
                        sbLogs.append( LOG_TASK_DELETE ).append( action.getIdDocument( ) ).append( NEWLINE );
                        processDeleteAction( action );
                        break;

                    default:
                        break;
                }

                WikiAIIndexerActionHome.remove( action.getIdAction( ), WikiAIPlugin.getPlugin( ) );
                nProcessed++;
            }
            catch( Exception e )
            {
                String errorMsg = LOG_ERROR + action.getIdAction( ) + ": " + e.getMessage( );
                sbLogs.append( errorMsg ).append( NEWLINE );
                AppLogService.error( LOG_ERROR_PARAMETERIZED, action.getIdAction( ), e.getMessage( ), e );
            }
        }

        sbLogs.append( LOG_COMPLETED ).append( nProcessed ).append( LOG_ACTIONS ).append( NEWLINE );
        setLastRunLogs( sbLogs.toString( ) );
    }

    /**
     * Process an indexer action for create or modify operations.
     *
     * @param action
     *            the {@link WikiAIIndexerAction} to process
     */
    private void processIndexAction( WikiAIIndexerAction action )
    {
        String [ ] parts = action.getIdDocument( ).split( UNDERSCORE_SEPARATOR );

        if ( parts.length < 2 )
        {
            AppLogService.error( ERROR_INVALID_DOCUMENT_ID, action.getIdDocument( ) );
            return;
        }

        int entityId = Integer.parseInt( parts [1] );

        CDI.current( ).select( EmbeddingService.class ).get( ).indexItem( entityId );
    }

    /**
     * Process an indexer action for delete operations.
     *
     * @param action
     *            the {@link WikiAIIndexerAction} to process
     */
    private void processDeleteAction( WikiAIIndexerAction action )
    {
        String [ ] parts = action.getIdDocument( ).split( UNDERSCORE_SEPARATOR );

        if ( parts.length < 2 )
        {
            AppLogService.error( ERROR_INVALID_DOCUMENT_ID, action.getIdDocument( ) );
            return;
        }

        String resourceType = parts [0];
        int entityId = Integer.parseInt( parts [1] );

        switch( resourceType.toLowerCase( ) )
        {
            case Space.RESOURCE_TYPE:
                CDI.current( ).select( EmbeddingService.class ).get( ).removeSpace( entityId );
                break;

            case Book.RESOURCE_TYPE:
                CDI.current( ).select( EmbeddingService.class ).get( ).removeBook( entityId );
                break;

            case Page.RESOURCE_TYPE:
                CDI.current( ).select( EmbeddingService.class ).get( ).removePage( entityId );
                break;

            default:
                break;
        }
    }
}
