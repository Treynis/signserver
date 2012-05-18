<%@ page pageEncoding="ISO-8859-1"%>
<%@ page contentType="text/html; charset=@page.encoding@" %>
<%@page errorPage="/errorpage.jsp" import="java.util.*, org.ejbca.ui.web.admin.configuration.EjbcaWebBean,org.ejbca.core.model.ra.raadmin.GlobalConfiguration, org.ejbca.core.model.SecConst, 
              org.ejbca.core.model.authorization.AuthorizationDeniedException, org.ejbca.core.model.authorization.AvailableAccessRules,
               org.ejbca.ui.web.admin.rainterface.RAInterfaceBean, org.ejbca.core.model.ra.userdatasource.*, org.ejbca.ui.web.admin.rainterface.EditUserDataSourceJSPHelper, 
               org.ejbca.util.dn.DNFieldExtractor"%>

<html>
<jsp:useBean id="ejbcawebbean" scope="session" class="org.ejbca.ui.web.admin.configuration.EjbcaWebBean" />
<jsp:useBean id="rabean" scope="session" class="org.ejbca.ui.web.admin.rainterface.RAInterfaceBean" />
<jsp:useBean id="userdatasourcehelper" scope="session" class="org.ejbca.ui.web.admin.rainterface.EditUserDataSourceJSPHelper" />

<% 

  // Initialize environment
  String includefile = "userdatasourcespage.jspf"; 


  GlobalConfiguration globalconfiguration = ejbcawebbean.initialize(request, AvailableAccessRules.REGULAR_EDITUSERDATASOURCES); 
                                            rabean.initialize(request, ejbcawebbean); 
                                            userdatasourcehelper.initialize(request,ejbcawebbean, rabean);
  String THIS_FILENAME            =  globalconfiguration.getRaPath()  + "/edituserdatasources/edituserdatasources.jsp";
  
%>
 
<head>
  <title><%= globalconfiguration .getEjbcaTitle() %></title>
  <base href="<%= ejbcawebbean.getBaseUrl() %>">
  <link rel=STYLESHEET href="<%= ejbcawebbean.getCssFile() %>">
  <script language=javascript src="<%= globalconfiguration .getAdminWebPath() %>ejbcajslib.js"></script>
</head>
<body>

<%  // Determine action 

  includefile = userdatasourcehelper.parseRequest(request);

 // Include page
  if( includefile.equals("userdatasourcepage.jspf")){ 
%>
   <%@ include file="userdatasourcepage.jspf" %>
<%}
  if( includefile.equals("userdatasourcespage.jspf")){ %>
   <%@ include file="userdatasourcespage.jspf" %> 
<%} 

   // Include Footer 
   String footurl =   globalconfiguration.getFootBanner(); %>
   
  <jsp:include page="<%= footurl %>" />

</body>
</html>
