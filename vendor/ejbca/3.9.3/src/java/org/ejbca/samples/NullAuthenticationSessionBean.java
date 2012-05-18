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

package org.ejbca.samples;

import java.rmi.RemoteException;
import java.util.ArrayList;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.ObjectNotFoundException;

import org.ejbca.core.ejb.BaseSessionBean;
import org.ejbca.core.ejb.log.ILogSessionHome;
import org.ejbca.core.ejb.log.ILogSessionRemote;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.ca.AuthLoginException;
import org.ejbca.core.model.ca.AuthStatusException;
import org.ejbca.core.model.log.Admin;
import org.ejbca.core.model.log.LogConstants;
import org.ejbca.core.model.ra.UserDataConstants;
import org.ejbca.core.model.ra.UserDataVO;
import org.ejbca.util.CertTools;




/**
 * Approves all authentication requests that contain a DN as the username, password is ignored and
 * the username is returned as DN. Could be useful for demo purposes to give out certificates to anyone.
 * 
 * To install it must replace the current org.ejbca.core.model.authorization.LocalAuthorizationSessionBean
 * which will require some work from your part.
 *
 * @ejb.bean
 *   display-name="AuthenticationSB"
 *   name="AuthenticationSession"
 *   jndi-name="AuthenticationSession"
 *   local-jndi-name="AuthenticationSessionLocal"
 *   view-type="both"
 *   type="Stateless"
 *   transaction-type="Container"
 *   generate="false"
 *
 * @ejb.transaction type="Supports"
 *
 * @ejb.ejb-external-ref
 *   description="The Log session bean"
 *   view-type="local"
 *   ejb-name="LogSessionLocal"
 *   type="Session"
 *   home="org.ejbca.core.ejb.log.ILogSessionLocalHome"
 *   business="org.ejbca.core.ejb.log.ILogSessionLocal"
 *   link="LogSession"
 * 
 * @ejb.home
 *   extends="javax.ejb.EJBHome"
 *   local-extends="javax.ejb.EJBLocalHome"
 *   local-class="org.ejbca.samples.IAuthenticationSessionLocalHome"
 *   remote-class="org.ejbca.samples.IAuthenticationSessionHome"
 *   generate="none"
 *
 * @ejb.interface
 *   extends="javax.ejb.EJBObject"
 *   local-extends="javax.ejb.EJBLocalObject"
 *   local-class="org.ejbca.samples.IAuthenticationSessionLocal"
 *   remote-class="org.ejbca.samples.IAuthenticationSessionRemote"
 *   generate="none"
 *
 *
 * @version $Id: NullAuthenticationSessionBean.java 6998 2009-02-20 15:45:19Z anatom $
 * 
 */
public class NullAuthenticationSessionBean extends BaseSessionBean {
    /** The remote interface of the log session bean */
    private ILogSessionRemote logsession;


    /**
     * Default create for SessionBean without any creation Arguments.
     *
     * @throws CreateException if bean instance can't be created
     */
    public void ejbCreate() throws CreateException {
        trace(">ejbCreate()");
        try {
            ILogSessionHome logsessionhome = (ILogSessionHome) getLocator().getLocalHome(ILogSessionHome.COMP_NAME);
            logsession = logsessionhome.create();
        } catch (Exception e) {
            throw new EJBException(e);
        }
        trace("<ejbCreate()");
    }

    /**
     * Implements IAuthenticationSession::authenticateUser. Implements a mechanism that does no
     * real authentication. Returns the username as DN is the username contains a DN. Only returns
     * entities of type USER_ENDUSER. STATUS_NEW, STATUS_FAILED or STATUS_INPROCESS.
     *
     * @param admin administrator performing this task
     * @param username username to be authenticated
     * @param password password for user to be authenticated
     *
     * @return UserData for authenticated user
     */
    public UserDataVO authenticateUser(Admin admin, String username, String password)
        throws ObjectNotFoundException, AuthStatusException, AuthLoginException {
        trace(">authenticateUser(" + username + ", hiddenpwd)");

        try {
            // Does the username contain a DN?
            String dn = CertTools.stringToBCDNString(username);

            if ((dn != null) && (dn.length() > 0)) {
            	String email = null;
                ArrayList emails = CertTools.getEmailFromDN(dn);
                if (emails.size() > 0) {
                	email = (String)emails.get(0);
                }
                try{
                  logsession.log(admin, admin.getCaId(), LogConstants.MODULE_CA, new java.util.Date(),username, null, LogConstants.EVENT_INFO_USERAUTHENTICATION,"NULL-Authenticated user");
                }catch(RemoteException re){
                  throw new EJBException(re);
                }

                String altName = (email == null) ? null : ("rfc822Name=" + email);

                // Use default certificate profile 0
                UserDataVO ret = new UserDataVO(username, dn, admin.getCaId(), altName, email, UserDataConstants.STATUS_NEW, SecConst.USER_ENDUSER, SecConst.PROFILE_NO_PROFILE, SecConst.PROFILE_NO_PROFILE, 
                		                        null, null, SecConst.TOKEN_SOFT_BROWSERGEN,0,null);
                ret.setPassword(password);
                trace("<authenticateUser("+username+", hiddenpwd)");
                return ret;
            }
            try{
              logsession.log(admin, admin.getCaId(), LogConstants.MODULE_CA, new java.util.Date(),username, null, LogConstants.EVENT_ERROR_USERAUTHENTICATION,"User does not contain a DN.");
            }catch(RemoteException re){
              throw new EJBException(re);
            }

            throw new AuthLoginException("User " + username + " does not contain a DN.");
        } catch (AuthLoginException le) {
            throw le;
        } catch (Exception e) {
            throw new EJBException(e.toString());
        }
    } //authenticateUser

    /**
     * Implements IAuthenticationSession::finishUser. Does nothing...
     *
     * @param admin administrator performing this task
     * @param username username to be finished
     * @param password password for user to be finished
     */
    public void finishUser(Admin admin, String username, String password)
        throws ObjectNotFoundException {
        trace(">finishUser(" + username + ", hiddenpwd)");
        trace("<finishUser(" + username + ", hiddenpwd)");
    } //finishUser
}
