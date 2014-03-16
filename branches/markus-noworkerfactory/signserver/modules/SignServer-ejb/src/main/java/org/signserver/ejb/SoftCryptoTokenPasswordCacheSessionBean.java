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
package org.signserver.ejb;


import org.signserver.ejb.interfaces.ISoftCryptoTokenPasswordCacheSession;
import java.util.Arrays;
import java.util.HashMap;
import javax.ejb.Stateless;

/**
 * Manages caching of soft crypto token passwords.
 *
 * @author Markus Kilås
 * @version $Id$
 */
@Stateless
//@Singleton
public class SoftCryptoTokenPasswordCacheSessionBean implements ISoftCryptoTokenPasswordCacheSession.ILocal {

    // Add business logic below. (Right-click in editor and choose
    // "Insert Code > Add Business Method")

    @Override
    public void cachePassword(final int workerId, final char[] password) {
        synchronized (PasswordCache.getInstance()) {
            PasswordCache.getInstance().put(workerId, password);
        }
    }

    @Override
    public char[] getCachedPassword(final int workerId) {
        synchronized (PasswordCache.getInstance()) {
            return PasswordCache.getInstance().get(workerId);
        }
    }

    @Override
    public void removeCachedPassword(final int workerId) {
        synchronized (PasswordCache.getInstance()) {
            PasswordCache.getInstance().remove(workerId);
        }
    }
    
    private static final class PasswordCache {
        private static final PasswordCache INSTANCE = new PasswordCache();
        
        private final HashMap<Integer, char[]> passwords = new HashMap<Integer, char[]>();
        
        public static PasswordCache getInstance() {
            return INSTANCE;
        }
        
        public void put(final int workerId, final char[] password) {
            char[] previous = passwords.put(workerId, password);
            if (previous != null) {
                Arrays.fill(previous, '\0');
            }
        }
        
        public char[] get(final int workerId) {
            return passwords.get(workerId);
        }
        
        public void remove(final int workerId) {
            char[] password = passwords.remove(workerId);
            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }
    }
}
