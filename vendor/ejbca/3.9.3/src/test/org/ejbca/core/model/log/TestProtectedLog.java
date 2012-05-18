package org.ejbca.core.model.log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.bouncycastle.cms.CMSSignedData;
import org.ejbca.core.model.ca.caadmin.X509CAInfo;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.CmsCAServiceInfo;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.ExtendedCAServiceInfo;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.OCSPCAServiceInfo;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.XKMSCAServiceInfo;
import org.ejbca.core.model.ca.catoken.SoftCATokenInfo;
import org.ejbca.util.TestTools;

public class TestProtectedLog extends TestCase {

	private final static String DEFAULT_CA_NAME		= "TEST";
	private final static String LOGMESSAGE					= "Logmessage ";
	private final static String ERROR_LASTACTION		= "Last actions should not have generated an error.";
	private final static String ERROR_NONEMPTY		= "The protected log was not empty.";
	private final static String ERROR_MISSINGROW		= "Did not detect missing rows.";
	private final static String ERROR_FROZENLOG		= "Did not detect frozen log.";
	private final static String ERROR_UNPROTECTED	= "The protected log was not unprotected.";
	private final static String ERROR_NOEXPORT			= "No export file was written.";
	private final static String ERROR_BADEXPORT		= "Exported log does not contain any log-data";

	private static final Logger log = Logger.getLogger(TestProtectedLog.class);
	private final Admin internalAdmin = new Admin(Admin.TYPE_INTERNALUSER);

	/**
	 * Creates a new TestProtectedLog object.
	 *
	 * @param name name
	 */
	public TestProtectedLog(String name) {
		super(name);
        assertTrue("Could not create TestCA.", TestTools.createTestCA());
	}

	protected void setUp() throws Exception {
		log.trace(">setUp()");
		// Stop services
		TestTools.getProtectedLogSession().stopServices();
		// Clear protected log
		TestTools.getProtectedLogSession().removeAllUntil(System.currentTimeMillis());
		TestTools.getProtectedLogSession().removeAllExports(true);
		// Make sure tempfile is removed
		ProtectedLogTestAction.removeFileInTempDir();
		log.trace("<setUp()");
	}

	protected void tearDown() throws Exception {
		// Clear protected log
		TestTools.getProtectedLogSession().removeAllUntil(System.currentTimeMillis()+60*1000);
		TestTools.getProtectedLogSession().removeAllExports(true);
		// Restore log devices
		TestTools.getLogSession().restoreTestDevice();
		// Start servies
		TestTools.getProtectedLogSession().startServices();
		ProtectedLogTestAction.removeFileInTempDir();
	}

	/**
	 * Test single node
	 *  Verifies protected log when everything is ok
	 *  Fails when event is removed
	 *  Tries emergency recovery
	 */
	public void test01() throws Exception {
		// Setup a protected log device
		Properties properties = new Properties();
		properties.setProperty(ProtectedLogDevice.CONFIG_TOKENREFTYPE, ProtectedLogDevice.CONFIG_TOKENREFTYPE_CANAME);
		properties.setProperty(ProtectedLogDevice.CONFIG_TOKENREF, DEFAULT_CA_NAME);
		properties.setProperty(ProtectedLogActions.CONF_USE_TESTACTION, "true");
		properties.setProperty("searchWindow", "1");
		TestTools.getLogSession().setTestDevice(ProtectedLogDeviceFactory.class, properties);
		assertTrue(ERROR_LASTACTION, ProtectedLogTestAction.getLastActionCause() == null);
		// Write an logevent and make sure it complains about an empty log
		int messageCounter = 0;
		TestTools.getLogSession().log(internalAdmin, internalAdmin.getCaId(), LogConstants.MODULE_SERVICES, new Date(), null, null,
				LogConstants.EVENT_INFO_STARTING, LOGMESSAGE+messageCounter++, null);
		assertEquals(ERROR_NONEMPTY, IProtectedLogAction.CAUSE_EMPTY_LOG, ProtectedLogTestAction.getLastActionCause());
		// Write another and make sure there are no error message
		TestTools.getLogSession().log(internalAdmin, internalAdmin.getCaId(), LogConstants.MODULE_CUSTOM, new Date(), null, null,
				LogConstants.EVENT_INFO_UNKNOWN, LOGMESSAGE+messageCounter++, null);
		assertTrue(ERROR_LASTACTION, ProtectedLogTestAction.getLastActionCause() == null);
		// Test if log-freeze is detected
		ProtectedLogActions protectedLogActions = new ProtectedLogActions(properties);
		TestTools.getProtectedLogSession().verifyEntireLog(protectedLogActions, -1);
		assertTrue(ERROR_FROZENLOG, IProtectedLogAction.CAUSE_FROZEN.equals(ProtectedLogTestAction.getLastActionCause()));
		TestTools.getProtectedLogSession().verifyEntireLog(protectedLogActions, 3600*1000);
		assertTrue(ERROR_LASTACTION, ProtectedLogTestAction.getLastActionCause() == null);
		// Test if removed logevents are detected
		long testTime1 = System.currentTimeMillis();
		TestTools.getProtectedLogSession().removeAllUntil(testTime1);
		Thread.sleep(1100);	// Default interval to search for its own event in database is 1 second.
		TestTools.getLogSession().log(internalAdmin, internalAdmin.getCaId(), LogConstants.MODULE_CUSTOM, new Date(), null, null,
				LogConstants.EVENT_INFO_UNKNOWN, LOGMESSAGE+messageCounter++, null);
		assertEquals(ERROR_MISSINGROW, IProtectedLogAction.CAUSE_MISSING_LOGROW, ProtectedLogTestAction.getLastActionCause());
		TestTools.getProtectedLogSession().verifyEntireLog(protectedLogActions, 3600*1000);
		assertEquals(ERROR_MISSINGROW, IProtectedLogAction.CAUSE_MISSING_LOGROW, ProtectedLogTestAction.getLastActionCause());
		// Recover
		TestTools.getProtectedLogSession().resetEntireLog(false, null);
		assertTrue(ERROR_LASTACTION, ProtectedLogTestAction.getLastActionCause() == null);
		TestTools.getProtectedLogSession().verifyEntireLog(protectedLogActions, 3600*1000);
		assertTrue(ERROR_LASTACTION, ProtectedLogTestAction.getLastActionCause() == null);
	}

	/**
	 * Test startup behaviour
	 *  Start and stop node with none-token
	 *  Start node with CAToken and sign unprotected chain.
	 */
	public void test02() throws Exception {
		// Setup a protected log device
		Properties properties = new Properties();
		properties.setProperty(ProtectedLogDevice.CONFIG_TOKENREFTYPE, ProtectedLogDevice.CONFIG_TOKENREFTYPE_NONE);
		properties.setProperty(ProtectedLogActions.CONF_USE_TESTACTION, "true");
		TestTools.getLogSession().setTestDevice(ProtectedLogDeviceFactory.class, properties);
		assertTrue(ERROR_LASTACTION, ProtectedLogTestAction.getLastActionCause() == null);
		ProtectedLogActions protectedLogActions = new ProtectedLogActions(properties);
		// Write an logevent and make sure it complains about an empty log
		int messageCounter = 0;
		TestTools.getLogSession().log(internalAdmin, internalAdmin.getCaId(), LogConstants.MODULE_CUSTOM, new Date(), null, null,
				LogConstants.EVENT_INFO_UNKNOWN, LOGMESSAGE+messageCounter++, null);
		assertEquals(ERROR_NONEMPTY, IProtectedLogAction.CAUSE_EMPTY_LOG, ProtectedLogTestAction.getLastActionCause());
		TestTools.getLogSession().log(internalAdmin, internalAdmin.getCaId(), LogConstants.MODULE_LOG, new Date(), null, null,
				LogConstants.EVENT_SYSTEM_STOPPED_LOGGING , "Terminating log session for this node.",null);
		assertTrue(ERROR_LASTACTION, ProtectedLogTestAction.getLastActionCause() == null);
		// Start new chain with CAName-token
		properties.setProperty(ProtectedLogDevice.CONFIG_TOKENREFTYPE, ProtectedLogDevice.CONFIG_TOKENREFTYPE_CANAME);
		properties.setProperty(ProtectedLogDevice.CONFIG_TOKENREF, DEFAULT_CA_NAME);
		TestTools.getLogSession().setTestDevice(ProtectedLogDeviceFactory.class, properties);
		assertTrue(ERROR_LASTACTION, ProtectedLogTestAction.getLastActionCause() == null);
		TestTools.getLogSession().log(internalAdmin, internalAdmin.getCaId(), LogConstants.MODULE_CUSTOM, new Date(), null, null,
				LogConstants.EVENT_INFO_UNKNOWN, LOGMESSAGE+messageCounter++, null);
		assertEquals(ERROR_UNPROTECTED, IProtectedLogAction.CAUSE_EMPTY_LOG, ProtectedLogTestAction.getLastActionCause());
		TestTools.getProtectedLogSession().verifyEntireLog(protectedLogActions, 3600*1000);
		assertEquals(ERROR_UNPROTECTED, IProtectedLogAction.CAUSE_UNVERIFYABLE_CHAIN, ProtectedLogTestAction.getLastActionCause());
		// Sign unsigned chain so it can be linked in
		TestTools.getProtectedLogSession().signAllUnsignedChains(false);
		assertTrue(ERROR_LASTACTION, ProtectedLogTestAction.getLastActionCause() == null);
		Thread.sleep(1100);	// By default it takes 1 second between searches new events from other nodes..
		// And that event will be set 10 seconds in the future so we have to wait 10 more seconds or "cheat"
		TestTools.getLogSession().log(internalAdmin, internalAdmin.getCaId(), LogConstants.MODULE_CUSTOM, new Date(new Date().getTime()+10*1000), null, null,
				LogConstants.EVENT_INFO_UNKNOWN, LOGMESSAGE+messageCounter++, null);
		assertTrue(ERROR_LASTACTION, ProtectedLogTestAction.getLastActionCause() == null);
		TestTools.getProtectedLogSession().verifyEntireLog(protectedLogActions, 3600*1000);
		assertTrue(ERROR_LASTACTION, ProtectedLogTestAction.getLastActionCause() == null);
		// Now try to remove the first chain and see if it will be detected
		TestTools.getProtectedLogSession().removeNodeChain(TestTools.getProtectedLogSession().findOldestProtectedLogEventRow().getNodeGUID());
		assertTrue(ERROR_LASTACTION, ProtectedLogTestAction.getLastActionCause() == null);
		TestTools.getProtectedLogSession().verifyEntireLog(protectedLogActions, 3600*1000);
		assertEquals(ERROR_UNPROTECTED, IProtectedLogAction.CAUSE_MISSING_LOGROW, ProtectedLogTestAction.getLastActionCause());
	}

	/**
	 * Test export handler
	 *  Exports log with CMS export handler and verifies that content contains log.
	 *  Exports next part of log and verify that there is no overlap 
	 */
	public void test03() throws Exception {
		final String logPrefix = "uniquelogprefix_";
		boolean wasCMSDisabled = false;
		// Remove any exported file
		File dir = new File(ProtectedLogTestAction.getTempDir());
		File[] files = dir.listFiles();
		for (int i=0; i<files.length; i++) {
			if (files[i].getName().indexOf(logPrefix) != -1) {
				files[i].delete();
			}
		}
		// Setup a protected log device
		Properties properties = new Properties();
		properties.setProperty(ProtectedLogDevice.CONFIG_TOKENREFTYPE, ProtectedLogDevice.CONFIG_TOKENREFTYPE_CANAME);
		properties.setProperty(ProtectedLogDevice.CONFIG_TOKENREF, DEFAULT_CA_NAME);
		properties.setProperty(ProtectedLogActions.CONF_USE_TESTACTION, "true");
		properties.setProperty(ProtectedLogCMSExportHandler.CONF_EXPORTPATH, ProtectedLogTestAction.getTempDir() + logPrefix);
		properties.setProperty(ProtectedLogCMSExportHandler.CONF_CANAME, DEFAULT_CA_NAME);
		TestTools.getLogSession().setTestDevice(ProtectedLogDeviceFactory.class, properties);
		assertTrue(ERROR_LASTACTION, ProtectedLogTestAction.getLastActionCause() == null);
		ProtectedLogActions protectedLogActions = new ProtectedLogActions(properties);
		int messageCounter = 0;
		TestTools.getLogSession().log(internalAdmin, internalAdmin.getCaId(), LogConstants.MODULE_CUSTOM, new Date(), null, null,
				LogConstants.EVENT_INFO_UNKNOWN, LOGMESSAGE+messageCounter++, null);
		assertEquals(ERROR_NONEMPTY, IProtectedLogAction.CAUSE_EMPTY_LOG, ProtectedLogTestAction.getLastActionCause());
		// Activate CMS service
		X509CAInfo x509cainfo = (X509CAInfo) TestTools.getCAAdminSession().getCAInfo(internalAdmin, DEFAULT_CA_NAME);
		assertTrue("The test expects the CA \""+DEFAULT_CA_NAME+"\" to exist.", x509cainfo != null);
		CmsCAServiceInfo cmscainfo = null;
		Collection extendedCAServiceInfos = x509cainfo.getExtendedCAServiceInfos();
		if (extendedCAServiceInfos == null) {
			wasCMSDisabled = true;
		} else {
			Iterator iter = extendedCAServiceInfos.iterator();
			while(iter.hasNext()){
				ExtendedCAServiceInfo serviceinfo = (ExtendedCAServiceInfo) iter.next();
				if(serviceinfo instanceof CmsCAServiceInfo){
					cmscainfo = (CmsCAServiceInfo) serviceinfo;
					if (cmscainfo.getStatus() == CmsCAServiceInfo.STATUS_INACTIVE) {
						wasCMSDisabled = true;
					}
				}
			}
		}
		if (wasCMSDisabled) {
			ArrayList extendedcaserviceinfos = new ArrayList();
			extendedcaserviceinfos.add(new OCSPCAServiceInfo(OCSPCAServiceInfo.STATUS_ACTIVE, false));    
			extendedcaserviceinfos.add(new XKMSCAServiceInfo(XKMSCAServiceInfo.STATUS_ACTIVE, false));
			extendedcaserviceinfos.add(new CmsCAServiceInfo(CmsCAServiceInfo.STATUS_ACTIVE, "CN=CMSCertificate, " + x509cainfo.getSubjectDN(), "",
					((SoftCATokenInfo) x509cainfo.getCATokenInfo()).getSignKeySpec(), ((SoftCATokenInfo) x509cainfo.getCATokenInfo()).getSignKeyAlgorithm()));
			x509cainfo.setExtendedCAServiceInfos(extendedcaserviceinfos);
			TestTools.getCAAdminSession().editCA(internalAdmin, x509cainfo);
		}
		try {
			// Write an logevent and make sure it complains about an empty log
			while (messageCounter < 100) {
				TestTools.getLogSession().log(internalAdmin, internalAdmin.getCaId(), LogConstants.MODULE_CUSTOM, new Date(), null, null,
						LogConstants.EVENT_INFO_UNKNOWN, LOGMESSAGE+messageCounter++, null);
			}
			assertTrue(ERROR_LASTACTION, ProtectedLogTestAction.getLastActionCause() == null);
			// Do export
			TestTools.getProtectedLogSession().exportLog(new ProtectedLogCMSExportHandler(), properties, protectedLogActions, "SHA-256", false, 0);
			assertTrue(ERROR_LASTACTION, ProtectedLogTestAction.getLastActionCause() == null);
			// See if any file was exported
			File file = null;
			dir = new File(ProtectedLogTestAction.getTempDir());
			files = dir.listFiles();
			for (int i=0; i<files.length; i++) {
				if (files[i].getName().indexOf(logPrefix) != -1) {
					file = files[i];
					break;
				}
			}
			assertTrue(ERROR_NOEXPORT, file != null);
			CMSSignedData cmsSignedData = new CMSSignedData(new FileInputStream(file));
			file.delete();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			cmsSignedData.getSignedContent().write(baos);
			assertTrue(ERROR_BADEXPORT, baos.toString().indexOf(LOGMESSAGE+(messageCounter-1)) != -1);
			// Export another 100 rows and make sure they don't overlap
			// Write an logevent and make sure it complains about an empty log
			while (messageCounter < 100+100) {
				TestTools.getLogSession().log(internalAdmin, internalAdmin.getCaId(), LogConstants.MODULE_CUSTOM, new Date(), null, null,
						LogConstants.EVENT_INFO_UNKNOWN, LOGMESSAGE+messageCounter++, null);
			}
			assertTrue(ERROR_LASTACTION, ProtectedLogTestAction.getLastActionCause() == null);
			// Do export
			TestTools.getProtectedLogSession().exportLog(new ProtectedLogCMSExportHandler(), properties, protectedLogActions, "SHA-256", false, 0);
			assertTrue(ERROR_LASTACTION, ProtectedLogTestAction.getLastActionCause() == null);
			// See if any file was exported
			file = null;
			dir = new File(ProtectedLogTestAction.getTempDir());
			files = dir.listFiles();
			for (int i=0; i<files.length; i++) {
				if (files[i].getName().indexOf(logPrefix) != -1) {
					file = files[i];
					break;
				}
			}
			assertTrue(ERROR_NOEXPORT, file != null);
			cmsSignedData = new CMSSignedData(new FileInputStream(file));
			file.delete();
			baos = new ByteArrayOutputStream();
			cmsSignedData.getSignedContent().write(baos);
			assertTrue(ERROR_BADEXPORT, baos.toString().indexOf(LOGMESSAGE+(messageCounter-100)) != -1);
			assertTrue(ERROR_BADEXPORT, baos.toString().indexOf(LOGMESSAGE+(messageCounter-101)) == -1);
		} finally {
			// Deactivate CMS service if needed
			if (wasCMSDisabled) {
				x509cainfo = (X509CAInfo) TestTools.getCAAdminSession().getCAInfo(internalAdmin, DEFAULT_CA_NAME);
				//CmsCAServiceInfo cmscainfo = null; 
				ArrayList extendedcaserviceinfos = new ArrayList();
				extendedcaserviceinfos.add(new OCSPCAServiceInfo(OCSPCAServiceInfo.STATUS_ACTIVE, false));    
				extendedcaserviceinfos.add(new XKMSCAServiceInfo(XKMSCAServiceInfo.STATUS_ACTIVE, false)); 
				extendedcaserviceinfos.add(new CmsCAServiceInfo(CmsCAServiceInfo.STATUS_INACTIVE, "CN=CMSCertificate, " + x509cainfo.getSubjectDN(), "",
						((SoftCATokenInfo) x509cainfo.getCATokenInfo()).getSignKeySpec(), ((SoftCATokenInfo) x509cainfo.getCATokenInfo()).getSignKeyAlgorithm()));
				x509cainfo.setExtendedCAServiceInfos(extendedcaserviceinfos);
				TestTools.getCAAdminSession().editCA(internalAdmin, x509cainfo);
			}
		}
	}
	
	/**
	 * Test that a log-events isn't rolled back when the surrounding transaction is.
	 * 
	 * @throws Exception
	 */
	public void test04() throws Exception {
		long now = System.currentTimeMillis();
		// Setup a protected log device
		Properties properties = new Properties();
		properties.setProperty(ProtectedLogDevice.CONFIG_TOKENREFTYPE, ProtectedLogDevice.CONFIG_TOKENREFTYPE_NONE);
		properties.setProperty(ProtectedLogActions.CONF_USE_TESTACTION, "true");
		TestTools.getLogSession().setTestDevice(ProtectedLogDeviceFactory.class, properties);
		TestTools.getLogSession().setTestDeviceOnLogSession(ProtectedLogDeviceFactory.class, properties);
		assertTrue(ERROR_LASTACTION, ProtectedLogTestAction.getLastActionCause() == null);
		try {
			TestTools.getLogSession().testRollbackInternal(now);
		} catch (Exception e) {
		}
		// TODO: The following test fails on Hypersonic the second time for some reason...
		assertEquals(ERROR_NONEMPTY, IProtectedLogAction.CAUSE_EMPTY_LOG, ProtectedLogTestAction.getLastActionCause());
		// Verify that event written at time "now" still exists
		assertTrue("The log event has been rolled back and cannot be found any more..", TestTools.getProtectedLogSession().existsAnyProtectedLogEventByTime(now));
		TestTools.getLogSession().restoreTestDeviceOnLogSession();
	}
	
	public void test99RemoveTestCA() throws Exception {
		TestTools.removeTestCA();
	}
}
