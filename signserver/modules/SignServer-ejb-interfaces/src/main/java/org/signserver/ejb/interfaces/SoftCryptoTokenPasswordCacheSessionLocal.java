/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.signserver.ejb.interfaces;

import javax.ejb.Local;

/**
 *
 * @author markus
 */
@Local
public interface SoftCryptoTokenPasswordCacheSessionLocal {

    void cachePassword(final int workerId, final char[] password);

    char[] getCachedPassword(final int workerId);

    void removeCachedPassword(final int workerId);
    
}
