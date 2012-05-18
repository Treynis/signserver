package org.ejbca.core.model.approval;

import java.io.File;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.approval.approvalrequests.DummyApprovalRequest;
import org.ejbca.core.model.authorization.AdminEntity;
import org.ejbca.core.model.authorization.AdminGroup;
import org.ejbca.core.model.log.Admin;
import org.ejbca.core.model.ra.UserDataVO;
import org.ejbca.ui.cli.batch.BatchMakeP12;
import org.ejbca.util.CertTools;
import org.ejbca.util.TestTools;
import org.ejbca.util.query.ApprovalMatch;
import org.ejbca.util.query.BasicMatch;
import org.ejbca.util.query.Query;

public class TestApprovalSession extends TestCase {
    
    private static Logger log = Logger.getLogger(TestApprovalSession.class);
    
    private static String reqadminusername = null;
    private static String adminusername1 = null;
    private static String adminusername2 = null;
    
    private static X509Certificate reqadmincert = null;    
    private static X509Certificate admincert1 = null;
    private static X509Certificate admincert2 = null;

    private static Admin reqadmin = null;
    private static Admin admin1 = null;
    private static Admin admin2 = null;
    
    private static int caid = TestTools.getTestCAId();
    private static ArrayList adminentities;
    
    private static final Admin intadmin = new Admin(Admin.TYPE_INTERNALUSER);
    
	protected void setUp() throws Exception {
		super.setUp();
		CertTools.installBCProvider();
		
		adminusername1 = genRandomUserName();
		adminusername2 = adminusername1 + "2";
		reqadminusername = "req" + adminusername1;
		
		UserDataVO userdata = new UserDataVO(adminusername1,"CN="+adminusername1,caid,null,null,1,SecConst.EMPTY_ENDENTITYPROFILE,
				SecConst.CERTPROFILE_FIXED_ENDUSER,SecConst.TOKEN_SOFT_P12,0,null);
		userdata.setPassword("foo123");
		TestTools.getUserAdminSession().addUser(intadmin, userdata , true);
		
		
		UserDataVO userdata2 = new UserDataVO(adminusername2,"CN="+adminusername2,caid,null,null,1,SecConst.EMPTY_ENDENTITYPROFILE,
				SecConst.CERTPROFILE_FIXED_ENDUSER,SecConst.TOKEN_SOFT_P12,0,null);
		userdata2.setPassword("foo123");
		TestTools.getUserAdminSession().addUser(intadmin, userdata2 , true);
		
		UserDataVO userdata3 = new UserDataVO(reqadminusername,"CN="+reqadminusername,caid,null,null,1,SecConst.EMPTY_ENDENTITYPROFILE,
				SecConst.CERTPROFILE_FIXED_ENDUSER,SecConst.TOKEN_SOFT_P12,0,null);
		userdata3.setPassword("foo123");
		TestTools.getUserAdminSession().addUser(intadmin, userdata3 , true);
		
        BatchMakeP12 makep12 = new BatchMakeP12();
        File tmpfile = File.createTempFile("ejbca", "p12");

        //System.out.println("tempdir="+tmpfile.getParent());
        makep12.setMainStoreDir(tmpfile.getParent());
        makep12.createAllNew();
        

        
		adminentities = new ArrayList();
		adminentities.add(new AdminEntity(AdminEntity.WITH_COMMONNAME,AdminEntity.TYPE_EQUALCASEINS,adminusername1,caid));	
		adminentities.add(new AdminEntity(AdminEntity.WITH_COMMONNAME,AdminEntity.TYPE_EQUALCASEINS,adminusername2,caid));
		adminentities.add(new AdminEntity(AdminEntity.WITH_COMMONNAME,AdminEntity.TYPE_EQUALCASEINS,reqadminusername,caid));
		TestTools.getAuthorizationSession().addAdminEntities(intadmin, AdminGroup.TEMPSUPERADMINGROUP, adminentities);
		
		TestTools.getAuthorizationSession().forceRuleUpdate(intadmin);
		
		admincert1 = (X509Certificate) TestTools.getCertificateStoreSession().findCertificatesByUsername(intadmin, adminusername1).iterator().next();
		admincert2 = (X509Certificate) TestTools.getCertificateStoreSession().findCertificatesByUsername(intadmin, adminusername2).iterator().next();
		reqadmincert = (X509Certificate) TestTools.getCertificateStoreSession().findCertificatesByUsername(intadmin, reqadminusername).iterator().next();
		
		admin1 = new Admin(admincert1);
		admin2 = new Admin(admincert2);
		reqadmin = new Admin(reqadmincert);
	}
	
    private String genRandomUserName() throws Exception {
        // Gen random user
        Random rand = new Random(new Date().getTime() + 4711);
        String username = "";
        for (int i = 0; i < 6; i++) {
            int randint = rand.nextInt(9);
            username += (new Integer(randint)).toString();
        }
        log.debug("Generated random username: username =" + username);

        return username;
    } // genRandomUserName
	
	public void testAddApprovalRequest() throws Exception {
				
		DummyApprovalRequest nonExecutableRequest = new DummyApprovalRequest(reqadmin,null,caid,SecConst.EMPTY_ENDENTITYPROFILE,false);
		
        //		 Test that the approvalrequest doesn't exists.
		Collection result = TestTools.getApprovalSession().findApprovalDataVO(admin1, nonExecutableRequest.generateApprovalId());
		assertTrue(result.size() == 0);
				
		TestTools.getApprovalSession().addApprovalRequest(admin1, nonExecutableRequest);
		
		
		// Test that the approvalRequest exists now
		result = TestTools.getApprovalSession().findApprovalDataVO(admin1, nonExecutableRequest.generateApprovalId());
		assertTrue(result.size() == 1);		
		
		ApprovalDataVO next = (ApprovalDataVO) result.iterator().next();
		assertTrue("Status = " + next.getStatus(), next.getStatus() == ApprovalDataVO.STATUS_WAITINGFORAPPROVAL);
		assertTrue(next.getCAId() == caid);
		assertTrue(next.getEndEntityProfileiId() == SecConst.EMPTY_ENDENTITYPROFILE);
		assertTrue(next.getReqadmincertissuerdn().equals(CertTools.getIssuerDN(reqadmincert)));
		assertTrue(next.getReqadmincertsn().equals(CertTools.getSerialNumberAsString(reqadmincert)));
		assertTrue(next.getApprovalId() == nonExecutableRequest.generateApprovalId());
		assertTrue(next.getApprovalType() == nonExecutableRequest.getApprovalType());
		assertTrue(next.getApprovals().size() == 0);
		assertTrue(!next.getApprovalRequest().isExecutable());
		assertTrue(next.getRemainingApprovals() == 2);
		
		// Test that the request exipres as it should
		Thread.sleep(5000);
		result = TestTools.getApprovalSession().findApprovalDataVO(admin1, nonExecutableRequest.generateApprovalId());
		assertTrue(result.size() == 1);		
		
		next = (ApprovalDataVO) result.iterator().next();
		assertTrue("Status = " + next.getStatus(), next.getStatus() == ApprovalDataVO.STATUS_EXPIRED);
		
		
		TestTools.getApprovalSession().removeApprovalRequest(admin1, next.getId());
		
		// Test to add the same action twice
		TestTools.getApprovalSession().addApprovalRequest(admin1, nonExecutableRequest);
		try{
		  TestTools.getApprovalSession().addApprovalRequest(admin1, nonExecutableRequest);
		  fail("It shouldn't be possible to add two identical requests.");
		}catch(ApprovalException e){}  
		
		// Then after one of them have expired
		Thread.sleep(5000);
		result = TestTools.getApprovalSession().findApprovalDataVO(admin1, nonExecutableRequest.generateApprovalId());
		ApprovalDataVO expired = (ApprovalDataVO) result.iterator().next();
		
		TestTools.getApprovalSession().addApprovalRequest(admin1, nonExecutableRequest);
		
		TestTools.getApprovalSession().removeApprovalRequest(admin1, expired.getId());
		
		result = TestTools.getApprovalSession().findApprovalDataVO(admin1, nonExecutableRequest.generateApprovalId());
		next = (ApprovalDataVO) result.iterator().next();
		
		TestTools.getApprovalSession().removeApprovalRequest(admin1, next.getId());
	}

	public void testApprove() throws Exception {
		DummyApprovalRequest nonExecutableRequest = new DummyApprovalRequest(reqadmin,null,caid,SecConst.EMPTY_ENDENTITYPROFILE,false);
		TestTools.getApprovalSession().addApprovalRequest(admin1, nonExecutableRequest);
		
		Approval approval1 = new Approval("ap1test");
		TestTools.getApprovalSession().approve(admin1, nonExecutableRequest.generateApprovalId(), approval1);
		
		Collection result = TestTools.getApprovalSession().findApprovalDataVO(admin1, nonExecutableRequest.generateApprovalId());
		assertTrue(result.size() == 1);			
		
		ApprovalDataVO next = (ApprovalDataVO) result.iterator().next();
		assertTrue("Status = " + next.getStatus(), next.getStatus() == ApprovalDataVO.STATUS_WAITINGFORAPPROVAL);
		assertTrue(next.getRemainingApprovals() == 1);
		
		Approval approvalAgain = new Approval("apAgaintest");
		try{
		  TestTools.getApprovalSession().approve(admin1, nonExecutableRequest.generateApprovalId(), approvalAgain);
		  fail("The same admin shouln'tt be able to approve a request twice");
		}catch(AdminAlreadyApprovedRequestException e){}
		
		Approval approval2 = new Approval("ap2test");
		TestTools.getApprovalSession().approve(admin2, nonExecutableRequest.generateApprovalId(), approval2);
		
		result = TestTools.getApprovalSession().findApprovalDataVO(admin1, nonExecutableRequest.generateApprovalId());
		assertTrue(result.size() == 1);	
		
		next = (ApprovalDataVO) result.iterator().next();
		assertTrue("Status = " + next.getStatus(), next.getStatus() == ApprovalDataVO.STATUS_APPROVED);
		assertTrue(next.getRemainingApprovals() == 0);
		
		// Test that the approval exipres as it should
		Thread.sleep(5000);
		result = TestTools.getApprovalSession().findApprovalDataVO(admin1, nonExecutableRequest.generateApprovalId());
		assertTrue(result.size() == 1);		
		
		next = (ApprovalDataVO) result.iterator().next();
		assertTrue("Status = " + next.getStatus(), next.getStatus() == ApprovalDataVO.STATUS_EXPIRED);
		
		TestTools.getApprovalSession().removeApprovalRequest(admin1, next.getId());
		
		
		// Test using an executable Dummy, different behaviour
		DummyApprovalRequest executableRequest = new DummyApprovalRequest(reqadmin,null,caid,SecConst.EMPTY_ENDENTITYPROFILE,true);
		TestTools.getApprovalSession().addApprovalRequest(admin1, executableRequest);
										
		TestTools.getApprovalSession().approve(admin1, nonExecutableRequest.generateApprovalId(), approval1);
		TestTools.getApprovalSession().approve(admin2, nonExecutableRequest.generateApprovalId(), approval2);
		
		result = TestTools.getApprovalSession().findApprovalDataVO(admin1, executableRequest.generateApprovalId());
		assertTrue(result.size() == 1);	
		next = (ApprovalDataVO) result.iterator().next();
		assertTrue("Status = " + next.getStatus(), next.getStatus() == ApprovalDataVO.STATUS_EXECUTED);
		
		// Make sure that the approval still have status executed after exiration
		Thread.sleep(5000);
		result = TestTools.getApprovalSession().findApprovalDataVO(admin1, executableRequest.generateApprovalId());
		assertTrue(result.size() == 1);		
		
		next = (ApprovalDataVO) result.iterator().next();
		assertTrue("Status = " + next.getStatus(), next.getStatus() == ApprovalDataVO.STATUS_EXECUTED);
		
		
		TestTools.getApprovalSession().removeApprovalRequest(admin1, next.getId());
		
		// Test to request and to approve with the same admin
		nonExecutableRequest = new DummyApprovalRequest(reqadmin,null,caid,SecConst.EMPTY_ENDENTITYPROFILE,false);
		TestTools.getApprovalSession().addApprovalRequest(admin1, nonExecutableRequest);
		Approval approvalUsingReqAdmin = new Approval("approvalUsingReqAdmin");
		try{
		  TestTools.getApprovalSession().approve(reqadmin, nonExecutableRequest.generateApprovalId(), approvalUsingReqAdmin);
		  fail("Request admin shouln't be able to approve their own request");
		}catch(AdminAlreadyApprovedRequestException e){}
		result = TestTools.getApprovalSession().findApprovalDataVO(admin1, executableRequest.generateApprovalId());
		assertTrue(result.size() == 1);	
		next = (ApprovalDataVO) result.iterator().next();		
		TestTools.getApprovalSession().removeApprovalRequest(admin1, next.getId());
		
		
	}

	
	public void testReject() throws Exception {
		DummyApprovalRequest nonExecutableRequest = new DummyApprovalRequest(reqadmin,null,caid,SecConst.EMPTY_ENDENTITYPROFILE,false);
		TestTools.getApprovalSession().addApprovalRequest(reqadmin, nonExecutableRequest);
		
		Approval approval1 = new Approval("ap1test");
		TestTools.getApprovalSession().approve(admin1, nonExecutableRequest.generateApprovalId(), approval1);
		
		Collection result = TestTools.getApprovalSession().findApprovalDataVO(admin1, nonExecutableRequest.generateApprovalId());			
		ApprovalDataVO next = (ApprovalDataVO) result.iterator().next();
		assertTrue("Status = " + next.getStatus(), next.getStatus() == ApprovalDataVO.STATUS_WAITINGFORAPPROVAL);
		assertTrue(next.getRemainingApprovals() == 1);
		
		Approval rejection = new Approval("rejectiontest");
		TestTools.getApprovalSession().reject(admin2, nonExecutableRequest.generateApprovalId(), rejection);
		result = TestTools.getApprovalSession().findApprovalDataVO(admin1, nonExecutableRequest.generateApprovalId());			
		next = (ApprovalDataVO) result.iterator().next();
		assertTrue("Status = " + next.getStatus(), next.getStatus() == ApprovalDataVO.STATUS_REJECTED);
		assertTrue(next.getRemainingApprovals() == 0);
		
		TestTools.getApprovalSession().removeApprovalRequest(admin1, next.getId());
		
		nonExecutableRequest = new DummyApprovalRequest(reqadmin,null,caid,SecConst.EMPTY_ENDENTITYPROFILE,false);
		TestTools.getApprovalSession().addApprovalRequest(reqadmin, nonExecutableRequest);
		
		
		rejection = new Approval("rejectiontest2");
		TestTools.getApprovalSession().reject(admin1, nonExecutableRequest.generateApprovalId(), rejection);
		result = TestTools.getApprovalSession().findApprovalDataVO(admin1, nonExecutableRequest.generateApprovalId());			
		next = (ApprovalDataVO) result.iterator().next();
		assertTrue("Status = " + next.getStatus(), next.getStatus() == ApprovalDataVO.STATUS_REJECTED);
		assertTrue(next.getRemainingApprovals() == 0);
		
		// Try to approve a rejected request
		try{
		  TestTools.getApprovalSession().approve(admin2, nonExecutableRequest.generateApprovalId(), approval1);
		  fail("It shouldn't be possible to approve a rejected request");
		}catch(ApprovalException e){}
		
		// Test that the approval exipres as it should
		Thread.sleep(5000);
		result = TestTools.getApprovalSession().findApprovalDataVO(admin1, nonExecutableRequest.generateApprovalId());
		assertTrue(result.size() == 1);		
		
		next = (ApprovalDataVO) result.iterator().next();
		assertTrue("Status = " + next.getStatus(), next.getStatus() == ApprovalDataVO.STATUS_EXPIRED);
		
		// Try to reject an expired request
		try{
		  TestTools.getApprovalSession().reject(admin2, nonExecutableRequest.generateApprovalId(), rejection);
		  fail("It shouln't be possible to reject and expired request");
		}catch(ApprovalException e){}
		
		
		TestTools.getApprovalSession().removeApprovalRequest(admin1, next.getId());

	}
 
	public void testIsApproved() throws Exception {
		DummyApprovalRequest nonExecutableRequest = new DummyApprovalRequest(reqadmin,null,caid,SecConst.EMPTY_ENDENTITYPROFILE,false);
		TestTools.getApprovalSession().addApprovalRequest(reqadmin, nonExecutableRequest);		
		
		int status = TestTools.getApprovalSession().isApproved(reqadmin, nonExecutableRequest.generateApprovalId());
		assertTrue(status == 2);
		
		Approval approval1 = new Approval("ap1test");
		TestTools.getApprovalSession().approve(admin1, nonExecutableRequest.generateApprovalId(), approval1);
		

		status = TestTools.getApprovalSession().isApproved(reqadmin, nonExecutableRequest.generateApprovalId());
		assertTrue(status == 1);
		
		Approval approval2 = new Approval("ap2test");
		TestTools.getApprovalSession().approve(admin2, nonExecutableRequest.generateApprovalId(), approval2);
		

		status = TestTools.getApprovalSession().isApproved(reqadmin, nonExecutableRequest.generateApprovalId());
		assertTrue(status == ApprovalDataVO.STATUS_APPROVED);
				
		// Test that the approval exipres as it should
		Thread.sleep(5000);
		
		try{
		  status = TestTools.getApprovalSession().isApproved(reqadmin, nonExecutableRequest.generateApprovalId());
		  fail("A ApprovalRequestExpiredException should be thrown here");
		}catch(ApprovalRequestExpiredException e){}
		
		status = TestTools.getApprovalSession().isApproved(reqadmin, nonExecutableRequest.generateApprovalId());
		assertTrue(status == ApprovalDataVO.STATUS_EXPIREDANDNOTIFIED);
		
		Collection result = TestTools.getApprovalSession().findApprovalDataVO(admin1, nonExecutableRequest.generateApprovalId());			
		ApprovalDataVO next = (ApprovalDataVO) result.iterator().next();
		
		TestTools.getApprovalSession().removeApprovalRequest(admin1, next.getId());
		
		
	}
	
	public void testIsApprovedWithSteps() throws Exception {
		DummyApprovalRequest nonExecutableRequest = new DummyApprovalRequest(reqadmin,null,caid,SecConst.EMPTY_ENDENTITYPROFILE,3,false);
		TestTools.getApprovalSession().addApprovalRequest(reqadmin, nonExecutableRequest);		
		
		int status = TestTools.getApprovalSession().isApproved(reqadmin, nonExecutableRequest.generateApprovalId(),0);
		assertTrue(status == 2);
				
		int approvalId = nonExecutableRequest.generateApprovalId();
		Approval approval1 = new Approval("ap1test");
		TestTools.getApprovalSession().approve(admin1, approvalId, approval1);
		

		status = TestTools.getApprovalSession().isApproved(reqadmin, nonExecutableRequest.generateApprovalId(),0);
		assertTrue(status == 1);
		
		Approval approval2 = new Approval("ap2test");
		TestTools.getApprovalSession().approve(admin2, approvalId, approval2);
		

		status = TestTools.getApprovalSession().isApproved(reqadmin, approvalId,0);
		assertTrue(status == ApprovalDataVO.STATUS_APPROVED);
		
		status = TestTools.getApprovalSession().isApproved(reqadmin, approvalId,1);
		assertTrue(status == ApprovalDataVO.STATUS_APPROVED);
		
		status = TestTools.getApprovalSession().isApproved(reqadmin, approvalId,2);
		assertTrue(status == ApprovalDataVO.STATUS_APPROVED);
		
		TestTools.getApprovalSession().markAsStepDone(reqadmin, approvalId, 0);
		

		status = TestTools.getApprovalSession().isApproved(reqadmin, approvalId,0);
		assertTrue(status == ApprovalDataVO.STATUS_EXPIRED);

		
		status = TestTools.getApprovalSession().isApproved(reqadmin, approvalId,1);
		assertTrue(status == ApprovalDataVO.STATUS_APPROVED);
		
		TestTools.getApprovalSession().markAsStepDone(reqadmin, approvalId, 1);
		
		status = TestTools.getApprovalSession().isApproved(reqadmin, approvalId,0);
		assertTrue(status == ApprovalDataVO.STATUS_EXPIRED);
		
		status = TestTools.getApprovalSession().isApproved(reqadmin, approvalId,1);
		assertTrue(status == ApprovalDataVO.STATUS_EXPIRED);
		
		status = TestTools.getApprovalSession().isApproved(reqadmin, approvalId,2);
		assertTrue(status == ApprovalDataVO.STATUS_APPROVED);
		
		TestTools.getApprovalSession().markAsStepDone(reqadmin, approvalId, 2);
		
		status = TestTools.getApprovalSession().isApproved(reqadmin, approvalId,2);
		assertTrue(status == ApprovalDataVO.STATUS_EXPIRED);	  
		
						
		
		Collection result = TestTools.getApprovalSession().findApprovalDataVO(admin1, nonExecutableRequest.generateApprovalId());			
		ApprovalDataVO next = (ApprovalDataVO) result.iterator().next();
		
		TestTools.getApprovalSession().removeApprovalRequest(admin1, next.getId());
		
		
	}

	public void testFindNonExpiredApprovalRequest() throws Exception {
		DummyApprovalRequest nonExecutableRequest = new DummyApprovalRequest(reqadmin,null,caid,SecConst.EMPTY_ENDENTITYPROFILE,false);
		
		TestTools.getApprovalSession().addApprovalRequest(admin1, nonExecutableRequest);
		
		// Then after one of them have expired
		Thread.sleep(5000);
		
		TestTools.getApprovalSession().addApprovalRequest(admin1, nonExecutableRequest);
		
		ApprovalDataVO result = TestTools.getApprovalSession().findNonExpiredApprovalRequest(admin1, nonExecutableRequest.generateApprovalId());
		assertNotNull(result);
		assertTrue(result.getStatus() == ApprovalDataVO.STATUS_WAITINGFORAPPROVAL);
		

		Collection all = TestTools.getApprovalSession().findApprovalDataVO(admin1, nonExecutableRequest.generateApprovalId());
		Iterator iter = all.iterator();
		while(iter.hasNext()){
			ApprovalDataVO next = (ApprovalDataVO) iter.next();
			TestTools.getApprovalSession().removeApprovalRequest(admin1, next.getId());
		}
		
	}


	public void testQuery() throws Exception {
		
		// Add a few requests
		DummyApprovalRequest req1 = new DummyApprovalRequest(reqadmin,null,caid,SecConst.EMPTY_ENDENTITYPROFILE,false);
		DummyApprovalRequest req2 = new DummyApprovalRequest(admin1,null,caid,SecConst.EMPTY_ENDENTITYPROFILE,false);
		DummyApprovalRequest req3 = new DummyApprovalRequest(admin2,null,3,2,false);
		
		TestTools.getApprovalSession().addApprovalRequest(admin1, req1);
		TestTools.getApprovalSession().addApprovalRequest(admin1, req2);
		TestTools.getApprovalSession().addApprovalRequest(admin1, req3);
		
		// Make som queries
		Query q1 = new Query(Query.TYPE_APPROVALQUERY);
		q1.add(ApprovalMatch.MATCH_WITH_APPROVALTYPE,BasicMatch.MATCH_TYPE_EQUALS,""+req1.getApprovalType());
		
		List result = TestTools.getApprovalSession().query(admin1, q1, 0, 3);
		assertTrue("Result size " + result.size(), result.size() >= 2 && result.size() <= 3);
		
		result = TestTools.getApprovalSession().query(admin1, q1, 1, 3);
		assertTrue("Result size " + result.size(), result.size() >= 1 && result.size() <= 3);
		
		result = TestTools.getApprovalSession().query(admin1, q1, 0, 1);
		assertTrue("Result size " + result.size(), result.size() == 1);
		
		Query q2 = new Query(Query.TYPE_APPROVALQUERY);
		q2.add(ApprovalMatch.MATCH_WITH_STATUS,BasicMatch.MATCH_TYPE_EQUALS,""+ApprovalDataVO.STATUS_WAITINGFORAPPROVAL,Query.CONNECTOR_AND);
		q2.add(ApprovalMatch.MATCH_WITH_REQUESTADMINCERTSERIALNUMBER,BasicMatch.MATCH_TYPE_EQUALS,reqadmincert.getSerialNumber().toString(16));		
		
		result = TestTools.getApprovalSession().query(admin1, q1, 1, 3);
		assertTrue("Result size " + result.size(), result.size() >= 1 && result.size() <= 3);
		
		// Remove the requests
		int  id1 = ((ApprovalDataVO) TestTools.getApprovalSession().findApprovalDataVO(admin1, req1.generateApprovalId()).iterator().next()).getId();
		int  id2 = ((ApprovalDataVO) TestTools.getApprovalSession().findApprovalDataVO(admin1, req2.generateApprovalId()).iterator().next()).getId();
		int  id3 = ((ApprovalDataVO) TestTools.getApprovalSession().findApprovalDataVO(admin1, req3.generateApprovalId()).iterator().next()).getId();
		TestTools.getApprovalSession().removeApprovalRequest(admin1, id1);
		TestTools.getApprovalSession().removeApprovalRequest(admin1, id2);
		TestTools.getApprovalSession().removeApprovalRequest(admin1, id3);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		TestTools.getUserAdminSession().deleteUser(intadmin, adminusername1);
		TestTools.getUserAdminSession().deleteUser(intadmin, adminusername2);
		TestTools.getUserAdminSession().deleteUser(intadmin, reqadminusername);
		TestTools.getAuthorizationSession().removeAdminEntities(intadmin, AdminGroup.TEMPSUPERADMINGROUP, adminentities);					
	}
}
