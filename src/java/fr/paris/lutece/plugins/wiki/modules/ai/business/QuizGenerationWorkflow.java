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
package fr.paris.lutece.plugins.wiki.modules.ai.business;

import java.io.Serializable;
import java.sql.Timestamp;

public class QuizGenerationWorkflow implements Serializable
{
    private static final long serialVersionUID = 1L;

    private int _nId;
    private int _nIdQuiz;
    private String _strIdUser;
    private WorkflowStatus _status = WorkflowStatus.PENDING;
    private String _strSelectedPageIds;
    private String _strErrorMessage;
    private int _nTotalPages;
    private int _nProcessedPages;
    private Timestamp _dateCreation;
    private Timestamp _dateCompletion;
    private String _strLocale;

    public int getId( )
    {
        return _nId;
    }

    public void setId( int nId )
    {
        _nId = nId;
    }

    public int getIdQuiz( )
    {
        return _nIdQuiz;
    }

    public void setIdQuiz( int nIdQuiz )
    {
        _nIdQuiz = nIdQuiz;
    }

    public String getIdUser( )
    {
        return _strIdUser;
    }

    public void setIdUser( String strIdUser )
    {
        _strIdUser = strIdUser;
    }

    public WorkflowStatus getStatus( )
    {
        return _status;
    }

    public void setStatus( WorkflowStatus status )
    {
        _status = status;
    }

    public void setStatusCode( String strStatus )
    {
        _status = WorkflowStatus.fromCode( strStatus );
    }

    public String getSelectedPageIds( )
    {
        return _strSelectedPageIds;
    }

    public void setSelectedPageIds( String strSelectedPageIds )
    {
        _strSelectedPageIds = strSelectedPageIds;
    }

    public String getErrorMessage( )
    {
        return _strErrorMessage;
    }

    public void setErrorMessage( String strErrorMessage )
    {
        _strErrorMessage = strErrorMessage;
    }

    public int getTotalPages( )
    {
        return _nTotalPages;
    }

    public void setTotalPages( int nTotalPages )
    {
        _nTotalPages = nTotalPages;
    }

    public int getProcessedPages( )
    {
        return _nProcessedPages;
    }

    public void setProcessedPages( int nProcessedPages )
    {
        _nProcessedPages = nProcessedPages;
    }

    public Timestamp getDateCreation( )
    {
        return _dateCreation;
    }

    public void setDateCreation( Timestamp dateCreation )
    {
        _dateCreation = dateCreation;
    }

    public Timestamp getDateCompletion( )
    {
        return _dateCompletion;
    }

    public void setDateCompletion( Timestamp dateCompletion )
    {
        _dateCompletion = dateCompletion;
    }

    public String getLocale( )
    {
        return _strLocale;
    }

    public void setLocale( String strLocale )
    {
        _strLocale = strLocale;
    }
}
