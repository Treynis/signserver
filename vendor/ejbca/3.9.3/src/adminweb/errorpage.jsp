<%@ page pageEncoding="ISO-8859-1"%>
<%@ page contentType="text/html;" %>
<%@page isErrorPage="true" import="org.ejbca.core.model.ra.raadmin.GlobalConfiguration, org.ejbca.core.model.authorization.AuthorizationDeniedException,
                                   org.ejbca.core.model.authorization.AuthenticationFailedException, org.ejbca.core.model.ca.catoken.CATokenOfflineException,
                                   org.ejbca.ui.web.ParameterError, org.ejbca.config.WebConfiguration"%>

<jsp:useBean id="ejbcawebbean" scope="session" class="org.ejbca.ui.web.admin.configuration.EjbcaWebBean" />
<jsp:setProperty name="ejbcawebbean" property="*" /> 

<%   // Initialize environment
   GlobalConfiguration globalconfiguration = ejbcawebbean.initialize_errorpage(request);

%>
<html>
<head>
  <title><%= globalconfiguration.getEjbcaTitle() %></title>
  <base href="<%= ejbcawebbean.getBaseUrl() %>">
  <link rel=STYLESHEET href="<%= ejbcawebbean.getCssFile() %>">
</head>
<body>
<br>
<br>
<% if( exception instanceof AuthorizationDeniedException){
       // Print Authorization Denied Exception.
     out.write("<H2>" + ejbcawebbean.getText("AUTHORIZATIONDENIED") + "</H2>");
     out.write("<H4>" + ejbcawebbean.getText("CAUSE") + " : " + exception.getMessage() + "</H4>");
     response.setStatus(HttpServletResponse.SC_OK);
   }
   else
   if( exception instanceof AuthenticationFailedException){
       // Print Authorization Denied Exception.
     out.write("<H2>" + ejbcawebbean.getText("AUTHORIZATIONDENIED") + "</H2>");
     out.write("<H4>" + ejbcawebbean.getText("CAUSE") + " : " + exception.getMessage() + "</H4>");
     response.setStatus(HttpServletResponse.SC_OK);
   }else
	if( exception instanceof CATokenOfflineException){
    // Print CATokenOfflineException.
    out.write("<H2>" + ejbcawebbean.getText("CATOKENISOFFLINE") + "</H2>");
    out.write("<H4>" + ejbcawebbean.getText("CAUSE") + " : " + exception.getMessage() + "</H4>");
    response.setStatus(HttpServletResponse.SC_OK);
   }else if ( exception instanceof ParameterError ) {
    out.write("<H2>" + exception.getLocalizedMessage() + "</H2>");
   } else {
       // Other exception occurred, print exception and stack trace.   
     out.write("<H2>" + WebConfiguration.notification(ejbcawebbean.getText("EXCEPTIONOCCURED")) + "</H2>");
     out.write("<H4>" + exception.getLocalizedMessage() + "</H4>");
     if ( WebConfiguration.doShowStackTraceOnErrorPage() ) {
         out.write("<br/><pre style=\"font-style: italic;\">");
         java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
         exception.printStackTrace(new java.io.PrintStream(baos));
         out.write(new String(baos.toByteArray()));
         out.write("</pre>");
     }
     exception.printStackTrace(); // Prints in server.log
     response.setStatus(HttpServletResponse.SC_OK);
   }
%>


</body>
</html>
