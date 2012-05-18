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

/**
 * Implements the CA command line interface
 *
 * @version $Id: ca.java 7821 2009-07-22 09:39:34Z netmackan $
 */
public class ca extends BaseCommand {
    /**
     * Main
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        try {
            IAdminCommand cmd = CaAdminCommandFactory.getCommand(args);

            if (cmd != null) {
                cmd.execute();
            } else {
                System.out.println(
                    "Usage: CA info | init | listcas | getrootcert | createcrl | getcrl | listexpired | exportprofiles | importprofiles | exportca | importca | importcacert | importcvca | importcert | republish | activateca | deactivateca | changecertprofile | changecatokensignalg | removekeystore | restorekeystore");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());            
            System.exit(-1);
        }
    } // main
} // ca
