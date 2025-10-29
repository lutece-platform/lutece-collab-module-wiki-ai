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
package fr.paris.lutece.plugins.wiki.modules.ai.service.quiz;

import java.util.Optional;

import fr.paris.lutece.plugins.wiki.modules.ai.business.QuizGenerationWorkflow;
import fr.paris.lutece.plugins.wiki.modules.ai.business.QuizGenerationWorkflowHome;
import fr.paris.lutece.portal.service.daemon.Daemon;
import fr.paris.lutece.portal.service.util.AppLogService;

public class QuizGenerationDaemon extends Daemon
{
    private static final String LOG_START = "QuizGenerationDaemon: Starting...";
    private static final String LOG_NO_PENDING = "QuizGenerationDaemon: No pending workflows.";
    private static final String LOG_PROCESSING = "QuizGenerationDaemon: Processing workflow #";
    private static final String LOG_COMPLETED = "QuizGenerationDaemon: Completed workflow #";
    private static final String LOG_ERROR = "QuizGenerationDaemon: Error processing workflow #";
    private static final String NEWLINE = "\r\n";

    @Override
    public void run( )
    {
        StringBuilder sbLogs = new StringBuilder( );
        sbLogs.append( LOG_START ).append( NEWLINE );

        Optional<QuizGenerationWorkflow> optWorkflow = QuizGenerationWorkflowHome.getFirstPending( );

        if ( optWorkflow.isEmpty( ) )
        {
            sbLogs.append( LOG_NO_PENDING ).append( NEWLINE );
            setLastRunLogs( sbLogs.toString( ) );
            return;
        }

        QuizGenerationWorkflow workflow = optWorkflow.get( );
        sbLogs.append( LOG_PROCESSING ).append( workflow.getId( ) ).append( NEWLINE );

        try
        {
            QuizGenerationService.getInstance( ).processWorkflow( workflow );
            sbLogs.append( LOG_COMPLETED ).append( workflow.getId( ) ).append( NEWLINE );
        }
        catch( Exception e )
        {
            sbLogs.append( LOG_ERROR ).append( workflow.getId( ) ).append( ": " ).append( e.getMessage( ) ).append( NEWLINE );
            AppLogService.error( LOG_ERROR + workflow.getId( ), e );
        }

        setLastRunLogs( sbLogs.toString( ) );
    }
}
