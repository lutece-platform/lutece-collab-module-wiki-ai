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

/**
 * AiFeature business class
 */
public class AiFeature implements Serializable
{
    private static final long serialVersionUID = 1L;

    private int _nId;
    private String _strName;
    private String _strType;
    private String _strPromptTemplate;
    private boolean _bIsActive;
    private int _nOrder;
    private String _strDisplayMode;

    /**
     * Gets the feature identifier
     *
     * @return the feature identifier
     */
    public int getId( )
    {
        return _nId;
    }

    /**
     * Sets the feature identifier
     *
     * @param nId
     *            the feature identifier
     */
    public void setId( int nId )
    {
        _nId = nId;
    }

    /**
     * Gets the feature name
     *
     * @return the feature name
     */
    public String getName( )
    {
        return _strName;
    }

    /**
     * Sets the feature name
     *
     * @param strName
     *            the feature name
     */
    public void setName( String strName )
    {
        _strName = strName;
    }

    /**
     * Gets the feature type
     *
     * @return the feature type
     */
    public String getType( )
    {
        return _strType;
    }

    /**
     * Sets the feature type
     *
     * @param strType
     *            the feature type
     */
    public void setType( String strType )
    {
        _strType = strType;
    }

    /**
     * Gets the prompt template
     *
     * @return the prompt template
     */
    public String getPromptTemplate( )
    {
        return _strPromptTemplate;
    }

    /**
     * Sets the prompt template
     *
     * @param strPromptTemplate
     *            the prompt template
     */
    public void setPromptTemplate( String strPromptTemplate )
    {
        _strPromptTemplate = strPromptTemplate;
    }

    /**
     * Checks if the feature is active
     *
     * @return true if the feature is active, false otherwise
     */
    public boolean isActive( )
    {
        return _bIsActive;
    }

    /**
     * Sets the feature active status
     *
     * @param bIsActive
     *            the active status
     */
    public void setActive( boolean bIsActive )
    {
        _bIsActive = bIsActive;
    }

    /**
     * Gets the feature order
     *
     * @return the feature order
     */
    public int getOrder( )
    {
        return _nOrder;
    }

    /**
     * Sets the feature order
     *
     * @param nOrder
     *            the feature order
     */
    public void setOrder( int nOrder )
    {
        _nOrder = nOrder;
    }

    /**
     * Gets the display mode (dropdown or button)
     *
     * @return the display mode
     */
    public String getDisplayMode( )
    {
        return _strDisplayMode;
    }

    /**
     * Sets the display mode (dropdown or button)
     *
     * @param strDisplayMode
     *            the display mode
     */
    public void setDisplayMode( String strDisplayMode )
    {
        _strDisplayMode = strDisplayMode;
    }
}
