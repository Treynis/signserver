/*************************************************************************
 *                                                                       *
 *  EJBCA: The OpenSource Certificate Authority                          *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.ejbca.ui.web.pub;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.rmi.RemoteException;
import java.security.cert.CertStore;
import java.security.cert.X509Certificate;
import java.util.Iterator;

import javax.naming.Context;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.ejbca.core.ejb.ca.store.ICertificateStoreSessionHome;
import org.ejbca.core.ejb.ca.store.ICertificateStoreSessionRemote;
import org.ejbca.core.ejb.ra.IUserAdminSessionHome;
import org.ejbca.core.ejb.ra.IUserAdminSessionRemote;
import org.ejbca.core.ejb.ra.raadmin.IRaAdminSessionHome;
import org.ejbca.core.ejb.ra.raadmin.IRaAdminSessionRemote;
import org.ejbca.core.model.ca.crl.RevokedCertInfo;
import org.ejbca.core.model.log.Admin;
import org.ejbca.util.Base64;
import org.ejbca.util.CertTools;

/**
 * @version $Id: TestAutoEnrollServlet.java 6976 2009-02-19 09:04:09Z anatom $
 */
public class TestAutoEnrollServlet extends TestCase {

	private static Logger log = Logger.getLogger(TestAutoEnrollServlet.class);

	private static Context context = null;
	private static IUserAdminSessionRemote userAdminSession = null;    
	private static ICertificateStoreSessionRemote certificateStoreSession = null;
	private static IRaAdminSessionRemote raAdminSession = null;

	private static final String CERTREQ_MACHINE_TEMPLATE =
		"-----BEGIN NEW CERTIFICATE REQUEST-----" +
		"MIIENzCCAx8CAQAwFzEVMBMGA1UEAxMMSWdub3JlZFZhbHVlMIIBIjANBgkqhkiG" +
		"9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvbtvsiUnHcQEq19jD80xXMtFA4IXsQ3sc3eM" +
		"YKBGFpaXekQUWmTHP/PMjZa8zMCJTCQg2sagOHNXBieL994oc+zUwlCEjzdYS61F" +
		"3sDFVnKXUHWUvDwg9cl2XyhlPnVQguozmdUDOLMoxwUD1a9i2vyiVAs/MtCdJA8X" +
		"I5IBew6FEkZ6SkJTtqT5SJGcE3ZxErpDGILpgYAYeUb3id5IFed1qqaJWJS5nrJC" +
		"5D6jadxU0Dsi84+eB60TjkFlkrCbw5xdjwa+9UnUZq/VJlpZZNy/F3wupNExCZ8N" +
		"vvYi36pPuOaSHvuW+A6tPg3CMVJltg7lG1RXhcG0xd5WDB9FFQIDAQABoIIB2TAa" +
		"BgorBgEEAYI3DQIDMQwWCjUuMi4zNzkwLjIwTAYJKwYBBAGCNxUUMT8wPQIBAQwX" +
		"Y29tcGFueS0xLkNvbXBhbnkubG9jYWwMEkNPTVBBTllcQ09NUEFOWS0xJAwLY2Vy" +
		"dHJlcS5leGUweQYJKoZIhvcNAQkOMWwwajAdBgNVHQ4EFgQUmTzIuRJLmlsIJwc9" +
		"8I4amvMqIGwwCwYDVR0PBAQDAgWgMB0GA1UdJQQWMBQGCCsGAQUFBwMCBggrBgEF" +
		"BQcDATAdBgkrBgEEAYI3FAIEEB4OAE0AYQBjAGgAaQBuAGUwgfEGCisGAQQBgjcN" +
		"AgIxgeIwgd8CAQIeTgBNAGkAYwByAG8AcwBvAGYAdAAgAFMAdAByAG8AbgBnACAA" +
		"QwByAHkAcAB0AG8AZwByAGEAcABoAGkAYwAgAFAAcgBvAHYAaQBkAGUAcgOBiQAA" +
		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMA0GCSqGSIb3" +
		"DQEBBQUAA4IBAQAYQL1PwoOaW/PoAFq+eqThq6vBYNBzvUYEsKCxwQz5k6gC6Rpt" +
		"2Hf9r+EFtlYSvHHSl8p4PMNhc2eqM3XT5QQ/w5IK2JG8b4gfA8Wy97pqo1iEHS/p" +
		"cikfVVFHH8dIxpyDtjz5asHj3xx6vLWeLtPVvcyIkt4StrFMq/pRshjJ97dQ5v4p" +
		"KkdSR5oEPU43zt8Ee6HIOY+B38hL6bHGU+vM2alSX+2kOTgK8SV3ZygaZmU+S1gY" +
		"OfGHAVoS03ZFkkzqUwd57B33ZZuJHz7yk9yoRHYT5UYtsbuJQYRycW3QpMkhOyGR" +
		"k3tnnw+9lDqfg9O66/Q22kaTrbuKatbK6YX0" +
		"-----END NEW CERTIFICATE REQUEST-----";

	private static final String CERTREQ_USER2_TEMPLATE =
		"-----BEGIN NEW CERTIFICATE REQUEST-----" +
		"MIIEQDCCAygCAQAwFzEVMBMGA1UEAxMMSWdub3JlZFZhbHVlMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqsrIJXjLqx0miC9skRZ8VISbPFJcEXbRtXYVD5QPc4ey1MhzQXYuN9d6O48vZUfAYOtIFINSfK4aFymWx/wyPCPB4aPWMBsRAL7n7sOv4Qs9owHoq7xCYsy31dx6zht0THqTWVI8ZpUZDE1eXh7hUGTcbxUu89KPM/5iUhI3u8pRkenPyo9tBNpTaalY+f2RwaIooJoLteiGZNZHsuN327K6u4yMrAsswOjTzGPuxQs/9JSxOgJTr0J60eqdG8LDY+47bx6pGHn9PH9dfdbdlWixIz6/p8N5CGQrLmEEunkIUl8sFjXxm34WKVHkct4V85/CIn4FIENJ9w9ctLXtcwIDAQABoIIB4jAaBgorBgEEAYI3DQIDMQwWCjUuMi4zNzkwLjIwTwYJKwYBBAGCNxUUMUIwQAIBAQwXY29tcGFueS0xLkNvbXBhbnkubG9jYWwMFUNPTVBBTllcQWRtaW5pc3RyYXRvcgwLY2VydHJlcS5leGUwfwYJKoZIhvcNAQkOMXIwcDAdBgNVHQ4EFgQULYfeDWF36Inuy4JAq5SyMYrlLCAwCwYDVR0PBAQDAgWgMCkGA1UdJQQiMCAGCisGAQQBgjcKAwQGCCsGAQUFBwMEBggrBgEFBQcDAjAXBgkrBgEEAYI3FAIECh4IAFUAcwBlAHIwgfEGCisGAQQBgjcNAgIxgeIwgd8CAQIeTgBNAGkAYwByAG8AcwBvAGYAdAAgAFMAdAByAG8AbgBnACAAQwByAHkAcAB0AG8AZwByAGEAcABoAGkAYwAgAFAAcgBvAHYAaQBkAGUAcgOBiQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMA0GCSqGSIb3DQEBBQUAA4IBAQCYDRy7JWWAalqo0M8jVGiiDJvS9MXRMcVfH6MypI+kBfi/KotzpLsHJeHjttjNKyuVUIm9BPmTucX21HsqehdM7B1xCBF1k4VHHsh68heIZQeG2NbvzXLA4k2MUCKybg9fi+7TMUcdvfg9HcBbOeqiiWWPFgyUUr3vxI7X3knVMYzVrDMm/sDthajo+1bK2NTjRQI/dPCuorND/4uK6wSL1TI4HjVwPWFVNKqrICNbhBtieUEZf53wjFP5XjgbHk3aMiS/RqNJhhYRDBAjR7YPHh417kR7duVLBYNorcAe9ccKtOGFmYcHg4GtlIN3cA3DfKHYlF1zEgem+tUJAhy1" + 
		"-----END NEW CERTIFICATE REQUEST-----";

	//GUID = 8274021d2b2fba42877e054397c77060
	//DNS = company-1.Company.local
	private static final String CERTREQ_DOMAIN_CONTROLLER_TEMPLATE =
		"-----BEGIN NEW CERTIFICATE REQUEST-----" +
		"MIIEoDCCA4gCAQAwFzEVMBMGA1UEAxMMSWdub3JlZFZhbHVlMIIBIjANBgkqhkiG" +
		"9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuOZTCzrHxE+wfbCWY1gVjr2dNNttMFNrupYl" +
		"AAgTe8nfjwfc+nNYuql03KV8pjtFJPbB9N/Jjj9iBg1sJ+QDN7akxStsEFtu5rY8" +
		"OKqJ68BRIAPVV+UjDixEd42oigvTogxvq8megcUmFrM1uHvKyaXbXvs71WpmSIQY" +
		"uKSdnF/Q+X4ps391To0hkt8ov1lJ6pZH2OYjOsFs9nrPMPDBtMEaCKVJ6vp2RV+C" +
		"o2h3WmvWo/Urlx3pZTGMJELiWxnPyGe7OXcgFDOr9RFI9QBe8dem6DKLZvrekNdZ" +
		"azU9b6wRgfZ1MwwHed4YKWgznAXAOydyu8FY8Q0DToFFkWGDNQIDAQABoIICQjAa" +
		"BgorBgEEAYI3DQIDMQwWCjUuMi4zNzkwLjIwTAYJKwYBBAGCNxUUMT8wPQIBAQwX" +
		"Y29tcGFueS0xLkNvbXBhbnkubG9jYWwMEkNPTVBBTllcQ09NUEFOWS0xJAwLY2Vy" +
		"dHJlcS5leGUwgdUGCSqGSIb3DQEJDjGBxzCBxDAdBgNVHQ4EFgQUu+V3exzk7a20" +
		"bsMjHKMaBrmhjyswLwYJKwYBBAGCNxQCBCIeIABEAG8AbQBhAGkAbgBDAG8AbgB0" +
		"AHIAbwBsAGwAZQByMEYGA1UdEQEB/wQ8MDqCF2NvbXBhbnktMS5Db21wYW55Lmxv" +
		"Y2FsoB8GCSsGAQQBgjcZAaASBBCCdAIdKy+6Qod+BUOXx3BgMB0GA1UdJQQWMBQG" +
		"CCsGAQUFBwMBBggrBgEFBQcDAjALBgNVHQ8EBAMCBaAwgf0GCisGAQQBgjcNAgIx" +
		"ge4wgesCAQEeWgBNAGkAYwByAG8AcwBvAGYAdAAgAFIAUwBBACAAUwBDAGgAYQBu" +
		"AG4AZQBsACAAQwByAHkAcAB0AG8AZwByAGEAcABoAGkAYwAgAFAAcgBvAHYAaQBk" +
		"AGUAcgOBiQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
		"MA0GCSqGSIb3DQEBBQUAA4IBAQCMp+XXrpyoC5vvJIqbZX8CeWRlQRTRincQVy3V" +
		"kaXiG8cQeAHtyWUsSDUdLdD2SQigSbwKnKnhnnxQgBdap7HTuArb4Iap4Fgs9BLC" +
		"3CQtPw1SwLRs/2YSUfrFRIbf8RROTp3xH+f4kHoSrb8ZhQ+cWubeKNivQ9YxTvlA" +
		"5bS2VYxzHeJQqdEtWc3AcWJ4ucAB7Xsosx1rPD83SQ6D8Z+QRH6jbhogQFdNatnY" +
		"zHneCzA4sIVXUzLpA9FYMRymckYGXuCmkn2PC6Dqlg43rF0Bq5ybsGw6MT3VXxfF" +
		"k/yUS9D7wZQ+vMw1zbz/mnAX3F38eMmurUKMayRNmORXvJwq" +
		"-----END NEW CERTIFICATE REQUEST-----";

	private Admin admin = new Admin(Admin.TYPE_INTERNALUSER);

	public TestAutoEnrollServlet(String name) {
		super(name);
	}

	boolean installedBCProvider = false;

	protected void setUp() throws Exception {
		log.trace(">setUp");
		if (!installedBCProvider) {
			CertTools.installBCProvider();
			installedBCProvider = true;
		}

		if (context == null) {
			context = new javax.naming.InitialContext();
		}
		if (userAdminSession == null) {
			userAdminSession = ((IUserAdminSessionHome) javax.rmi.PortableRemoteObject.narrow(
					context.lookup(IUserAdminSessionHome.JNDI_NAME), IUserAdminSessionHome.class)).create();
		}
		if (certificateStoreSession == null) {
			certificateStoreSession = ((ICertificateStoreSessionHome) javax.rmi.PortableRemoteObject.narrow(
					context.lookup(ICertificateStoreSessionHome.JNDI_NAME), ICertificateStoreSessionHome.class)).create();        
		}
		if (raAdminSession == null) {
			raAdminSession = ((IRaAdminSessionHome) javax.rmi.PortableRemoteObject.narrow(
					context.lookup(IRaAdminSessionHome.JNDI_NAME), IRaAdminSessionHome.class)).create();        
		}
		log.trace("<setUp");
	}

	protected void tearDown() throws Exception {
		log.trace(">tearDown");
		log.trace("<tearDown");
	}

	/**
	 * Test if a User-template certificate request is handled ok. 
	 */
	public void test01TestUserRequest() throws Exception {
		log.trace(">test01TestUserRequest");
		String remoteUser = "AETester@COMPANY.LOCAL";
		X509Certificate cert = doRequest(remoteUser, CERTREQ_USER2_TEMPLATE); // "User"
		assertTrue("Returned certificate with wrong CN", ("CN=AETester".equals(cert.getSubjectDN().getName())));
		assertFalse("Returned certificate without critical EKU.", cert.getCriticalExtensionOIDs().isEmpty());
		boolean isExtendedKeyUsageCritical = false;
		Iterator i = cert.getCriticalExtensionOIDs().iterator();
		while (i.hasNext()) {
			if ("2.5.29.37".equals(i.next())) {
				isExtendedKeyUsageCritical = true;
			}
		}
		assertTrue(isExtendedKeyUsageCritical);
		assertEquals("OK", getStatus(remoteUser, "User"));
		cleanUp("AETester", "Autoenroll-User");
		assertEquals("NO_SUCH_USER", getStatus(remoteUser, "User"));
		log.trace("<test01TestUserRequest");
	}

	/**
	 * Test if a User-template certificate request is handled ok. 
	 */
	public void test02TestMachineRequest() throws Exception {
		log.trace(">test02TestMachineRequest");
		String remoteUser = "TESTSRV-1$@COMPANY.LOCAL";	
		X509Certificate cert = doRequest(remoteUser, CERTREQ_MACHINE_TEMPLATE); // "Machine"
		// Expecting the $-sign to be removed
		assertTrue("Returned certificate with wrong CN.", ("CN=TESTSRV-1".equals(cert.getSubjectDN().getName())));
		assertFalse("Returned certificate without critical EKU.", cert.getCriticalExtensionOIDs().isEmpty());
		boolean isExtendedKeyUsageCritical = false;
		Iterator i = cert.getCriticalExtensionOIDs().iterator();
		while (i.hasNext()) {
			if ("2.5.29.37".equals(i.next())) {
				isExtendedKeyUsageCritical = true;
			}
		}
		assertTrue(isExtendedKeyUsageCritical);
		cleanUp("TESTSRV-1", "Autoenroll-Machine");
		log.trace("<test02TestMachineRequest");
	}

	/**
	 * Test if a DomainController-template certificate request is handled ok. 
	 */
	public void test03TestDomainControllerRequest() throws Exception {
		log.trace(">test03TestDomainControllerRequest");
		String remoteUser = "TESTSRV-1$@COMPANY.LOCAL";	
		X509Certificate cert = doRequest(remoteUser, CERTREQ_DOMAIN_CONTROLLER_TEMPLATE); // "DomainController"
		// Expecting the $-sign to be removed
		assertTrue("Returned certificate with wrong CN.", ("CN=TESTSRV-1".equals(cert.getSubjectDN().getName())));
		assertFalse("Returned certificate without critical EKU.", cert.getCriticalExtensionOIDs().isEmpty());
		boolean isExtendedKeyUsageCritical = false;
		Iterator i = cert.getCriticalExtensionOIDs().iterator();
		while (i.hasNext()) {
			if ("2.5.29.37".equals(i.next())) {
				isExtendedKeyUsageCritical = true;
			}
		}
		assertTrue(isExtendedKeyUsageCritical);
		cleanUp("TESTSRV-1", "Autoenroll-DomainController");
		log.trace("<test03TestDomainControllerRequest");
	}

	/**
	 * Test if a SmartcardLogon-template certificate request is handled ok. 
	 */
	/*public void test04TestSmartcardLogonRequest() throws Exception {
		log.trace(">test04TestSmartcardLogonRequest");
		assertFalse("The test does not exist yet. Write it.", true);
		log.trace("<test04TestSmartcardLogonRequest");
	}*/

	/**
	 * Post Certificate request to Servlet 
	 */
	private X509Certificate doRequest(String remoteUser, String requestData)  throws Exception {
		URL localAutoEnrollServletURL = new URL("http://127.0.0.1:8080/ejbca/autoenroll");
		HttpURLConnection localServletConnection = (HttpURLConnection) localAutoEnrollServletURL.openConnection();
		localServletConnection.setRequestProperty("X-Remote-User", remoteUser);
		localServletConnection.setRequestMethod("POST");
		localServletConnection.setDoOutput(true);
		localServletConnection.connect();
		OutputStream os = localServletConnection.getOutputStream();
		os.write(("request="+requestData + "&").getBytes());
		os.write("debug=false&".getBytes());
		//os.write(("CertificateTemplate=" + certificateTemplate).getBytes());
		os.flush();
		os.close();
		InputStream is = localServletConnection.getInputStream();
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String response = "";
		while (br.ready()) {
			response += br.readLine();
		}
		assertFalse("AutoEnrollment has to be enabled for this test to work.", response.contains("Not allowed."));
		response = response.replaceFirst("-----BEGIN PKCS7-----", "").replaceFirst("-----END PKCS7-----", "");
		byte[] responseData = Base64.decode(response.getBytes());
		X509Certificate returnCertificate= null;
		CMSSignedData p7b = new CMSSignedData(responseData);
		CertStore certStore = p7b.getCertificatesAndCRLs("Collection", "BC");
		SignerInformationStore  signers = p7b.getSignerInfos();
		Iterator iter = signers.getSigners().iterator();
		while (iter.hasNext())
		{
			SignerInformation signer = (SignerInformation)iter.next();
			X509Certificate caCert = (X509Certificate) certStore.getCertificates(signer.getSID()).iterator().next();
			Iterator iter2 = certStore.getCertificates(null).iterator();
			if (iter2.hasNext()) {
				X509Certificate cert = (X509Certificate)iter2.next();
				if (!caCert.getSubjectDN().getName().equals(cert.getSubjectDN().getName())) {
					returnCertificate = cert;
				}
			}
		}
		assertNotNull("No requested certificate present in response.", returnCertificate);
		return returnCertificate;
	}

	/**
	 * Get status from Servlet 
	 */
	private String getStatus(String remoteUser, String certificateTemplate)  throws Exception {
		URL localAutoEnrollServletURL = new URL("http://127.0.0.1:8080/ejbca/autoenroll");
		HttpURLConnection localServletConnection = (HttpURLConnection) localAutoEnrollServletURL.openConnection();
		localServletConnection.setRequestProperty("X-Remote-User", remoteUser);
		localServletConnection.setRequestProperty("CertificateTemplate", certificateTemplate);
		localServletConnection.setRequestMethod("GET");
		localServletConnection.setDoOutput(false);
		localServletConnection.connect();
		InputStream is = localServletConnection.getInputStream();
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String response = "";
		while (br.ready()) {
			response += br.readLine();
		}
		assertNotNull("No response.", response);
		return response;
	}

	private void cleanUp(String username, String profileName) throws RemoteException{
		try {
			userAdminSession.revokeAndDeleteUser(admin, username, RevokedCertInfo.REVOKATION_REASON_UNSPECIFIED);
		} catch (Exception e) {
			log.debug(e);
		}
		raAdminSession.removeEndEntityProfile(admin, profileName);
		certificateStoreSession.removeCertificateProfile(admin, profileName);
	}
}
