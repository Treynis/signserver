<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ include file="header.jsp" %>

	<h1>@EJBCA@ Fetch CA CRL</h1>

	<jsp:useBean id="finder" class="org.ejbca.ui.web.pub.retrieve.CertificateFinderBean" scope="page" />
	<% finder.initialize(request.getRemoteAddr()); %>

	<c:forEach var="ca_id" items="${finder.availableCAs}">
		<jsp:useBean id="ca_id" type="java.lang.Integer" />
		<% finder.setCurrentCA(ca_id); %>

		<c:set var="caName" value="${finder.CAInfo.name}" />
		<c:set var="caDN" value="${finder.CADN}" />

		<c:url var="der" value="../publicweb/webdist/certdist" >
			<c:param name="cmd" value="crl" />
			<c:param name="issuer" value="${caDN}" />
		</c:url>
		<c:url var="pem" value="../publicweb/webdist/certdist" >
			<c:param name="cmd" value="crl" />
			<c:param name="format" value="PEM" />
			<c:param name="issuer" value="${caDN}" />
		</c:url>
		<c:url var="moz" value="../publicweb/webdist/certdist" >
			<c:param name="cmd" value="crl" />
			<c:param name="issuer" value="${caDN}" />
			<c:param name="moz" value="y" />
		</c:url>
		
		<c:url var="derdelta" value="../publicweb/webdist/certdist" >
			<c:param name="cmd" value="deltacrl" />
			<c:param name="issuer" value="${caDN}" />
		</c:url>
		<c:url var="pemdelta" value="../publicweb/webdist/certdist" >
			<c:param name="cmd" value="deltacrl" />
			<c:param name="format" value="PEM" />
			<c:param name="issuer" value="${caDN}" />
		</c:url>
		<c:url var="mozdelta" value="../publicweb/webdist/certdist" >
			<c:param name="cmd" value="deltacrl" />
			<c:param name="issuer" value="${caDN}" />
			<c:param name="moz" value="y" />
		</c:url>

		<hr />
		<h2>CA: ${caName}</h2>
		<p>The Certificate Revocation List is available in three ways:
		<table>
		<thead><tr><td>CRL</td><td>Delta CRL</td></tr></thead>
		<tbody>
		<tr>
		<td>
		<ul>
		  	<li><a href="${der}">DER format</a></li> 
		  	<li><a href="${pem}">PEM format</a></li> 
		  	<li><a href="${moz}">Firefox direct import</a></li>
		</ul>
		</td>
		<td>
		<ul>
		  	<li><a href="${derdelta}">DER format</a></li> 
		  	<li><a href="${pemdelta}">PEM format</a></li> 
		  	<li><a href="${mozdelta}">Firefox direct import</a></li>
		</ul>
		</td>
		</tr>
		</tbody>
		</table>
		</p>
	</c:forEach>
<%@ include file="footer.inc" %>
