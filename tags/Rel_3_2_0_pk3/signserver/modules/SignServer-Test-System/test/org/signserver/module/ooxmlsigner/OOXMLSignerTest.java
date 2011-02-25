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

package org.signserver.module.ooxmlsigner;

import java.io.File;
import java.io.FileOutputStream;
import java.security.cert.Certificate;

import junit.framework.TestCase;

import org.ejbca.util.Base64;
import org.signserver.cli.CommonAdminInterface;
import org.signserver.common.GenericSignRequest;
import org.signserver.common.GenericSignResponse;
import org.signserver.common.RequestContext;
import org.signserver.common.SignServerUtil;
import org.signserver.common.SignerStatus;
import org.signserver.common.ServiceLocator;
import org.signserver.common.clusterclassloader.MARFileParser;
import org.signserver.ejb.interfaces.IWorkerSession;
import org.signserver.testutils.TestUtils;
import org.signserver.testutils.TestingSecurityManager;

/**
 * 
 * Test for ooxmlsigner. Worker ID of 5677 is hard coded here and used from module-configs/ooxmlsigner/junittest-part-config.properties
 * 
 * Test case : signs docx file with certificate defined in module-configs/ooxmlsigner/junittest-part-config.properties
 *  
 * @author Aziz G�ktepe
 * @version $Id: TestOOXMLSigner.java 550 2009-08-12 11:56:16Z rayback_2 $
 */
public class OOXMLSignerTest extends TestCase {

	/**
	 * WORKERID used in this test case as defined in
	 * junittest-part-config.properties
	 */
	private static final int WORKERID = 5677;

	private static IWorkerSession.IRemote sSSession = null;
	private static String signserverhome;
	private static int moduleVersion;

	public static void main(String[] args)
	{
		try
		{
		OOXMLSignerTest den = new OOXMLSignerTest();
		den.setUp();
		den.test00SetupDatabase();
		den.test01SignDocx();
		den.test02GetStatus();
		den.test99TearDownDatabase();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	protected void setUp() throws Exception {
		super.setUp();
		SignServerUtil.installBCProvider();
		sSSession = ServiceLocator.getInstance().lookupRemote(IWorkerSession.IRemote.class);
		TestUtils.redirectToTempOut();
		TestUtils.redirectToTempErr();
		TestingSecurityManager.install();
		signserverhome = System.getenv("SIGNSERVER_HOME");
		assertNotNull("Please set SIGNSERVER_HOME environment variable", signserverhome);
		CommonAdminInterface.BUILDMODE = "SIGNSERVER";
		
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		TestingSecurityManager.remove();
	}	
	
	public void test00SetupDatabase() throws Exception {

		MARFileParser marFileParser = new MARFileParser(signserverhome + "/dist-server/ooxmlsigner.mar");
		moduleVersion = marFileParser.getVersionFromMARFile();

		TestUtils.assertSuccessfulExecution(new String[] { "module", "add", signserverhome + "/dist-server/ooxmlsigner.mar", "junittest" });
		assertTrue(TestUtils.grepTempOut("Loading module OOXMLSIGNER"));
		assertTrue(TestUtils.grepTempOut("Module loaded successfully."));

		sSSession.reloadConfiguration(WORKERID);
	}

	public void test01SignDocx() throws Exception {

		int reqid = 13;

		GenericSignRequest signRequest = new GenericSignRequest(reqid, Base64.decode(testDocx.getBytes()));

		GenericSignResponse res = (GenericSignResponse) sSSession.process(WORKERID, signRequest, new RequestContext());
		byte[] data = res.getProcessedData();
		
		// Answer to right question
		assertTrue(reqid == res.getRequestID());
		
		// Output for manual inspection
		File file = new File(signserverhome + File.separator + "tmp" + File.separator + "signedTestDoc.docx");
		FileOutputStream fos = new FileOutputStream(file);
		fos.write((byte[]) data);
		fos.close();

		
		//TODO : validate signed document by calling ooxmlvalidator (first of course code one)
		
		// Check certificate
		Certificate signercert = res.getSignerCertificate();
		assertNotNull(signercert);

	}

	public void test02GetStatus() throws Exception {
		SignerStatus stat = (SignerStatus) sSSession.getStatus(WORKERID);
		assertTrue(stat.getTokenStatus() == SignerStatus.STATUS_ACTIVE);
	}

	public void test99TearDownDatabase() throws Exception {
		TestUtils.assertSuccessfulExecution(new String[] { "removeworker", ""+WORKERID });

		TestUtils.assertSuccessfulExecution(new String[] { "module", "remove", "ooxmlsigner", "" + moduleVersion });
		assertTrue(TestUtils.grepTempOut("Removal of module successful."));
		sSSession.reloadConfiguration(WORKERID);
	}

	/**
	 * predefined docx file in base64 format.
	 */
	private static final String testDocx = "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";
	
}

