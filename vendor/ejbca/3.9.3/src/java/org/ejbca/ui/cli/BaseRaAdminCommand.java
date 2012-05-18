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
 
package org.ejbca.ui.cli;

import org.ejbca.core.model.log.Admin;



/**
 * Base for RA commands, contains comom functions for RA operations
 *
 * @version $Id: BaseRaAdminCommand.java 5585 2008-05-01 20:55:00Z anatom $
 */
public abstract class BaseRaAdminCommand extends BaseAdminCommand {

    /**
     * Creates a new instance of BaseRaAdminCommand
     *
     * @param args command line arguments
     */
    public BaseRaAdminCommand(String[] args) {
        super(args, Admin.TYPE_RA_USER, "cli");
    }    
    
}
