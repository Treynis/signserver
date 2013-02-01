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
package org.signserver.admin.cli.defaultimpl.auditlog;

import org.cesecore.audit.impl.integrityprotected.AuditRecordData;
import org.cesecore.util.query.elems.RelationalOperator;
import org.cesecore.util.query.elems.Term;

import junit.framework.TestCase;

/**
 * Tests the query parser.
 * 
 * @author Marcus Lundblad
 * @version $Id$
 *
 */

public class QueryAuditLogTest extends TestCase {

    /**
     * Test with a valid criteria.
     * @throws Exception
     */
    public void test01ParseCriteria() throws Exception {
        final String criteria = "customId EQ 1";
        final Term term = QueryAuditLogCommand.parseCriteria(criteria);
        
        assertEquals("Operation", RelationalOperator.EQ, term.getOperator());
        assertEquals("Name", AuditRecordData.FIELD_CUSTOM_ID, term.getName());
        assertEquals("Value", "1", term.getValue());
    }
    
    /**
     * Test that a non-existing operator isn't accepted.
     * @throws Exception
     */
    public void test02ParseCriteriaInvalidOperator() throws Exception {
        final String criteria = "customId FOO 1";
        
        try {
            final Term term = QueryAuditLogCommand.parseCriteria(criteria);
            fail("Should throw an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getClass().getName());
        }
    }

    /**
     * Test that the BETWEEN operator is properly rejected.
     * @throws Exception
     */
    public void test03ParseCriteriaBetween() throws Exception {
        final String criteria = "customId BETWEEN 1";
        
        try {
            final Term term = QueryAuditLogCommand.parseCriteria(criteria);
            fail("Should throw an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getClass().getName());
        }
    }
    
    /**
     * Test that using numerical field yields a Long value (otherwise hibernate will get upset...)
     * @throws Exception
     */
    public void test04ParseCriteriaNumericValue() throws Exception {
        final String criteria = "sequenceNumber GT 1";
        final Term term = QueryAuditLogCommand.parseCriteria(criteria);
        
        assertEquals("Operation", RelationalOperator.GT, term.getOperator());
        assertEquals("Name", AuditRecordData.FIELD_SEQUENCENUMBER, term.getName());
        assertEquals("Value", Long.valueOf(1), term.getValue());
    }
    
    /**
     * Test the NULL operator.
     */
    public void test05ParseCriteriaNull() throws Exception {
        final String criteria = "searchDetail2 NULL";
        final Term term = QueryAuditLogCommand.parseCriteria(criteria);
        
        assertEquals("Operation", RelationalOperator.NULL, term.getOperator());
        assertEquals("Name", AuditRecordData.FIELD_SEARCHABLE_DETAIL2, term.getName());
        assertNull("Value", term.getValue());
    }
    
    /**
     * Test that setting a non-numeric value for a numeric field fails.
     * @throws Exception
     */
    public void test06ParseCriteriaInvalidValue() throws Exception {
        final String criteria = "timeStamp EQ foo";
        
        try {
            final Term term = QueryAuditLogCommand.parseCriteria(criteria);
            fail("Should throw a NumberFormatException");
        } catch (NumberFormatException e) {
            // expected
        } catch (Exception e) {
            fail("Unexpect exception: " + e.getClass().getName());
        }
    }
}
