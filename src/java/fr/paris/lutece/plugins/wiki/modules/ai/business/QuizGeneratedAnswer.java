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

public class QuizGeneratedAnswer implements Serializable
{
    private static final long serialVersionUID = 1L;

    private int _nId;
    private int _nIdQuestion;
    private String _strAnswerText;
    private boolean _bIsCorrect;
    private String _strMatchTarget;
    private Integer _nCorrectOrder;
    private int _nDisplayOrder;

    public int getId( )
    {
        return _nId;
    }

    public void setId( int nId )
    {
        _nId = nId;
    }

    public int getIdQuestion( )
    {
        return _nIdQuestion;
    }

    public void setIdQuestion( int nIdQuestion )
    {
        _nIdQuestion = nIdQuestion;
    }

    public String getAnswerText( )
    {
        return _strAnswerText;
    }

    public void setAnswerText( String strAnswerText )
    {
        _strAnswerText = strAnswerText;
    }

    public boolean getIsCorrect( )
    {
        return _bIsCorrect;
    }

    public void setIsCorrect( boolean bIsCorrect )
    {
        _bIsCorrect = bIsCorrect;
    }

    public String getMatchTarget( )
    {
        return _strMatchTarget;
    }

    public void setMatchTarget( String strMatchTarget )
    {
        _strMatchTarget = strMatchTarget;
    }

    public Integer getCorrectOrder( )
    {
        return _nCorrectOrder;
    }

    public void setCorrectOrder( Integer nCorrectOrder )
    {
        _nCorrectOrder = nCorrectOrder;
    }

    public int getDisplayOrder( )
    {
        return _nDisplayOrder;
    }

    public void setDisplayOrder( int nDisplayOrder )
    {
        _nDisplayOrder = nDisplayOrder;
    }
}
