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

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.util.HashMap;
import java.util.Properties;

import junit.framework.TestCase;
import org.signserver.ejb.interfaces.SoftCryptoTokenPasswordCacheSessionLocal;

/**
 * Tests for a crypto token that uses a Java Keystore (JKS) file.
 * 
 * TODO: This is a unit test consider moving from SignServer Test-System to SignServer-Server project.
 *
 * @version $Id$
 */
public class JKSCryptoTokenTest extends TestCase {

    /** Project base directory. */
    private transient File homeDir;

    @Override
    protected void setUp() throws Exception {
        final String signServerHome = System.getenv("SIGNSERVER_HOME");
        assertNotNull("Please set SIGNSERVER_HOME environment variable",
                signServerHome);
        homeDir = new File(signServerHome);
        assertTrue("SIGNSERVER_HOME nonexisting directory", homeDir.exists());
    }

    /**
     * Makes sure that the private key verifies with the public key in the
     * certificate.
     * @throws Exception
     */
    public final void testGetPrivateKeyWithRSA() throws Exception {
        signTester("SHA256WITHRSA", "/res/test/xmlsigner2.jks");
    }

    /**
     * Makes sure that the private key verifies with the public key in the
     * certificate.
     * @throws Exception
     */
    public final void testGetPrivateKeyWithDSA() throws Exception {
        signTester("SHA1WITHDSA", "/res/test/xmlsigner4.jks");
    }

    public final void signTester(final String signatureAlg, final String file)
            throws Exception {

        // Create crypto token
        final JKSCryptoToken signToken = new MockedJKSCryptoToken();
        final Properties props = new Properties();

        props.setProperty("KEYSTOREPATH",
                new File(homeDir, file).getAbsolutePath());
        signToken.init(1, props);

        // Activate
        signToken.activate("foo123");

        Signature sig;
        try {
            sig = Signature.getInstance(signatureAlg, "BC");
        } catch (NoSuchAlgorithmException e) {
            throw new SecurityException("exception creating signature", e);
        }

        sig.initSign(signToken.getPrivateKey(ICryptoToken.PURPOSE_SIGN));

        try {
            sig.update("Hello World".getBytes());
        } catch (Exception e) {
            throw new SecurityException("Error updating with string", e);
        }

        final byte[] result = sig.sign();

        sig.initVerify(signToken.getPublicKey(ICryptoToken.PURPOSE_SIGN));
        sig.update("Hello World".getBytes());
        assertTrue("verify signature", sig.verify(result));

        assertTrue("deactivate token", signToken.deactivate());
    }
    
    public static class MockedJKSCryptoToken extends JKSCryptoToken {

        private final SoftCryptoTokenPasswordCacheSessionLocal passwordCache = new SoftCryptoTokenPasswordCacheSessionLocal() {
            
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
        protected SoftCryptoTokenPasswordCacheSessionLocal getPasswordCacheSession() {
            return passwordCache;
        }
    }
}
