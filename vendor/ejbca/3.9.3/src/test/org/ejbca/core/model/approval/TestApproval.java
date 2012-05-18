package org.ejbca.core.model.approval;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import junit.framework.TestCase;

import org.ejbca.util.Base64;
import org.ejbca.util.CertTools;

/**
 * Test to externalize an approval
 * @author Philip Vendil
 * $Id: TestApproval.java 5631 2008-05-22 11:46:54Z anatom $
 */

public class TestApproval extends TestCase {
	
    private static byte[] testcertenc = Base64.decode(("MIIDATCCAmqgAwIBAgIIczEoghAwc3EwDQYJKoZIhvcNAQEFBQAwLzEPMA0GA1UE"
            + "AxMGVGVzdENBMQ8wDQYDVQQKEwZBbmFUb20xCzAJBgNVBAYTAlNFMB4XDTAzMDky"
            + "NDA2NDgwNFoXDTA1MDkyMzA2NTgwNFowMzEQMA4GA1UEAxMHcDEydGVzdDESMBAG"
            + "A1UEChMJUHJpbWVUZXN0MQswCQYDVQQGEwJTRTCBnTANBgkqhkiG9w0BAQEFAAOB"
            + "iwAwgYcCgYEAnPAtfpU63/0h6InBmesN8FYS47hMvq/sliSBOMU0VqzlNNXuhD8a"
            + "3FypGfnPXvjJP5YX9ORu1xAfTNao2sSHLtrkNJQBv6jCRIMYbjjo84UFab2qhhaJ"
            + "wqJgkQNKu2LHy5gFUztxD8JIuFPoayp1n9JL/gqFDv6k81UnDGmHeFcCARGjggEi"
            + "MIIBHjAPBgNVHRMBAf8EBTADAQEAMA8GA1UdDwEB/wQFAwMHoAAwOwYDVR0lBDQw"
            + "MgYIKwYBBQUHAwEGCCsGAQUFBwMCBggrBgEFBQcDBAYIKwYBBQUHAwUGCCsGAQUF"
            + "BwMHMB0GA1UdDgQWBBTnT1aQ9I0Ud4OEfNJkSOgJSrsIoDAfBgNVHSMEGDAWgBRj"
            + "e/R2qFQkjqV0pXdEpvReD1eSUTAiBgNVHREEGzAZoBcGCisGAQQBgjcUAgOgCQwH"
            + "Zm9vQGZvbzASBgNVHSAECzAJMAcGBSkBAQEBMEUGA1UdHwQ+MDwwOqA4oDaGNGh0"
            + "dHA6Ly8xMjcuMC4wLjE6ODA4MC9lamJjYS93ZWJkaXN0L2NlcnRkaXN0P2NtZD1j"
            + "cmwwDQYJKoZIhvcNAQEFBQADgYEAU4CCcLoSUDGXJAOO9hGhvxQiwjGD2rVKCLR4"
            + "emox1mlQ5rgO9sSel6jHkwceaq4A55+qXAjQVsuy76UJnc8ncYX8f98uSYKcjxo/"
            + "ifn1eHMbL8dGLd5bc2GNBZkmhFIEoDvbfn9jo7phlS8iyvF2YhC4eso8Xb+T7+BZ"
            + "QUOBOvc=").getBytes());

	protected void setUp() throws Exception {
		super.setUp();
		CertTools.installBCProvider();
	}

	public void testWriteExternal() throws Exception {
		Certificate testcert = CertTools.getCertfromByteArray(testcertenc);
		ArrayList approvals = new ArrayList();
		
		Approval ap = new Approval("test");
		Date apDate = ap.getApprovalDate();
		ap.setApprovalCertificateAndUsername(true,testcert, "USERNAME");
		approvals.add(ap);
		
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	ObjectOutputStream oos = new ObjectOutputStream(baos);
    	
		int size = approvals.size();
		oos.writeInt(size);
		Iterator iter = approvals.iterator();
		while(iter.hasNext()){
			Approval next = (Approval) iter.next();
			oos.writeObject(next);
		}
		oos.flush();
    	String result = new String(Base64.encode(baos.toByteArray(),false));

    	
    	Collection readapprovals = ApprovalDataUtil.getApprovals(result);
    	assertTrue(readapprovals.size() == 1);
    	
    	Approval rap = (Approval) readapprovals.iterator().next();
    	assertTrue(rap.getAdminCertIssuerDN().equals(CertTools.getIssuerDN(testcert)));
    	assertTrue(rap.getAdminCertSerialNumber().equals(CertTools.getSerialNumber(testcert)));
    	assertTrue(rap.getUsername().equals("USERNAME"));
    	assertTrue(rap.isApproved());
    	assertTrue(rap.getComment().equals("test"));
    	assertTrue(rap.getApprovalDate().equals(apDate));
	}

}
