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

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import javax.ejb.FinderException;

import org.ejbca.core.ejb.ca.store.CertificateDataBean;
import org.ejbca.core.ejb.hardtoken.IHardTokenSessionRemote;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.authorization.AuthorizationDeniedException;
import org.ejbca.core.model.log.Admin;
import org.ejbca.core.model.ra.raadmin.GlobalConfiguration;
import org.ejbca.core.model.ra.raadmin.UserDoesntFullfillEndEntityProfile;

/**
 * Adds a user to the database.
 *
 * @version $Id: RaAddUserCommand.java 7777 2009-07-01 09:08:56Z anatom $
 */
public class RaAddUserCommand extends BaseRaAdminCommand {
	
	private static final String USERGENERATED = "USERGENERATED"; 
	private static final String P12           = "P12";
	private static final String JKS           = "JKS";
	private static final String PEM           = "PEM";
	
	private final String[] softtokennames = {USERGENERATED,P12,JKS,PEM};
	private final int[] softtokenids = {SecConst.TOKEN_SOFT_BROWSERGEN,
			SecConst.TOKEN_SOFT_P12, SecConst.TOKEN_SOFT_JKS, SecConst.TOKEN_SOFT_PEM};
	
    /**
     * Creates a new instance of RaAddUserCommand
     *
     * @param args command line arguments
     */
    public RaAddUserCommand(String[] args) {
        super(args);
    }

    /**
     * Runs the command
     *
     * @throws IllegalAdminCommandException Error in command args
     * @throws ErrorAdminCommandException Error running command
     */
    public void execute() throws IllegalAdminCommandException, ErrorAdminCommandException {
        try {
            GlobalConfiguration globalconfiguration = getRaAdminSession().loadGlobalConfiguration(administrator);
            boolean usehardtokens = globalconfiguration.getIssueHardwareTokens();
            boolean usekeyrecovery = globalconfiguration.getEnableKeyRecovery();
            String[] hardtokenissueraliases = null;
            Collection authorizedhardtokenprofiles   = null;
            HashMap hardtokenprofileidtonamemap = null;            

            if(usehardtokens){  
              hardtokenissueraliases = (String[]) getHardTokenSession().getHardTokenIssuerAliases(administrator).toArray(new String[0]);             

              authorizedhardtokenprofiles = getHardTokenSession().getAuthorizedHardTokenProfileIds(administrator);
              hardtokenprofileidtonamemap = getHardTokenSession().getHardTokenProfileIdToNameMap(administrator);
            }  

            String types = "Type (mask): INVALID=0; END-USER=1; SENDNOTIFICATION=256; PRINTUSERDATA=512"; 
            if (usekeyrecovery) {
                types = "Type (mask): INVALID=0; END-USER=1; KEYRECOVERABLE=128; SENDNOTIFICATION=256; PRINTUSERDATA=512";
            } 

            if ( (args.length < 9) || (args.length > 12) ) {
                Collection certprofileids = getCertificateStoreSession().getAuthorizedCertificateProfileIds(administrator, CertificateDataBean.CERTTYPE_ENDENTITY);
                HashMap certificateprofileidtonamemap = getCertificateStoreSession().getCertificateProfileIdToNameMap(administrator);
                
                Collection endentityprofileids =  getRaAdminSession().getAuthorizedEndEntityProfileIds(administrator);
                HashMap endentityprofileidtonamemap = getRaAdminSession().getEndEntityProfileIdToNameMap(administrator);
                
                Collection caids = getAuthorizationSession().getAuthorizedCAIds(administrator);
                HashMap caidtonamemap = getCAAdminSession().getCAIdToNameMap(administrator);
                
                if( usehardtokens) {
                  getOutputStream().println("Usage: RA adduser <username> <password> <dn> <subjectAltName> <caname> <email> <type> <token> [<certificateprofile>]  [<endentityprofile>] [<hardtokenissuer>]");
                } else {
                  getOutputStream().println("Usage: RA adduser <username> <password> <dn> <subjectAltName> <caname> <email> <type> <token> [<certificateprofile>]  [<endentityprofile>] ");
                }


                getOutputStream().println("");
                getOutputStream().println("DN is of form \"C=SE, O=MyOrg, OU=MyOrgUnit, CN=MyName\" etc.");
                getOutputStream().println(
                    "SubjectAltName is of form \"rfc822Name=<email>, dNSName=<host name>, uri=<http://host.com/>, ipaddress=<address>, upn=<MS UPN>, guid=<MS globally unique id>, directoryName=<LDAP escaped DN>, krb5principal=<Krb5 principal name>\"");
                getOutputStream().println("An LDAP escaped DN is for example:");
                getOutputStream().println("DN: CN=Tomas Gustavsson, O=PrimeKey Solutions, C=SE");
                getOutputStream().println("LDAP escaped DN: CN=Tomas Gustavsson\\, O=PrimeKey Solutions\\, C=SE");

                getOutputStream().println(types);

                getOutputStream().print("Existing tokens      : " + USERGENERATED + ", " +
                                          P12 + ", "+ JKS + ", "  + PEM);

                if (usehardtokens) {
                  Iterator iter = authorizedhardtokenprofiles.iterator();
                  while(iter.hasNext()){
                    getOutputStream().print(", " + hardtokenprofileidtonamemap.get(iter.next()));
                  }
                }

                getOutputStream().print("\n");
                
                
                getOutputStream().print("Existing cas  : ");
                boolean first = true;
                Iterator iter = caids.iterator();
                while(iter.hasNext()){
                  if(first) {                    
                    first= false;
                  } else {
                    getOutputStream().print(", ");
                  }
                  getOutputStream().print(caidtonamemap.get(iter.next()));
                }
                getOutputStream().print("\n");
                
                getOutputStream().print("Existing certificate profiles  : ");
                first = true;
                iter = certprofileids.iterator();
                while(iter.hasNext()){
                  if(first) {                    
                    first= false;
                  } else {
                    getOutputStream().print(", ");
                  }
                  getOutputStream().print(certificateprofileidtonamemap.get(iter.next()));
                }
                getOutputStream().print("\n");


                getOutputStream().print("Existing endentity profiles  : ");
                first = true;
                iter = endentityprofileids.iterator();
                while(iter.hasNext()){
                  if(first) {                    
                    first= false;
                  } else {
                    getOutputStream().print(", ");
                  }
                  getOutputStream().print(endentityprofileidtonamemap.get(iter.next()));
                }
                
                getOutputStream().print("\n");
                if( usehardtokens && hardtokenissueraliases.length > 0){                
                  getOutputStream().print("Existing hardtoken issuers  : ");
                  for(int i=0; i < hardtokenissueraliases.length-1; i++){
                    getOutputStream().print(hardtokenissueraliases[i] + ", ");
                  }
                  getOutputStream().print(hardtokenissueraliases[hardtokenissueraliases.length-1] + "\n");               
                }

                getOutputStream().println(
                    "If the user does not have a SubjectAltName or an email address,\n or you want the password to be auto-generated use the value 'null'. ");
                return;
            }

            String username = args[1];
            String password = args[2];
            String dn = args[3];
            String subjectaltname = args[4];
            String caname  = args[5];
            String email = args[6];
            int type  = 1;
            try {
            	type = Integer.parseInt(args[7]);
            } catch (NumberFormatException e) {
                throw new NumberFormatException("Invalid type, '"+args[7]+"'.\n"+types);
            }
            String tokenname = args[8];
            int profileid =  SecConst.EMPTY_ENDENTITYPROFILE;
            int certificatetypeid = SecConst.CERTPROFILE_FIXED_ENDUSER;
            int hardtokenissuerid = SecConst.NO_HARDTOKENISSUER;
            boolean error = false;
            boolean usehardtokenissuer = false;

            int caid = 0;
            try{
              caid = getCAAdminSession().getCAInfo(administrator, caname).getCAId();
            }catch(Exception e){               
            }
            
            if(args.length > 9){
              // Use certificate type, no end entity profile.
              certificatetypeid = getCertificateStoreSession().getCertificateProfileId(administrator, args[9]);
              getOutputStream().println("Using certificate profile: "+args[9]+", with id: "+certificatetypeid);
            }

            if(args.length > 10){
              // Use certificate type and end entity profile.
              profileid = getRaAdminSession().getEndEntityProfileId(administrator, args[10]);
              getOutputStream().println("Using entity profile: "+args[10]+", with id: "+profileid);
            }

            if(args.length == 12 && usehardtokens){
              // Use certificate type, end entity profile and hardtokenisseur.
              hardtokenissuerid = getHardTokenSession().getHardTokenIssuerId(administrator,args[11]);
              usehardtokenissuer = true;
              getOutputStream().println("Using hard token issuer: "+args[11]+", with id: "+hardtokenissuerid);
            }
            
            int tokenid =getTokenId(administrator, tokenname, usehardtokens, getHardTokenSession());
            if (tokenid == 0) {
                getOutputStream().println("Error : Invalid token id.");
                error = true;
            }

            if (certificatetypeid == SecConst.PROFILE_NO_PROFILE) { // Certificate profile not found i database.
                getOutputStream().println("Error : Couldn't find certificate profile in database.");
                error = true;
            }

            if(profileid == 0){ // End entity profile not found i database.
              getOutputStream().println("Error : Couldn't find end entity profile in database." );
              error = true;
            }
            
            if(caid == 0){ // CA not found i database.
              getOutputStream().println("Error : Couldn't find CA in database." );
              error = true;
            }
            
            if(usehardtokenissuer && hardtokenissuerid == SecConst.NO_HARDTOKENISSUER){
              getOutputStream().println("Error : Couldn't find hard token issuer in database." );
              error = true;       
            }  

            if ((tokenid > SecConst.TOKEN_SOFT) &&
                    (hardtokenissuerid == SecConst.NO_HARDTOKENISSUER)) {
                getOutputStream().println(
                    "Error : HardTokenIssuer has to be choosen when user with hard tokens is added.");
                error = true;
            }

            if (email.equalsIgnoreCase("NULL") &&
                    ((type & SecConst.USER_SENDNOTIFICATION) == SecConst.USER_SENDNOTIFICATION)) {
                getOutputStream().println(
                    "Error : Email field cannot be null when send notification type is given.");
                error = true;
            }

            // Check if username already exists.
            try {
                if (getUserAdminSession().findUser(administrator, username) != null) {
                    getOutputStream().println("Error : User already exists in the database.");
                    error = true;
                }
            } catch (FinderException e) {
            }


            if(!error){
              getOutputStream().println("Trying to add user:");
              getOutputStream().println("Username: "+username);
              getOutputStream().println("Password (hashed only): "+password);
              getOutputStream().println("DN: "+dn);
              getOutputStream().println("CA Name: "+caname);
              getOutputStream().println("SubjectAltName: "+subjectaltname);
              getOutputStream().println("Email: "+email);
              getOutputStream().println("Type: "+type);
              getOutputStream().println("Token: "+tokenname);
              getOutputStream().println("Certificate profile: "+certificatetypeid);
              getOutputStream().println("End entity profile: "+profileid);
			  if (password.toUpperCase().equals("NULL")) {
				  password = null;
			  }
              if (subjectaltname.toUpperCase().equals("NULL")) {
                  subjectaltname = null;
              }
              if (email.toUpperCase().equals("NULL")) {
                  email = null;
              }
              try{
                getUserAdminSession().addUser(administrator, username, password, dn, subjectaltname, email, false, profileid, certificatetypeid,
                                         type, tokenid, hardtokenissuerid, caid);
                getOutputStream().println("User '"+username+"' has been added.");
                getOutputStream().println();
                getOutputStream().println("Note: If batch processing should be possible, \nalso use 'ra setclearpwd "+username+" <pwd>'.");
              }catch(AuthorizationDeniedException e){
                  getOutputStream().println("Error : " + e.getMessage());
              }catch(UserDoesntFullfillEndEntityProfile e){
                 getOutputStream().println("Error : Given userdata doesn't fullfill end entity profile. : " +  e.getMessage());
              }
            }
        } catch (Exception e) {
            throw new ErrorAdminCommandException(e);
        }
    }

    // execute
    /**
     *  Returns the tokenid type of the user, returns 0 if invalid tokenname.    
     */
    
    private int getTokenId(Admin administrator, String tokenname, boolean usehardtokens, IHardTokenSessionRemote hardtokensession) throws RemoteException {
        int returnval = 0;
        
        // First check for soft token type
        for(int i=0;i< softtokennames.length;i++){
        	if(softtokennames[i].equals(tokenname)){
        		returnval = softtokenids[i];
        		break;
        	}        	
        }

        if (returnval == 0 && usehardtokens) {
             returnval = hardtokensession.getHardTokenProfileId(administrator , tokenname);
        }

        return returnval;
    }
}
