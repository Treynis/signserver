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
package org.ejbca.ui.web.admin.services.servicetypes;


/**
 * Class representing an Action Type, should be registered in the 
 * ServiceTypesManager. Should be inhereted by all action managed beans.
 * 
 * @author Philip Vendil 2006 sep 29
 *
 * @version $Id: ActionType.java 5585 2008-05-01 20:55:00Z anatom $
 */
public abstract class ActionType extends ServiceType {

	public ActionType(String subViewPage, String name, boolean translatable) {
		super(subViewPage, name, translatable);
	}

}
