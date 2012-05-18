/*************************************************************************
 *                                                                       *
 *  EJBCA: The OpenSource Certificate Authority                          *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.ejbca.core.model.services.workers;

import java.util.Collection;

import javax.ejb.CreateException;

import org.apache.log4j.Logger;
import org.ejbca.core.ejb.ca.crl.ICreateCRLSessionLocal;
import org.ejbca.core.ejb.ca.crl.ICreateCRLSessionLocalHome;
import org.ejbca.core.model.InternalResources;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.services.BaseWorker;
import org.ejbca.core.model.services.ServiceExecutionFailedException;

/**
 * Class managing the updating of CRLs.
 * 
 * This is a replacement of the old jboss service.
 * 
 * @author Philip Vendil
 * @version $Id: CRLUpdateWorker.java 7781 2009-07-02 00:50:15Z anatom $
 */
public class CRLUpdateWorker extends BaseWorker {

    private static final Logger log = Logger.getLogger(CRLUpdateWorker.class);	
    /** Internal localization of logs and errors */
    private static final InternalResources intres = InternalResources.getInstance();

    private ICreateCRLSessionLocal createcrlsession = null;

	private static boolean running = false;

	/**
	 * Checks if there are any CRL that needs to be updated, and then does the creation.
	 * 
	 * @see org.ejbca.core.model.services.IWorker#work()
	 */
	public void work() throws ServiceExecutionFailedException {
		// A semaphore used to not run parallel CRL generation jobs if it is slow
		// in generating CRLs, and this job runs very often
		if (!running) {
			try {
				running = true;
			    long polltime = getNextInterval();
			    ICreateCRLSessionLocal session = getCreateCRLSession();
			    if (session != null) {
				    Collection caids = getCAIdsToCheck(true); 
			    	session.createCRLs(getAdmin(), caids, polltime*1000);
			    	session.createDeltaCRLs(getAdmin(), caids, polltime*1000);
			    }			
			} finally {
				running = false;
			}			
		} else {
    		String msg = intres.getLocalizedMessage("services.alreadyrunninginvm", CRLUpdateWorker.class.getName());            	
			log.info(msg);
		}
	}

	
	public ICreateCRLSessionLocal getCreateCRLSession(){
		if(createcrlsession == null){
			try {
	            ICreateCRLSessionLocalHome home = (ICreateCRLSessionLocalHome) getLocator().getLocalHome(ICreateCRLSessionLocalHome.COMP_NAME);
				this.createcrlsession = home.create();
			} catch (CreateException e) {
				log.error(e);
			}
		}
  
		return createcrlsession;
	}
}
