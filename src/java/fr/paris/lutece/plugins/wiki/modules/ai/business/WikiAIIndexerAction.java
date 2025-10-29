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

/**
 * WikiAIIndexerAction represents an indexation action for the Wiki AI module
 */
public class WikiAIIndexerAction
{
    public static final int TASK_CREATE = 1;
    public static final int TASK_MODIFY = 2;
    public static final int TASK_DELETE = 3;

    private int _nIdAction;
    private String _strIdDocument;
    private int _nIdTask;

    /**
     * Gets the action identifier
     *
     * @return the action identifier
     */
    public int getIdAction( )
    {
        return _nIdAction;
    }

    /**
     * Sets the action identifier
     *
     * @param nIdAction the action identifier
     */
    public void setIdAction( int nIdAction )
    {
        _nIdAction = nIdAction;
    }

    /**
     * Gets the document identifier
     *
     * @return the document identifier
     */
    public String getIdDocument( )
    {
        return _strIdDocument;
    }

    /**
     * Sets the document identifier
     *
     * @param strIdDocument the document identifier
     */
    public void setIdDocument( String strIdDocument )
    {
        _strIdDocument = strIdDocument;
    }

    /**
     * Gets the task identifier
     *
     * @return the task identifier
     */
    public int getIdTask( )
    {
        return _nIdTask;
    }

    /**
     * Sets the task identifier
     *
     * @param nIdTask the task identifier
     */
    public void setIdTask( int nIdTask )
    {
        _nIdTask = nIdTask;
    }
}
