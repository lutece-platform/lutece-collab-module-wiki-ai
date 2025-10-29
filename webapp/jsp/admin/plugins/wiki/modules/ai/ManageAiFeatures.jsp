<jsp:useBean id="manageAiFeatures" scope="session" class="fr.paris.lutece.plugins.wiki.modules.ai.web.ManageAiFeaturesJspBean" />
<% String strContent = manageAiFeatures.processController ( request , response ); %>

<%@ page errorPage="../../../../ErrorPage.jsp" %>
<jsp:include page="../../../../AdminHeader.jsp" />

<%= strContent %>

<%@ include file="../../../../AdminFooter.jsp" %>
