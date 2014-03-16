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

import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.Signature;
import java.util.HashMap;
import java.util.Properties;

import junit.framework.TestCase;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.signserver.ejb.interfaces.ISoftCryptoTokenPasswordCacheSession;

/**
 * TODO: Document me!
 * 
 * TODO: This is a unit test consider moving from SignServer Test-System to SignServer-Server project.
 * 
 * @version $Id$
 */
public class P12CryptoTokenTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Install BC Provider
        Security.addProvider(new BouncyCastleProvider());
    }

    /*
     * Test method for 'org.signserver.server.HardCodedSignToken.getPrivateKey(int)'
     * 
     * Makes sure that the private key verifies with the public key in the certificate.
     */
    public void testGetPrivateKey() throws Exception {
        Signature sig = null;
        String signatureAlgorithm = "SHA256WITHRSAANDMGF1";

        P12CryptoToken signToken = new MockedP12CryptoToken();
        Properties props = new Properties();
        String signserverhome = System.getenv("SIGNSERVER_HOME");
        assertNotNull(signserverhome);
        props.setProperty("KEYSTOREPATH", signserverhome + "/res/test/timestamp1.p12");
        signToken.init(1, props);

        signToken.activate("foo123");

        try {
            sig = Signature.getInstance(signatureAlgorithm, "BC");
        } catch (NoSuchAlgorithmException e) {
            throw new SecurityException("exception creating signature: " + e.toString());
        }


        sig.initSign(signToken.getPrivateKey(ICryptoToken.PURPOSE_SIGN));

        try {
            sig.update("Hello World".getBytes());
        } catch (Exception e) {
            throw new SecurityException("Error updating with string " + e);
        }

        byte[] result = sig.sign();

        sig.initVerify(signToken.getPublicKey(ICryptoToken.PURPOSE_SIGN));
        sig.update("Hello World".getBytes());
        assertTrue(sig.verify(result));

        assertTrue(signToken.deactivate());
    }
    
    public static class MockedP12CryptoToken extends P12CryptoToken {

        private final ISoftCryptoTokenPasswordCacheSession.ILocal passwordCache = new ISoftCryptoTokenPasswordCacheSession.ILocal() {
            
            private final HashMap<Integer, char[]> passwords = new HashMap<Integer, char[]>();

            @Override
            public void cachePassword(int workerId, char[] password) {
                passwords.put(workerId, password);
            }

            @Override
            public char[] getCachedPassword(int workerId) {
                return passwords.get(workerId);
            }

            @Override
            public void removeCachedPassword(int workerId) {
                passwords.remove(workerId);
            }
        };
        
        @Override
        protected ISoftCryptoTokenPasswordCacheSession.ILocal getPasswordCacheSession() {
            return passwordCache;
        }
    }
}
