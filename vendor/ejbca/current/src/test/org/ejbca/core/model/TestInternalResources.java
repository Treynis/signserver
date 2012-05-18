package org.ejbca.core.model;

import junit.framework.TestCase;

public class TestInternalResources extends TestCase {

	

	protected void setUp() throws Exception {		
		super.setUp();
	}

	public void testGetLocalizedMessageString() {
		InternalResources intres = InternalResourcesTestClass.getInstance();
		String res = intres.getLocalizedMessage("raadmin.testmsg");
		assertEquals("Test ENG", res);		
		// This message will only exist in the secondary language file
		res = intres.getLocalizedMessage("raadmin.testmsgsv");
		assertEquals("Test SV", res);		
	}
	
	public void testNonExistingLocalizedMessageString() {
		InternalResources intres = InternalResourcesTestClass.getInstance();
		
		String res = intres.getLocalizedMessage("raadmin.foo");
		assertEquals("raadmin.foo", res);
	}

	public void testGetLocalizedMessageStringObject() {
		InternalResources intres = InternalResourcesTestClass.getInstance();
		String res = intres.getLocalizedMessage("raadmin.testparams",new Long(1), new Integer(3), "hi", new Boolean(true), "bye");
		assertEquals("Test 1 3 hi true bye message 1", res);
	}

	public void testGetLocalizedMessageStringObjectWithNull() {
		InternalResources intres = InternalResourcesTestClass.getInstance();
		
		String res = intres.getLocalizedMessage("raadmin.testparams",null, new Integer(3), null, new Boolean(true), "bye");		
		assertEquals("Test  3  true bye message ", res);

		res = intres.getLocalizedMessage("raadmin.testparams");		
		assertEquals("Test      message ", res);
	}
	
	public void testMessageStringWithExtraParameter() {
		InternalResources intres = InternalResourcesTestClass.getInstance();
		String res = intres.getLocalizedMessage("raadmin.testmsgsv");
		assertEquals("Test SV", res);		
		res = intres.getLocalizedMessage("raadmin.testmsgsv", "foo $bar \\haaaar");
		assertEquals("Test SV", res);		
		
	}
}
