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
 
package org.ejbca.core.model.ca.caadmin.extendedcaservices;

import java.io.Serializable;



/**
 * Should be enherited by all ExtendedCAServiceResonse Value objects.  
 *
 * @version $Id: ExtendedCAServiceResponse.java 5585 2008-05-01 20:55:00Z anatom $
 */
public abstract class ExtendedCAServiceResponse  implements Serializable {    
       
    public ExtendedCAServiceResponse(){}    

}
