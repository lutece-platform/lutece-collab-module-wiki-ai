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
package fr.paris.lutece.plugins.wiki.modules.ai.service.rag.util;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tracks the indexing status of wiki content in Elasticsearch. Thread-safe implementation for concurrent access.
 */
public class IndexingStatus
{
    private int _nNbTotalObj;
    private int _nCurrentNbIndexedObj;
    private final AtomicBoolean _bIsRunning = new AtomicBoolean( false );
    private StringBuilder _sbLogs;

    /**
     * Get the total number of items to index.
     *
     * @return the total number of items
     */
    public int getNbTotalObj( )
    {
        return _nNbTotalObj;
    }

    /**
     * Set the total number of items to index.
     *
     * @param nNbTotalObj
     *            the total number of items
     */
    public void setNbTotalObj( int nNbTotalObj )
    {
        _nNbTotalObj = nNbTotalObj;
    }

    /**
     * Get the current number of indexed items.
     *
     * @return the current number of indexed items
     */
    public int getCurrentNbIndexedObj( )
    {
        return _nCurrentNbIndexedObj;
    }

    /**
     * Set the current number of indexed items.
     *
     * @param nCurrentNbIndexedObj
     *            the current number
     */
    public void setCurrentNbIndexedObj( int nCurrentNbIndexedObj )
    {
        _nCurrentNbIndexedObj = nCurrentNbIndexedObj;
    }

    /**
     * Returns the running flag as an AtomicBoolean.
     *
     * @return the running flag
     */
    public AtomicBoolean getIsRunning( )
    {
        return _bIsRunning;
    }

    /**
     * Get the progress percentage of indexed items.
     *
     * @return the progress percentage (0-100)
     */
    public double getProgress( )
    {
        if ( _nNbTotalObj == 0 )
        {
            return 0;
        }
        return (double) _nCurrentNbIndexedObj / (double) _nNbTotalObj * 100.0;
    }

    /**
     * Returns the logs as a string.
     *
     * @return the logs
     */
    public String getStringSbLogs( )
    {
        if ( _sbLogs == null )
        {
            _sbLogs = new StringBuilder( );
        }
        return _sbLogs.toString( );
    }

    /**
     * Returns the StringBuilder for logs.
     *
     * @return the StringBuilder
     */
    public StringBuilder getSbLogs( )
    {
        if ( _sbLogs == null )
        {
            _sbLogs = new StringBuilder( );
        }
        return _sbLogs;
    }

    /**
     * Reset the indexing status to initial state.
     */
    public void reset( )
    {
        _nNbTotalObj = 0;
        _nCurrentNbIndexedObj = 0;
        if ( _sbLogs != null )
        {
            _sbLogs.setLength( 0 );
        }
        else
        {
            _sbLogs = new StringBuilder( );
        }
    }
}
