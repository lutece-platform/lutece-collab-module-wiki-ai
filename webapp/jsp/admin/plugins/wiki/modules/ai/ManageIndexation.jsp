<jsp:useBean id="wikiAIAdmin" scope="session" class="fr.paris.lutece.plugins.wiki.modules.ai.web.WikiAIAdminJspBean" />
<% String strContent = wikiAIAdmin.processController ( request , response ); %>

<%@ page errorPage="../../../../ErrorPage.jsp" %>
<jsp:include page="../../../../AdminHeader.jsp" />

<%= strContent %>

<%@ include file="../../../../AdminFooter.jsp" %>
