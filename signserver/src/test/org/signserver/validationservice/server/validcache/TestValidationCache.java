/*************************************************************************
 *                                                                       *
 *  SignServer: The OpenSource Automated Signing Server                  *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/

package org.signserver.validationservice.server.validcache;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;

import org.ejbca.util.CertTools;
import org.ejbca.util.KeyTools;
import org.signserver.common.SignServerUtil;
import org.signserver.validationservice.common.Validation;

import junit.framework.TestCase;

public class TestValidationCache extends TestCase {

	private static X509Certificate cert1;
	private static X509Certificate cert2;
	private static X509Certificate cert3;

	/**
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		SignServerUtil.installBCProvider();
	
		
		KeyPair keys = KeyTools.genKeys("512", "RSA");
		cert1 = CertTools.genSelfCert("CN=cert1", 367, null, keys.getPrivate(), keys.getPublic(), "SHA1WithRSA", false);
		cert2 = CertTools.genSelfCert("CN=cert2", 367, null, keys.getPrivate(), keys.getPublic(), "SHA1WithRSA", false);
		cert3 = CertTools.genSelfCert("CN=cert3", 367, null, keys.getPrivate(), keys.getPublic(), "SHA1WithRSA", false);
	}




	/**
	 * Test method for {@link org.signserver.validationservice.server.validcache.ValidationCache}
	 * @throws InterruptedException 
	 */
	public void testValidationCache() throws InterruptedException {
		ArrayList<String> cachedIssuerDNs = new ArrayList<String>();
		cachedIssuerDNs.add(CertTools.getIssuerDN(cert1));
		cachedIssuerDNs.add(CertTools.getIssuerDN(cert2));
		ValidationCache cache = new ValidationCache(cachedIssuerDNs,2000);
		
		Validation val1 = new Validation(cert1,Validation.STATUS_VALID,"TESTMESSAGE");
		Validation val2 = new Validation(cert2,Validation.STATUS_REVOKED,"TESTMESSAGE", new Date(),3);
		Validation val3 = new Validation(cert3,Validation.STATUS_VALID,"TESTMESSAGE");
		
		// Check validation isn't cached for cert not in the list.
		cache.put(cert3, val3);
		assertTrue(cache.get(cert3) == null);
		
		cache.put(cert1, val1);
		Thread.sleep(1000);
		cache.put(cert2, val2);
		Thread.sleep(1100);
		
		assertTrue(cache.get(cert1) == null);
		Validation val = cache.get(cert2);
		assertTrue(val != null);
		assertTrue(val.getStatus().equals(Validation.STATUS_REVOKED));
		assertTrue(val.getRevokationReason() == 3);
		assertTrue(val.getRevokedDate() != null);
		Thread.sleep(1000);
		assertTrue(cache.get(cert2) == null);
		
		
	}



}
