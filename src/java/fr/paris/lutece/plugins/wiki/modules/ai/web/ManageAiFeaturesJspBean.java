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
package fr.paris.lutece.plugins.wiki.modules.ai.web;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

import fr.paris.lutece.plugins.wiki.modules.ai.business.AiFeature;
import fr.paris.lutece.plugins.wiki.modules.ai.business.AiFeatureHome;
import fr.paris.lutece.portal.service.message.AdminMessage;
import fr.paris.lutece.portal.service.message.AdminMessageService;
import fr.paris.lutece.portal.util.mvc.admin.MVCAdminJspBean;
import fr.paris.lutece.portal.util.mvc.admin.annotations.Controller;
import fr.paris.lutece.portal.util.mvc.commons.annotations.Action;
import fr.paris.lutece.portal.util.mvc.commons.annotations.View;
import fr.paris.lutece.portal.web.cdi.mvc.Models;
import fr.paris.lutece.util.url.UrlItem;

@SessionScoped
@Named
@Controller( controllerJsp = "ManageAiFeatures.jsp", controllerPath = "jsp/admin/plugins/wiki/modules/ai/", right = "WIKI_AI_FEATURES" )
public class ManageAiFeaturesJspBean extends MVCAdminJspBean
{
    private static final long serialVersionUID = 1L;

    private static final String TEMPLATE_MANAGE_FEATURES = "/admin/plugins/wiki/modules/ai/manage_ai_features.html";
    private static final String TEMPLATE_CREATE_FEATURE = "/admin/plugins/wiki/modules/ai/create_ai_feature.html";
    private static final String TEMPLATE_MODIFY_FEATURE = "/admin/plugins/wiki/modules/ai/modify_ai_feature.html";
    private static final String PARAMETER_ID_FEATURE = "id_feature";
    private static final String VIEW_MANAGE_FEATURES = "manageFeatures";
    private static final String VIEW_CREATE_FEATURE = "createFeature";
    private static final String VIEW_MODIFY_FEATURE = "modifyFeature";
    private static final String ACTION_CREATE_FEATURE = "createFeature";
    private static final String ACTION_MODIFY_FEATURE = "modifyFeature";
    private static final String ACTION_REMOVE_FEATURE = "removeFeature";
    private static final String ACTION_CONFIRM_REMOVE_FEATURE = "confirmRemoveFeature";
    private static final String PROPERTY_PAGE_TITLE_MANAGE = "module.wiki.ai.manage_features.pageTitle";
    private static final String PROPERTY_PAGE_TITLE_CREATE = "module.wiki.ai.create_feature.pageTitle";
    private static final String PROPERTY_PAGE_TITLE_MODIFY = "module.wiki.ai.modify_feature.pageTitle";
    private static final String VALIDATION_ATTRIBUTES_PREFIX = "module.wiki.ai.create_feature.attribute.";
    private static final String MARK_FEATURE = "feature";
    private static final String MARK_FEATURE_LIST = "feature_list";
    private static final String INFO_FEATURE_CREATED = "module.wiki.ai.info.feature.created";
    private static final String INFO_FEATURE_UPDATED = "module.wiki.ai.info.feature.updated";
    private static final String INFO_FEATURE_REMOVED = "module.wiki.ai.info.feature.removed";
    private static final String MESSAGE_CONFIRM_REMOVE_FEATURE = "module.wiki.ai.message.confirmRemoveFeature";

    @Inject
    private Models _models;

    private AiFeature _feature;

    /**
     * Displays the manage AI features page
     *
     * @param request
     *            the HTTP request
     * @return the manage features view
     */
    @View( value = VIEW_MANAGE_FEATURES, defaultView = true )
    public String getManageFeatures( HttpServletRequest request )
    {
        _feature = null;
        List<AiFeature> listFeatures = AiFeatureHome.getAiFeaturesList( );
        _models.put( MARK_FEATURE_LIST, listFeatures );
        return getPage( PROPERTY_PAGE_TITLE_MANAGE, TEMPLATE_MANAGE_FEATURES );
    }

    /**
     * Displays the create AI feature page
     *
     * @param request
     *            the HTTP request
     * @return the create feature view
     */
    @View( VIEW_CREATE_FEATURE )
    public String getCreateFeature( HttpServletRequest request )
    {
        _feature = new AiFeature( );
        List<AiFeature> listFeatures = AiFeatureHome.getAiFeaturesList( );
        _feature.setOrder( listFeatures.size( ) + 1 );
        _models.put( MARK_FEATURE, _feature );
        _models.put( MARK_FEATURE_LIST, listFeatures );
        return getPage( PROPERTY_PAGE_TITLE_CREATE, TEMPLATE_CREATE_FEATURE );
    }

    /**
     * Creates a new AI feature
     *
     * @param request
     *            the HTTP request
     * @return the view after feature creation
     */
    @Action( ACTION_CREATE_FEATURE )
    public String doCreateFeature( HttpServletRequest request )
    {
        populate( _feature, request );
        if ( !validateBean( _feature, VALIDATION_ATTRIBUTES_PREFIX ) )
        {
            return redirectView( request, VIEW_CREATE_FEATURE );
        }
        AiFeatureHome.create( _feature );
        addInfo( INFO_FEATURE_CREATED, getLocale( ) );
        return redirectView( request, VIEW_MANAGE_FEATURES );
    }

    /**
     * Displays the modify AI feature page
     *
     * @param request
     *            the HTTP request
     * @return the modify feature view
     */
    @View( VIEW_MODIFY_FEATURE )
    public String getModifyFeature( HttpServletRequest request )
    {
        int nId = Integer.parseInt( request.getParameter( PARAMETER_ID_FEATURE ) );
        if ( _feature == null || _feature.getId( ) != nId )
        {
            Optional<AiFeature> optFeature = AiFeatureHome.findByPrimaryKey( nId );
            _feature = optFeature.orElse( null );
        }
        List<AiFeature> listFeatures = AiFeatureHome.getAiFeaturesList( );
        _models.put( MARK_FEATURE, _feature );
        _models.put( MARK_FEATURE_LIST, listFeatures );
        return getPage( PROPERTY_PAGE_TITLE_MODIFY, TEMPLATE_MODIFY_FEATURE );
    }

    /**
     * Modifies an existing AI feature
     *
     * @param request
     *            the HTTP request
     * @return the view after feature modification
     */
    @Action( ACTION_MODIFY_FEATURE )
    public String doModifyFeature( HttpServletRequest request )
    {
        populate( _feature, request );
        if ( !validateBean( _feature, VALIDATION_ATTRIBUTES_PREFIX ) )
        {
            return redirectView( request, VIEW_MODIFY_FEATURE );
        }
        AiFeatureHome.update( _feature );
        addInfo( INFO_FEATURE_UPDATED, getLocale( ) );
        return redirectView( request, VIEW_MANAGE_FEATURES );
    }

    /**
     * Displays the confirmation page for removing an AI feature
     *
     * @param request
     *            the HTTP request
     * @return the confirmation redirect URL
     */
    @Action( value = ACTION_CONFIRM_REMOVE_FEATURE, securityTokenAction = ACTION_REMOVE_FEATURE )
    public String getConfirmRemoveFeature( HttpServletRequest request )
    {
        int nId = Integer.parseInt( request.getParameter( PARAMETER_ID_FEATURE ) );
        UrlItem url = new UrlItem( getActionUrl( ACTION_REMOVE_FEATURE ) );
        url.addParameter( PARAMETER_ID_FEATURE, nId );
        String strMessageUrl = AdminMessageService.getMessageUrl( request, MESSAGE_CONFIRM_REMOVE_FEATURE, url.getUrl( ), AdminMessage.TYPE_CONFIRMATION );
        return redirect( request, strMessageUrl );
    }

    /**
     * Removes an AI feature
     *
     * @param request
     *            the HTTP request
     * @return the view after feature removal
     */
    @Action( ACTION_REMOVE_FEATURE )
    public String doRemoveFeature( HttpServletRequest request )
    {
        int nId = Integer.parseInt( request.getParameter( PARAMETER_ID_FEATURE ) );
        AiFeatureHome.remove( nId );
        addInfo( INFO_FEATURE_REMOVED, getLocale( ) );
        return redirectView( request, VIEW_MANAGE_FEATURES );
    }
}
