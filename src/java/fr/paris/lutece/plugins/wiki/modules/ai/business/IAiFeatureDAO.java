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

import java.util.List;
import java.util.Optional;

import fr.paris.lutece.portal.service.plugin.Plugin;

/**
 * IAiFeatureDAO interface for AI feature data access operations
 */
public interface IAiFeatureDAO
{
    /**
     * Inserts a new AI feature
     *
     * @param aiFeature
     *            the AI feature to insert
     * @param plugin
     *            the plugin
     */
    void insert( AiFeature aiFeature, Plugin plugin );

    /**
     * Loads an AI feature by identifier
     *
     * @param nId
     *            the feature identifier
     * @param plugin
     *            the plugin
     * @return an Optional containing the AI feature if found, empty otherwise
     */
    Optional<AiFeature> load( int nId, Plugin plugin );

    /**
     * Updates an existing AI feature
     *
     * @param aiFeature
     *            the AI feature to update
     * @param plugin
     *            the plugin
     */
    void store( AiFeature aiFeature, Plugin plugin );

    /**
     * Deletes an AI feature
     *
     * @param nId
     *            the feature identifier
     * @param plugin
     *            the plugin
     */
    void delete( int nId, Plugin plugin );

    /**
     * Retrieves all AI features
     *
     * @param plugin
     *            the plugin
     * @return a list of all AI features
     */
    List<AiFeature> selectAll( Plugin plugin );

    /**
     * Retrieves all active AI features
     *
     * @param plugin
     *            the plugin
     * @return a list of active AI features
     */
    List<AiFeature> selectActiveFeatures( Plugin plugin );

    /**
     * Retrieves active AI features by type
     *
     * @param strType
     *            the feature type
     * @param plugin
     *            the plugin
     * @return a list of active AI features matching the specified type
     */
    List<AiFeature> selectFeaturesByType( String strType, Plugin plugin );

    /**
     * Shifts order numbers up to make room for insertion at specified position
     *
     * @param nOrder
     *            the order position where the feature will be inserted
     * @param nExcludeId
     *            the feature id to exclude from shifting (-1 for new features)
     * @param plugin
     *            the plugin
     */
    void shiftOrdersUp( int nOrder, int nExcludeId, Plugin plugin );

    /**
     * Shifts order numbers down after removing a feature from specified position
     *
     * @param nOrder
     *            the order position being vacated
     * @param nExcludeId
     *            the feature id to exclude from shifting
     * @param plugin
     *            the plugin
     */
    void shiftOrdersDown( int nOrder, int nExcludeId, Plugin plugin );
}
