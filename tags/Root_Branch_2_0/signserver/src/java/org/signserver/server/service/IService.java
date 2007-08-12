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

package org.signserver.server.service;

import org.signserver.server.IWorker;
import org.signserver.server.ServiceExecutionFailedException;


/**
 * IService is an interface that all services should implement
 * 
 * There exists a BaseService that can be extended covering some of it's functions
 * 
 * 
 * @author Philip Vendil
 * $Id: IService.java,v 1.1 2007-02-27 16:18:28 herrvendil Exp $
 */
public interface IService extends IWorker{
	 
	/**
	 * Constant indicating if the service should stop executing
	 */
	public static final long DONT_EXECUTE = -1;
	
	
	
	/**
	 * Method that should do the actual work and should
	 * be implemented by all services. The method is run
	 * at a periodical interval defined in getNextInterval.
	 * 
	 * @throws ServiceExecutionFailedException if execution of a service failed
	 */
	public void work() throws ServiceExecutionFailedException;
	
	/**
	 * @return should return the seconds to next time the service should
	 * execute, or -1 (DONE_EXECUTE) if the service should stop executing.
	 */
	public long getNextInterval();
	
	/**
	 * @return true if the service should be active and run
	 */
	public boolean isActive();
	
	/**
	 * @return true if it should be a singleton only runned at one node at
	 * the time, of false if it should be runned on all nodes simultainously.
	 */
	public boolean isSingleton();

}
