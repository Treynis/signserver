<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ include file="header.jsp" %>

	<h1 class="title">Fetch CA & OCSP Certificates</h1>

	<jsp:useBean id="finder" class="org.ejbca.ui.web.pub.retrieve.CertificateFinderBean" scope="page" />
	<% finder.initialize(request.getRemoteAddr()); %>

	<c:forEach var="ca_id" items="${finder.availableCAs}">
		<jsp:useBean id="ca_id" type="java.lang.Integer" />
		<% finder.setCurrentCA(ca_id); %>

		<c:set var="ca" value="${finder.CAInfo}" />

		<hr />
		<h2><c:out value="CA: ${ca.name}" /></h2>

		<c:set var="chain" value="${finder.CACertificateChainReversed}" />
		<c:set var="chainsize" value="${fn:length(chain)}" />
	
		<c:choose>
			<c:when test="${chainsize == 0}">
				<p>No CA certificates exist</p>
			</c:when>
			<c:otherwise>
				<c:set var="issuercert" value="${chain[chainsize - 1]}" />
				<c:set var="issuerdn" value="${issuercert.subjectDN}" />

				<div>
				<c:forEach var="cert" items="${chain}" varStatus="status">
					<div style="padding-left: ${status.index}0px ; margin-left: ${status.index}0px ;">
					<p>
					<c:if test="${status.last}"><b></c:if>
						<i><c:out value="${cert.subjectDN}" /></i>
					<c:if test="${status.last}"></b></c:if>
					</p><p>
					<c:out value="CA certificate: " />
					<c:url var="pem" value="../publicweb/webdist/certdist" >
						<c:param name="cmd" value="cacert" />
						<c:param name="issuer" value="${issuerdn}" />
						<c:param name="level" value="${chainsize - status.count}" />
					</c:url>
					<a href="${pem}">Download as PEM</a>,
					<c:url var="ns" value="../publicweb/webdist/certdist" >
						<c:param name="cmd" value="nscacert" />
						<c:param name="issuer" value="${issuerdn}" />
						<c:param name="level" value="${chainsize - status.count}" />
					</c:url>
					<a href="${ns}">Download to Firefox</a>,
					<c:url var="ie" value="../publicweb/webdist/certdist" >
						<c:param name="cmd" value="iecacert" />
						<c:param name="issuer" value="${issuerdn}" />
						<c:param name="level" value="${chainsize - status.count}" />
					</c:url>
					<a href="${ie}">Download to Internet Explorer</a>
					</p>
					<c:if test="${status.last && finder.ocspEnabled}">
					<p>
					<c:out value="OCSP certificate: "/>
					<c:url var="pem_ocsp" value="../publicweb/webdist/certdist" >
						<c:param name="cmd" value="ocspcert" />
						<c:param name="issuer" value="${cert.subjectDN}" />
					</c:url>
					<a href="${pem_ocsp}">Download as PEM</a>,
					<!-- OCSP certificates cannot be imported to Firefox, so there is no point in showing this
					<c:url var="ns_ocsp" value="../publicweb/webdist/certdist" >
						<c:param name="cmd" value="nsocspcert" />
						<c:param name="issuer" value="${cert.subjectDN}" />
					</c:url>
					<a href="${ns_ocsp}">Download to Firefox</a>,
					-->
					<c:url var="ie_ocsp" value="../publicweb/webdist/certdist" >
						<c:param name="cmd" value="ieocspcert" />
						<c:param name="issuer" value="${cert.subjectDN}" />
					</c:url>
					<a href="${ie_ocsp}">Download to Internet Explorer</a>
					</p>
					</c:if>
					</div>
				</c:forEach>
				</div>
			</c:otherwise>
		</c:choose>
	</c:forEach>
	
<%@ include file="footer.inc" %>
