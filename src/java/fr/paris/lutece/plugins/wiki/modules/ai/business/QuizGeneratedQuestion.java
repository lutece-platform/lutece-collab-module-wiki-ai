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
import java.util.ArrayList;
import java.util.List;

import fr.paris.lutece.plugins.wiki.business.item.impl.Page;

public class QuizGeneratedQuestion implements Serializable
{
    private static final long serialVersionUID = 1L;

    private int _nId;
    private int _nIdWorkflow;
    private int _nIdSourcePage;
    private String _strQuestionText;
    private String _strQuestionType;
    private String _strExplanation;
    private boolean _bIsImported;
    private boolean _bIsDiscarded;
    private transient List<Page> _listSourcePages;
    private transient List<QuizGeneratedAnswer> _listAnswers;

    public int getId( )
    {
        return _nId;
    }

    public void setId( int nId )
    {
        _nId = nId;
    }

    public int getIdWorkflow( )
    {
        return _nIdWorkflow;
    }

    public void setIdWorkflow( int nIdWorkflow )
    {
        _nIdWorkflow = nIdWorkflow;
    }

    public int getIdSourcePage( )
    {
        return _nIdSourcePage;
    }

    public void setIdSourcePage( int nIdSourcePage )
    {
        _nIdSourcePage = nIdSourcePage;
    }

    public String getQuestionText( )
    {
        return _strQuestionText;
    }

    public void setQuestionText( String strQuestionText )
    {
        _strQuestionText = strQuestionText;
    }

    public String getQuestionType( )
    {
        return _strQuestionType;
    }

    public void setQuestionType( String strQuestionType )
    {
        _strQuestionType = strQuestionType;
    }

    public String getExplanation( )
    {
        return _strExplanation;
    }

    public void setExplanation( String strExplanation )
    {
        _strExplanation = strExplanation;
    }

    public List<QuizGeneratedAnswer> getAnswers( )
    {
        if ( _listAnswers == null )
        {
            _listAnswers = new ArrayList<>( );
        }
        return _listAnswers;
    }

    public void setAnswers( List<QuizGeneratedAnswer> listAnswers )
    {
        _listAnswers = listAnswers;
    }

    public boolean isImported( )
    {
        return _bIsImported;
    }

    public void setImported( boolean bIsImported )
    {
        _bIsImported = bIsImported;
    }

    public boolean isDiscarded( )
    {
        return _bIsDiscarded;
    }

    public void setDiscarded( boolean bIsDiscarded )
    {
        _bIsDiscarded = bIsDiscarded;
    }

    public List<Page> getSourcePages( )
    {
        if ( _listSourcePages == null )
        {
            _listSourcePages = new ArrayList<>( );
        }
        return _listSourcePages;
    }

    public void setSourcePages( List<Page> listSourcePages )
    {
        _listSourcePages = listSourcePages;
    }
}
