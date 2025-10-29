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
package fr.paris.lutece.plugins.wiki.modules.ai.rs;

/**
 * Constants for Wiki AI REST endpoints including error messages, JSON keys, and success messages.
 */
public final class WikiAIRestConstants
{
    public static final String ERROR_NOT_AUTHENTICATED = "Vous devez être connecté pour accéder à cette ressource";
    public static final String ERROR_ADMIN_NOT_AUTHENTICATED = "Vous devez être connecté en tant qu'administrateur";
    public static final String ERROR_NOT_AUTHORIZED = "Vous n'avez pas les droits nécessaires pour accéder à cette ressource";
    public static final String ERROR_INTERNAL = "Erreur interne du serveur";
    public static final String ERROR_AUTHENTICATION_FAILED = "Authentification requise pour accéder à cette ressource";
    public static final String ERROR_TEXT_REQUIRED = "Le texte est obligatoire";
    public static final String ERROR_FEATURE_ID_REQUIRED = "L'identifiant de la feature est obligatoire";
    public static final String ERROR_FEATURE_NOT_FOUND = "Feature non trouvée";
    public static final String ERROR_QUERY_REQUIRED = "La requête est obligatoire";
    public static final String ERROR_CONVERSATION_ID_REQUIRED = "Identifiant de conversation requis";
    public static final String ERROR_INVALID_CONVERSATION_ID = "Format d'identifiant de conversation invalide";
    public static final String ERROR_STREAM_SETUP_FAILED = "Impossible d'établir la connexion de streaming";
    public static final String ERROR_HIGH_TRAFFIC = "Trop de requêtes simultanées, veuillez réessayer";
    public static final String ERROR_CREATING_CONVERSATION = "Erreur lors de la création de la conversation";

    public static final String KEY_ERROR = "error";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_RESPONSE = "response";
    public static final String KEY_CONVERSATION_ID = "conversation_id";
    public static final String KEY_STREAM_ID = "stream_id";
    public static final String KEY_TOKEN = "token";

    public static final String SUCCESS_INDEX_STARTED = "Indexation lancée avec succès";
    public static final String SUCCESS_INDEX_SPACE_STARTED = "Indexation du space lancée avec succès";
    public static final String ERROR_SPACE_CODE_REQUIRED = "Le code du space est obligatoire";
    public static final String SUCCESS_CLEAR_STARTED = "Nettoyage de l'index lancé avec succès";

    /**
     * Private constructor to prevent instantiation.
     */
    private WikiAIRestConstants( )
    {
    }
}
