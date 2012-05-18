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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.rmi.RemoteException;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.ObjectNotFoundException;
import javax.naming.NamingException;

import org.ejbca.core.ejb.BaseSessionBean;
import org.ejbca.core.ejb.log.ILogSessionHome;
import org.ejbca.core.ejb.log.ILogSessionRemote;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.ca.AuthLoginException;
import org.ejbca.core.model.ca.AuthStatusException;
import org.ejbca.core.model.log.Admin;
import org.ejbca.core.model.log.LogConstants;
import org.ejbca.core.model.ra.UserDataVO;




/**
 * Authenticates users towards a remote user database, using HTTP-based protocol.
 * 
 * -AuthResult.java
 * -RemoteVerifyServlet.java
 * These files are a sample of a remote user database providing user authentication
 * to the CA when the CA is about to generate a certificate for a user. 
 * A remote user database is used by configuring the CA to use the 'RemoteAuthenticationSession' 
 * instead of 'LocalAuthenticationSession'. 
 * The sample files implement a simple file based user database and a servlet that
 * responds to the HTTP requests comming from the CA.
 * 
 * To install it must replace the current org.ejbca.core.model.authorization.LocalAuthorizationSessionBean
 * which will require some work from your part.
 *
 * @ejb.bean
 *   generate="false"
 *   display-name="RemoteAuthenticationSB"
 *   name="RemoteAuthenticationSession"
 * @ejb.home
 *   generate="none"
 * @ejb.interface
 *   generate="none"
 *   
 * @version $Id: RemoteAuthenticationSessionBean.java 6668 2008-11-28 16:28:44Z jeklund $
 */
public class RemoteAuthenticationSessionBean extends BaseSessionBean {
    private static String REMOTE_PROTOCOL_VER = "1.0";

    /** URL to remote authentication server */
    String remoteurl = null;

    /** The remote interface of the log session bean */
    private ILogSessionRemote logsession;



    /**
     * Default create for SessionBean without any creation Arguments.
     *
     * @throws CreateException if bean instance can't be created
     */
    public void ejbCreate() throws CreateException {
        trace(">ejbCreate()");

        // Get the URL from the environment from deployment descriptor
        remoteurl = getLocator().getString("java:comp/env/AuthURL");
        try {
            ILogSessionHome logsessionhome = (ILogSessionHome) getLocator().getLocalHome(ILogSessionHome.COMP_NAME);
            logsession = logsessionhome.create();
        } catch (Exception e) {
            throw new EJBException(e);
        }

        trace("<ejbCreate()");
    }

    /**
     * Implements IAuthenticationSession::authenticateUser. Implements a mechanism that queries a
     * remote database through a HTTP-based protocol.
     *
     * @param admin administrator performing this task
     * @param username username to be authenticated
     * @param password password for user to be authenticated
     *
     * @return UserData for authenticated user
     */
    public UserDataVO authenticateUser(Admin admin, String username, String password)
        throws ObjectNotFoundException, AuthStatusException, AuthLoginException {
    	if (log.isTraceEnabled()) {
        	log.trace(">authenticateUser(" + username + ", hiddenpwd)");
    	}
        UserDataVO ret;

        try {
            ret = getDNfromRemote(REMOTE_PROTOCOL_VER, username, password);
        } catch (Exception e) {
            error("Authentication failed.", e);
            throw new EJBException(e);
        }

        // Only end users can be authenticated on remote database (so far...)
        ret.setType(SecConst.USER_ENDUSER);
        try{
          logsession.log(admin, ret.getCAId(), LogConstants.MODULE_CA, new java.util.Date(),username, null, LogConstants.EVENT_INFO_USERAUTHENTICATION,"Autenticated user");
        }catch(RemoteException re){
           throw new EJBException(re);
        }
    	if (log.isTraceEnabled()) {
            log.trace(">authenticateUser("+username+", hiddenpwd)");
    	}
        return ret;
    } // authenticateUser

    /**
     * Implements IAuthenticationSession::finishUser. Does nothing!.
     *
     * @param admin administrator performing this task
     * @param username username to be finished
     * @param password password for user to be finished
     */
    public void finishUser(Admin admin, String username, String password)
        throws ObjectNotFoundException {
    }

    /**
     * Retieves user authentication data from a remote database using a simple HTTP-based protocol
     * TODO: explain protocol
     *
     * @param version verison of protocol
     * @param user username
     * @param password user's password
     *
     * @return strnig contining the users DN
     *
     * @exception IOException communication error
     * @exception NamingException cannot find AuthURL EJB-environment var
     */
    private UserDataVO getDNfromRemote(String version, String user, String password)
        throws NamingException, IOException {
    	if (log.isTraceEnabled()) {
        	log.trace(">getDNfromRemote(" + version + ", " + user + ", " + password + ")");
    	}
        // Connect to url and do our stuff...
        URL url = new URL(remoteurl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");

        {
            PrintWriter out = new PrintWriter(connection.getOutputStream());
            out.print("version=" + URLEncoder.encode(version,"UTF-8") + '&');
            out.print("username=" + URLEncoder.encode(user,"UTF-8") + '&');
            out.print("password=" + URLEncoder.encode(password,"UTF-8"));
            out.close();
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        if ((in.readLine().indexOf("status=200 OK") >= 0) &&
                (in.readLine().indexOf("result=grant") >= 0)) {
            String dname = "";
            String email = null;
            final String preFix = "dn-";

            while (true) {
                final String line = in.readLine();

                if (line == null) {
                    break;
                }

                line.trim();

                if (line.indexOf('=') > 0) {
                    if (line.indexOf(preFix) == 0) {
                        if (line.substring(preFix.length()).indexOf("email") == 0) {
                            email = line.substring(preFix.length() + 6);
                        } else {
                            if (dname.length() > 0) {
                                dname += ", ";
                            }

                            dname += line.substring(preFix.length());
                        }
                    } else {
                        dname += line;
                    }
                }
            }

            UserDataVO ret = new UserDataVO();
            ret.setDN(dname);
            ret.setEmail(email);
            log.trace("<getDNfromRemote");

            return ret;
        }
        log.trace("<getDNfromRemote");
        return null;
    } // getDNfromRemote
} // RemoteAuthenticationSessionBean
