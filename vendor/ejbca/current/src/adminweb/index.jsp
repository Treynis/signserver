<%@ page pageEncoding="ISO-8859-1"%>
<%@ page contentType="text/html; charset=@page.encoding@" %>
<%@page errorPage="errorpage.jsp" import="org.ejbca.core.model.ra.raadmin.GlobalConfiguration,org.ejbca.ui.web.RequestHelper"%>
<html>
<jsp:useBean id="ejbcawebbean" scope="session" class="org.ejbca.ui.web.admin.configuration.EjbcaWebBean" />
<jsp:setProperty name="ejbcawebbean" property="*" /> 
<%   // Initialize environment
  GlobalConfiguration globalconfiguration = ejbcawebbean.initialize(request,"/administrator"); 
%>
<head>
  <title><%= globalconfiguration.getEjbcaTitle() %></title>
  <base href="<%= ejbcawebbean.getBaseUrl() %>">
  <link rel="stylesheet" type="text/css" href="<%= ejbcawebbean.getCssFile() %>">
  <meta http-equiv="Content-Type" content="text/html; charset=<%= RequestHelper.getDefaultContentEncoding() %>">
</head>

<frameset rows="100,*" cols="*" frameborder="NO" border="0" framespacing="0"> 
  <frame name="<%= globalconfiguration.HEADERFRAME %>" scrolling="NO" noresize src="<%= globalconfiguration.getHeadBanner() %>" >
  <frameset cols="250,*" frameborder="NO" border="0" framespacing="0" rows="*"> 
    <frame name="<%= globalconfiguration.MENUFRAME %>" noresize scrolling="NO" src="<%= globalconfiguration.getAdminWebPath() +
                                                                                        globalconfiguration.getMenuFilename() %>">
    <frame name="<%= globalconfiguration.MAINFRAME %>" src="<%= globalconfiguration.getAdminWebPath() + globalconfiguration.getMainFilename() %>">
  </frameset>
</frameset>
<noframes>
<body>
  <h1><%= ejbcawebbean.getText("ERRORNOBROWSER") %></h1>
</body>
</noframes>
</html>
