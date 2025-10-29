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

/**
 * AI User Rate Limit entity for tracking user message limits
 */
public class AiUserRateLimit implements Serializable
{
    private static final long serialVersionUID = 1L;

    private int _nId;
    private String _strUserId;
    private int _nMessageCount;
    private Timestamp _timestampDateFirstMessage;

    /**
     * Gets the ID
     * 
     * @return the ID
     */
    public int getId( )
    {
        return _nId;
    }

    /**
     * Sets the ID
     * 
     * @param nId
     *            the ID to set
     */
    public void setId( int nId )
    {
        _nId = nId;
    }

    /**
     * Gets the user ID
     * 
     * @return the user ID
     */
    public String getUserId( )
    {
        return _strUserId;
    }

    /**
     * Sets the user ID
     * 
     * @param strUserId
     *            the user ID to set
     */
    public void setUserId( String strUserId )
    {
        _strUserId = strUserId;
    }

    /**
     * Gets the message count
     * 
     * @return the message count
     */
    public int getMessageCount( )
    {
        return _nMessageCount;
    }

    /**
     * Sets the message count
     * 
     * @param nMessageCount
     *            the message count to set
     */
    public void setMessageCount( int nMessageCount )
    {
        _nMessageCount = nMessageCount;
    }

    /**
     * Gets the date of first message
     * 
     * @return the date of first message
     */
    public Timestamp getDateFirstMessage( )
    {
        return _timestampDateFirstMessage;
    }

    /**
     * Sets the date of first message
     * 
     * @param timestampDateFirstMessage
     *            the date of first message to set
     */
    public void setDateFirstMessage( Timestamp timestampDateFirstMessage )
    {
        _timestampDateFirstMessage = timestampDateFirstMessage;
    }
}
