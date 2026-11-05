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
package fr.paris.lutece.plugins.wiki.modules.ai.service.chat;

/**
 * One request of an AI edit on a selection: a feature or a free instruction, with the answer when it is already known.
 */
public final class EditStep
{
    private final Integer _nFeatureId;
    private final String _strInstruction;
    private final String _strResult;

    /**
     * Creates a step.
     *
     * @param nFeatureId
     *            the AI feature to apply, or null for a free instruction
     * @param strInstruction
     *            the instruction typed by the user, or null for a feature
     * @param strResult
     *            the answer of the model, or null for the request to run
     */
    public EditStep( Integer nFeatureId, String strInstruction, String strResult )
    {
        _nFeatureId = nFeatureId;
        _strInstruction = strInstruction;
        _strResult = strResult;
    }

    /**
     * Returns the AI feature to apply.
     *
     * @return the feature identifier, or null for a free instruction
     */
    public Integer getFeatureId( )
    {
        return _nFeatureId;
    }

    /**
     * Returns the instruction typed by the user.
     *
     * @return the instruction, or null for a feature
     */
    public String getInstruction( )
    {
        return _strInstruction;
    }

    /**
     * Returns the answer of the model.
     *
     * @return the answer, or null for the request to run
     */
    public String getResult( )
    {
        return _strResult;
    }
}
