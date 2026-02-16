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

import java.util.Map;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import fr.paris.lutece.plugins.wiki.modules.ai.service.rag.EmbeddingService;
import fr.paris.lutece.plugins.wiki.modules.ai.service.rag.util.IndexingStatus;
import fr.paris.lutece.portal.business.user.AdminUser;
import fr.paris.lutece.portal.service.admin.AdminAuthenticationService;

/**
 * REST endpoint for managing wiki AI indexation operations.
 */
@RequestScoped
@Path( "wiki/ai/indexation" )
public class IndexationRest extends AbstractRestEndpoint
{
    private static final String PATH_FULL = "full";
    private static final String PATH_CLEAR = "clear";
    private static final String PATH_STATUS = "status";
    private static final String PATH_STATS = "stats";
    private static final String LOG_ERROR_INDEXATION = "Error in indexation REST API: ";
    private static final String RIGHT_WIKI_AI_MANAGEMENT = "WIKI_AI_MANAGEMENT";

    @Context
    private HttpServletRequest _request;

    @Inject
    private EmbeddingService _embeddingService;

    /**
     * Triggers a full reindexation of all wiki content.
     *
     * @return response containing success message or error details
     */
    @POST
    @Path( PATH_FULL )
    @Produces( MediaType.APPLICATION_JSON )
    public Response indexFull( )
    {
        if ( !isAuthorized( ) )
        {
            return createUnauthorizedResponse( );
        }

        try
        {
            _embeddingService.reindexAll( );
            return createSuccessResponse( WikiAIRestConstants.SUCCESS_INDEX_STARTED );
        }
        catch( Exception e )
        {
            return handleException( e, LOG_ERROR_INDEXATION );
        }
    }

    /**
     * Clears the entire index asynchronously.
     *
     * @return response containing success message or error details
     */
    @POST
    @Path( PATH_CLEAR )
    @Produces( MediaType.APPLICATION_JSON )
    public Response clearIndex( )
    {
        if ( !isAuthorized( ) )
        {
            return createUnauthorizedResponse( );
        }

        try
        {
            _embeddingService.clearIndexAsync( );
            return createSuccessResponse( WikiAIRestConstants.SUCCESS_CLEAR_STARTED );
        }
        catch( Exception e )
        {
            return handleException( e, LOG_ERROR_INDEXATION );
        }
    }

    /**
     * Retrieves the current indexing status.
     *
     * @return response containing the indexing status or error details
     */
    @GET
    @Path( PATH_STATUS )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getStatus( )
    {
        if ( !isAuthorized( ) )
        {
            return createUnauthorizedResponse( );
        }

        try
        {
            IndexingStatus status = _embeddingService.getIndexingStatus( );
            return Response.status( Response.Status.OK ).entity( status ).build( );
        }
        catch( Exception e )
        {
            return handleException( e, LOG_ERROR_INDEXATION );
        }
    }

    /**
     * Retrieves statistics about the current index.
     *
     * @return response containing the index statistics or error details
     */
    @GET
    @Path( PATH_STATS )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getIndexStatistics( )
    {
        if ( !isAuthorized( ) )
        {
            return createUnauthorizedResponse( );
        }

        try
        {
            Map<String, Object> stats = _embeddingService.getIndexStatistics( );
            return Response.status( Response.Status.OK ).entity( stats ).build( );
        }
        catch( Exception e )
        {
            return handleException( e, LOG_ERROR_INDEXATION );
        }
    }

    /**
     * Checks if the current user is authorized to access indexation endpoints.
     *
     * @return true if the user is authorized, false otherwise
     */
    private boolean isAuthorized( )
    {
        AdminUser adminUser = AdminAuthenticationService.getInstance( ).getRegisteredUser( _request );
        if ( adminUser == null )
        {
            return false;
        }
        return adminUser.checkRight( RIGHT_WIKI_AI_MANAGEMENT );
    }

    /**
     * Creates an unauthorized response based on the authentication status.
     *
     * @return response with appropriate error status and message
     */
    private Response createUnauthorizedResponse( )
    {
        AdminUser adminUser = AdminAuthenticationService.getInstance( ).getRegisteredUser( _request );
        if ( adminUser == null )
        {
            return createErrorResponse( Response.Status.UNAUTHORIZED, WikiAIRestConstants.ERROR_ADMIN_NOT_AUTHENTICATED );
        }
        return createErrorResponse( Response.Status.FORBIDDEN, WikiAIRestConstants.ERROR_NOT_AUTHORIZED );
    }
}
