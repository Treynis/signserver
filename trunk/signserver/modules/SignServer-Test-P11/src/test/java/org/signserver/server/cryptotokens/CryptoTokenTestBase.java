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
package org.signserver.server.cryptotokens;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.util.encoders.Base64;
import org.cesecore.util.CertTools;
import org.cesecore.util.query.elems.LogicOperator;
import org.cesecore.util.query.elems.RelationalOperator;
import org.cesecore.util.query.elems.Term;
import static org.junit.Assert.*;
import org.signserver.common.Base64SignerCertReqData;
import org.signserver.common.CryptoTokenOfflineException;
import org.signserver.common.ICertReqData;
import org.signserver.common.ISignerCertReqInfo;
import org.signserver.common.InvalidWorkerIdException;
import org.signserver.common.OperationUnsupportedException;
import org.signserver.common.PKCS10CertReqInfo;
import org.signserver.common.SignServerException;
import org.signserver.test.utils.builders.CryptoUtils;
import org.signserver.testutils.ModulesTestCase;

/**
 * Generic CryptoToken tests. This class can be extended and the abstract
 * methods implemented to test a specific CryptoToken implementation.
 *
 * @author Markus Kilås
 * @version $Id$
 */
public abstract class CryptoTokenTestBase extends ModulesTestCase {
    
    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(CryptoTokenTestBase.class);
    
    protected abstract TokenSearchResults searchTokenEntries(final int startIndex, final int max, List<Term> queryTerms, LogicOperator queryOperator) 
            throws CryptoTokenOfflineException, KeyStoreException, InvalidWorkerIdException, SignServerException;
    
    protected abstract void generateKey(String keyType, String keySpec, String alias) throws CryptoTokenOfflineException, InvalidWorkerIdException, SignServerException;
    protected abstract boolean destroyKey(String alias) throws CryptoTokenOfflineException, InvalidWorkerIdException, SignServerException, KeyStoreException;
    
    protected abstract void importCertificateChain(List<Certificate> chain, String alias)
            throws CryptoTokenOfflineException, IllegalArgumentException,
                   CertificateException, CertificateEncodingException,
                   OperationUnsupportedException;
    
    protected abstract ICertReqData genCertificateRequest(ISignerCertReqInfo req,
                                                               boolean explicitEccParameters,
                                                               String alias)
            throws CryptoTokenOfflineException, InvalidWorkerIdException;
    
    protected abstract List<Certificate> getCertificateChain(String alias)
            throws CryptoTokenOfflineException, InvalidWorkerIdException;

//    private static final Set<String> longFields;
//    private static final Set<String> dateFields;
//    private static final Set<RelationalOperator> noArgOps;
//    private static final Set<String> allowedFields;
    
    // TODO: This could be useful in some accessible helper class
    
//    static {
//        longFields = new HashSet<String>();
//        longFields.add(AuditRecordData.FIELD_SEQUENCENUMBER);
//        
//        dateFields = new HashSet<String>();
//        dateFields.add(AuditRecordData.FIELD_TIMESTAMP);
//        
//        noArgOps = new HashSet<RelationalOperator>();
//        noArgOps.add(RelationalOperator.NULL);
//        noArgOps.add(RelationalOperator.NOTNULL);
//        
//        // allowed fields from CESeCore
//        // TODO: should maybe define this in CESeCore?
//        allowedFields = new HashSet<String>();
//        allowedFields.add(AuditRecordData.FIELD_ADDITIONAL_DETAILS);
//        allowedFields.add(AuditRecordData.FIELD_AUTHENTICATION_TOKEN);
//        allowedFields.add(AuditRecordData.FIELD_CUSTOM_ID);
//        allowedFields.add(AuditRecordData.FIELD_EVENTSTATUS);
//        allowedFields.add(AuditRecordData.FIELD_EVENTTYPE);
//        allowedFields.add(AuditRecordData.FIELD_MODULE);
//        allowedFields.add(AuditRecordData.FIELD_NODEID);
//        allowedFields.add(AuditRecordData.FIELD_SEARCHABLE_DETAIL1);
//        allowedFields.add(AuditRecordData.FIELD_SEARCHABLE_DETAIL2);
//        allowedFields.add(AuditRecordData.FIELD_SERVICE);
//        allowedFields.add(AuditRecordData.FIELD_SEQUENCENUMBER);
//        allowedFields.add(AuditRecordData.FIELD_TIMESTAMP);
//    }
    public static final String FIELD_ALIAS = "alias";
    
    
    /**
     * TODO tests...
     * 
     * Checks that the entries are returned in the same order for each call (given no entries added or removed).
     * @param existingKey
     * @throws Exception 
     */
    protected void searchTokenEntriesHelper(final String existingKey) throws Exception {
        
        final String[] testAliases = new String[] { "alias-14", "alias-13", "alias-5", "alias-10", "alias-2", "alias-1" };
        
        try {
            // First it is empty
            TokenSearchResults searchResults = searchTokenEntries(0, Integer.MAX_VALUE, Collections.<Term>emptyList(), LogicOperator.AND);
            LinkedList<String> aliases = new LinkedList<String>();
            for (TokenEntry entry : searchResults.getEntries()) {
                aliases.add(entry.getAlias());
            }
            LOG.info("Existing aliases: " + aliases);
            assertEquals("no entries except the test key yet", 1, searchResults.getEntries().size());
            assertFalse("no more entries", searchResults.isMoreEntriesAvailable());

            // Now create some entries
            for (String alias : testAliases) {
                generateKey("RSA", "1024", alias);
            }

            searchResults = searchTokenEntries(0, Integer.MAX_VALUE, Collections.<Term>emptyList(), LogicOperator.AND);
            aliases = new LinkedList<String>();
            for (TokenEntry entry : searchResults.getEntries()) {
                aliases.add(entry.getAlias());
            }
            
            // Check that all aliases are there
            for (String alias : testAliases) {
                assertTrue("should contain " + alias + " but only had " + aliases,
                        aliases.contains(alias));
            }
            assertTrue("should contain " + existingKey + " but only had " + aliases,
                        aliases.contains(existingKey));
            assertEquals("no more aliases than the expected in " + aliases,
                    testAliases.length + 1, aliases.size());
            
            final String[] allAliases = aliases.toArray(new String[0]);
            LOG.info("allAliases: " + Arrays.toString(allAliases));

            // Search 1 at the time
            searchResults = searchTokenEntries(0, 1, Collections.<Term>emptyList(), LogicOperator.AND);
            aliases = new LinkedList<String>();
            for (TokenEntry entry : searchResults.getEntries()) {
                aliases.add(entry.getAlias());
            }
            assertArrayEquals(new String[] { allAliases[0] }, aliases.toArray());
            assertTrue("more entries available", searchResults.isMoreEntriesAvailable());

            // Search 1 at the time
            searchResults = searchTokenEntries(1, 1, Collections.<Term>emptyList(), LogicOperator.AND);
            aliases = new LinkedList<String>();
            for (TokenEntry entry : searchResults.getEntries()) {
                aliases.add(entry.getAlias());
            }
            assertArrayEquals(new String[] { allAliases[1] }, aliases.toArray());
            assertTrue("more entries available", searchResults.isMoreEntriesAvailable());

            // Search 4 at the time, and then there are no more
            searchResults = searchTokenEntries(2, 5, Collections.<Term>emptyList(), LogicOperator.AND);
            aliases = new LinkedList<String>();
            for (TokenEntry entry : searchResults.getEntries()) {
                aliases.add(entry.getAlias());
            }
            assertArrayEquals(new String[] { allAliases[2], allAliases[3], allAliases[4], allAliases[5], allAliases[6] }, aliases.toArray());
            assertFalse("no more entries available", searchResults.isMoreEntriesAvailable());

            // Querying out of index returns empty results
            searchResults = searchTokenEntries(7, 1, Collections.<Term>emptyList(), LogicOperator.AND);
            aliases = new LinkedList<String>();
            for (TokenEntry entry : searchResults.getEntries()) {
                aliases.add(entry.getAlias());
            }
            assertArrayEquals(new String[] {}, aliases.toArray());
            assertFalse("no more entries available", searchResults.isMoreEntriesAvailable());
            
            // Query one specific entry
            searchResults = searchTokenEntries(0, Integer.MAX_VALUE, Arrays.asList(new Term(RelationalOperator.EQ, CryptoTokenHelper.TokenEntryFields.alias.name(), allAliases[3])), LogicOperator.AND);
            aliases = new LinkedList<String>();
            for (TokenEntry entry : searchResults.getEntries()) {
                aliases.add(entry.getAlias());
            }
            assertArrayEquals(new String[] { allAliases[3] }, aliases.toArray());
            assertFalse("no more entries available", searchResults.isMoreEntriesAvailable());
            
            // Query two specific entries
            searchResults = searchTokenEntries(0, Integer.MAX_VALUE, Arrays.asList(new Term(RelationalOperator.EQ, CryptoTokenHelper.TokenEntryFields.alias.name(), allAliases[3]), new Term(RelationalOperator.EQ, CryptoTokenHelper.TokenEntryFields.alias.name(), allAliases[1])), LogicOperator.OR);
            aliases = new LinkedList<String>();
            for (TokenEntry entry : searchResults.getEntries()) {
                aliases.add(entry.getAlias());
            }
            assertArrayEquals(new String[] { allAliases[1], allAliases[3] }, aliases.toArray());
            assertFalse("no more entries available", searchResults.isMoreEntriesAvailable());
            
            // Query all except 3 and 1
            searchResults = searchTokenEntries(0, Integer.MAX_VALUE, Arrays.asList(new Term(RelationalOperator.NEQ, CryptoTokenHelper.TokenEntryFields.alias.name(), allAliases[3]), new Term(RelationalOperator.NEQ, CryptoTokenHelper.TokenEntryFields.alias.name(), allAliases[1])), LogicOperator.AND);
            aliases = new LinkedList<String>();
            for (TokenEntry entry : searchResults.getEntries()) {
                aliases.add(entry.getAlias());
            }
            assertArrayEquals(new String[] { allAliases[0], allAliases[2], allAliases[4], allAliases[5], allAliases[6] }, aliases.toArray());
            assertFalse("no more entries available", searchResults.isMoreEntriesAvailable());
            
            // Query all except 3 and 1, only get the 4 first entries
            searchResults = searchTokenEntries(0, 4, Arrays.asList(new Term(RelationalOperator.NEQ, CryptoTokenHelper.TokenEntryFields.alias.name(), allAliases[3]), new Term(RelationalOperator.NEQ, CryptoTokenHelper.TokenEntryFields.alias.name(), allAliases[1])), LogicOperator.AND);
            aliases = new LinkedList<String>();
            for (TokenEntry entry : searchResults.getEntries()) {
                aliases.add(entry.getAlias());
            }
            assertArrayEquals(new String[] { allAliases[0], allAliases[2], allAliases[4], allAliases[5] }, aliases.toArray());
            assertTrue("more entries available", searchResults.isMoreEntriesAvailable());
            
            // Query all except 3 and 1 (same as last), but get the next one
            searchResults = searchTokenEntries(4, Integer.MAX_VALUE, Arrays.asList(new Term(RelationalOperator.NEQ, CryptoTokenHelper.TokenEntryFields.alias.name(), allAliases[3]), new Term(RelationalOperator.NEQ, CryptoTokenHelper.TokenEntryFields.alias.name(), allAliases[1])), LogicOperator.AND);
            aliases = new LinkedList<String>();
            for (TokenEntry entry : searchResults.getEntries()) {
                aliases.add(entry.getAlias());
            }
            assertArrayEquals(new String[] { allAliases[6] }, aliases.toArray());
            assertFalse("no more entries available", searchResults.isMoreEntriesAvailable());
            
        } finally {
            for (String alias : testAliases) {
                try {
                    destroyKey(alias);
                } catch (Exception ex) {
                    LOG.error("Failed to remove alias: " + alias + ": " + ex.getLocalizedMessage());
                }
            }
        }
    }
    
    protected void importCertificateChainHelper(final String existingKey) 
            throws NoSuchAlgorithmException, NoSuchProviderException,
                   OperatorCreationException, IOException, CertificateException,
                   CryptoTokenOfflineException, 
                   IllegalArgumentException, 
                   CertificateEncodingException, 
                   OperationUnsupportedException, InvalidWorkerIdException {
        final ISignerCertReqInfo req =
                new PKCS10CertReqInfo("SHA1WithRSA", "CN=imported", null);
        final Base64SignerCertReqData reqData =
                (Base64SignerCertReqData) genCertificateRequest(req, false, existingKey);
        
        // Issue certificate
        PKCS10CertificationRequest csr = new PKCS10CertificationRequest(Base64.decode(reqData.getBase64CertReq()));
        KeyPair issuerKeyPair = CryptoUtils.generateRSA(512);
        X509CertificateHolder cert = new X509v3CertificateBuilder(new X500Name("CN=Test Issuer"), BigInteger.ONE, new Date(), new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(365)), csr.getSubject(), csr.getSubjectPublicKeyInfo()).build(new JcaContentSignerBuilder("SHA256WithRSA").setProvider("BC").build(issuerKeyPair.getPrivate()));

        // import certficate chain
        importCertificateChain(Arrays.asList(CertTools.getCertfromByteArray(cert.getEncoded())), existingKey);
        
        final List<Certificate> chain = getCertificateChain(existingKey);
        
        assertEquals("Number of certs", 1, chain.size());
        
        final Certificate foundCert = chain.get(0);
        
        assertTrue("Imported cert",
                Arrays.equals(foundCert.getEncoded(), cert.getEncoded()));
    }
    
}
