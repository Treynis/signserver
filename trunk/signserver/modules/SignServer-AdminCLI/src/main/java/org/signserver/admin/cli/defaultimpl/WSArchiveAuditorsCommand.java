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
package org.signserver.admin.cli.defaultimpl;

import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;

import org.signserver.cli.spi.CommandFailureException;
import org.signserver.cli.spi.IllegalCommandArgumentsException;
import org.signserver.cli.spi.UnexpectedCommandFailureException;
import org.signserver.common.GlobalConfiguration;
import org.signserver.common.SignServerUtil;

/**
 * Command for managing the list of authorized WS auditors.
 *
 * @version $Id$
 */
public class WSArchiveAuditorsCommand extends AbstractWSClientsCommand {
    
    private static final String USAGE =
            "Usage: signserver wsarchiveauditors -add -certserialno <certificate serial number> -issuerdn <issuer DN>\n"
                + "Usage: signserver warchiveauditors -add -cert <PEM or DER file>\n"
            + "Usage: signserver warchiveauditors -remove -certserialno <certificate serial number> -issuerdn <issuer DN>\n"
            + "Usage: signserver warchiveauditors -list\n"
            + "Example 1: signserver warchiveauditors -add -certserialno 0123ABCDEF -issuerdn \"CN=Neo Morpheus, C=SE\"\n"
            + "Example 2: signserver warchiveauditors -add -cert wsauditor.pem\n"
            + "Example 3: signserver warchiveauditors -remove -certserialno 0123ABCDEF -issuerdn \"CN=Neo Morpheus, C=SE\"\n"
            + "Example 4: signserver warchiveauditors -list";

    @Override
    public String getDescription() {
        return "Manages authorizations for WS auditors";
    }

    @Override
    public String getUsages() {
        return USAGE;
    }

    @Override
    protected String getClientsProperty() {
        return "WSARCHIVEAUDITORS";
    }
}
