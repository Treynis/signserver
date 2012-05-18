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

package org.ejbca.ui.web.admin.configuration;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.ejbca.core.ejb.authorization.IAuthorizationSessionLocal;
import org.ejbca.core.ejb.ra.raadmin.IRaAdminSessionLocal;
import org.ejbca.core.model.authorization.AuthorizationDeniedException;
import org.ejbca.core.model.log.Admin;
import org.ejbca.core.model.ra.raadmin.GlobalConfiguration;

/**
 * A class handling the saving and loading of global configuration data.
 * By default all data are saved to a database.
 *
 * @author  Philip Vendil
 * @version $Id: GlobalConfigurationDataHandler.java 5585 2008-05-01 20:55:00Z anatom $
 */
public class GlobalConfigurationDataHandler implements java.io.Serializable {
    
    /** Creates a new instance of GlobalConfigurationDataHandler */
    public GlobalConfigurationDataHandler(Admin administrator,IRaAdminSessionLocal raadminsession, IAuthorizationSessionLocal authorizationsession){
        this.raadminsession = raadminsession;
        this.authorizationsession = authorizationsession;
        this.administrator = administrator;
    }
    
    public GlobalConfiguration loadGlobalConfiguration() throws NamingException{
        GlobalConfiguration ret = null;
        
        ret = raadminsession.loadGlobalConfiguration(administrator);
        InitialContext ictx = new InitialContext();
        Context myenv = (Context) ictx.lookup("java:comp/env");      
        ret.initialize( (String) myenv.lookup("ADMINDIRECTORY"),
                (String) myenv.lookup("AVAILABLELANGUAGES"), (String) myenv.lookup("AVAILABLETHEMES"), 
                (String) myenv.lookup("PUBLICPORT"),(String) myenv.lookup("PRIVATEPORT"),
                (String) myenv.lookup("PUBLICPROTOCOL"),(String) myenv.lookup("PRIVATEPROTOCOL"));
        return ret;
    }
    
    public void saveGlobalConfiguration(GlobalConfiguration gc) throws AuthorizationDeniedException {
        if(this.authorizationsession.isAuthorizedNoLog(administrator, "/super_administrator"))
            raadminsession.saveGlobalConfiguration(administrator,  gc);
    }
    
    // private IRaAdminSessionHome  raadminsessionhome;
    private IRaAdminSessionLocal raadminsession;
    private IAuthorizationSessionLocal  authorizationsession;
    private Admin administrator;
}
