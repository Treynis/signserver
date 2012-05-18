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
 
package org.ejbca.core.protocol.ws.client;


import java.util.Iterator;
import java.util.List;

import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.ra.UserDataConstants;
import org.ejbca.core.protocol.ws.client.gen.AuthorizationDeniedException_Exception;
import org.ejbca.core.protocol.ws.client.gen.UserDataVOWS;
import org.ejbca.core.protocol.ws.client.gen.UserMatch;
//import org.ejbca.core.protocol.ws.wsclient.UserDataVOWS;
//import org.ejbca.core.protocol.ws.wsclient.UserMatch;
import org.ejbca.ui.cli.ErrorAdminCommandException;
import org.ejbca.ui.cli.IAdminCommand;
import org.ejbca.ui.cli.IllegalAdminCommandException;

/**
 * Finds a user in the database
 *
 * @version $Id: FindUserCommand.java 6424 2008-10-03 07:44:31Z anatom $
 */
public class FindUserCommand extends EJBCAWSRABaseCommand implements IAdminCommand{

	
	private static final int ARG_MATCHWITH                = 1;
	private static final int ARG_MATCHTYPE                = 2;
	private static final int ARG_MATCHVALUE               = 3;
	
	

    
    private static final String[] MATCHWITHTEXTS = {"USERNAME","EMAIL", "STATUS", "ENDENTITYPROFILE",
    	                                       "CERTIFICATEPROFILE", "CA", "TOKEN", "DN", "UID",
    	                                       "COMMONNAME","DNSERIALNUMBER","GIVENNAME","INITIALS",
    	                                       "SURNAME","TITLE","ORGANIZATIONUNIT","ORGANIZATION",
    	                                       "LOCALE","STATE","DOMAINCOMPONENT","COUNTRY"};

    private static final int[] MATCHWITHVALUES = { org.ejbca.util.query.UserMatch.MATCH_WITH_USERNAME, 
    	                                           org.ejbca.util.query.UserMatch.MATCH_WITH_EMAIL, 
    	                                           org.ejbca.util.query.UserMatch.MATCH_WITH_STATUS, 
    	                                           org.ejbca.util.query.UserMatch.MATCH_WITH_ENDENTITYPROFILE,
    	                                           org.ejbca.util.query.UserMatch.MATCH_WITH_CERTIFICATEPROFILE, 
    	                                           org.ejbca.util.query.UserMatch.MATCH_WITH_CA, 
    	                                           org.ejbca.util.query.UserMatch.MATCH_WITH_TOKEN, 
    	                                           org.ejbca.util.query.UserMatch.MATCH_WITH_DN, 
    	                                           org.ejbca.util.query.UserMatch.MATCH_WITH_UID,
    	                                           org.ejbca.util.query.UserMatch.MATCH_WITH_COMMONNAME,
    	                                           org.ejbca.util.query.UserMatch.MATCH_WITH_DNSERIALNUMBER, 
    	                                           org.ejbca.util.query.UserMatch.MATCH_WITH_GIVENNAME, 
    	                                           org.ejbca.util.query.UserMatch.MATCH_WITH_INITIALS,
    	                                           org.ejbca.util.query.UserMatch.MATCH_WITH_SURNAME,
    	                                           org.ejbca.util.query.UserMatch.MATCH_WITH_TITLE, 
    	                                           org.ejbca.util.query.UserMatch.MATCH_WITH_ORGANIZATIONUNIT, 
    	                                           org.ejbca.util.query.UserMatch.MATCH_WITH_ORGANIZATION,
    	                                           org.ejbca.util.query.UserMatch.MATCH_WITH_LOCALE,
    	                                           org.ejbca.util.query.UserMatch.MATCH_WITH_STATE,
    	                                           org.ejbca.util.query.UserMatch.MATCH_WITH_DOMAINCOMPONENT,
    	                                           org.ejbca.util.query.UserMatch.MATCH_WITH_COUNTRY};
    
    private static final String[] STATUS_TEXTS = {"NEW", "FAILED","INITIALIZED",
    	                                          "INPROCESS","GENERATED","REVOKED",
    	                                          "HISTORICAL","KEYRECOVERY"}; 
    private static final int[] STATUS_VALUES = {UserDataConstants.STATUS_NEW, UserDataConstants.STATUS_FAILED, UserDataConstants.STATUS_INITIALIZED,
    	                                        UserDataConstants.STATUS_INPROCESS, UserDataConstants.STATUS_GENERATED,  
    	                                        UserDataConstants.STATUS_REVOKED, UserDataConstants.STATUS_HISTORICAL,
    	                                        UserDataConstants.STATUS_KEYRECOVERY};
	
    /**
     * Creates a new instance of RaAddUserCommand
     *
     * @param args command line arguments
     */
    public FindUserCommand(String[] args) {
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
           
            if(args.length !=  4){
            	usage();
            	System.exit(-1);
            }
            
            int matchwith = getMatchWith(args[ARG_MATCHWITH]);
            int matchtype = getMatchType(args[ARG_MATCHTYPE], matchwith);
            String matchvalue = args[ARG_MATCHVALUE];
            
            
            try{
            	UserMatch match = new UserMatch();
            	match.setMatchtype(matchtype);
            	match.setMatchvalue(matchvalue);
            	match.setMatchwith(matchwith);
            	
            	List<UserDataVOWS> result = getEjbcaRAWS().findUser(match);
            	
            	Iterator iter = result.iterator();
            	if(result==null || result.size() == 0){
            		getPrintStream().println("No matching users could be found in database");
            	}else{
            		getPrintStream().println("The following users found in database :");
            		for(int i=0;i<result.size();i++){
        			    UserDataVOWS next = (UserDataVOWS) iter.next();
                        getPrintStream().println("\nUser : " + (i +1));
                        getPrintStream().println("  Username: "+next.getUsername());
                        getPrintStream().println("  Subject DN: "+next.getSubjectDN());
                        if(next.getSubjectAltName() == null){
                            getPrintStream().println("  Subject Altname: NONE");	
                        }else{
                            getPrintStream().println("  Subject Altname: "+next.getSubjectAltName());
                        }
                        if(next.getEmail() == null){
                        	getPrintStream().println("  Email: NONE");	
                        }else{
                        	getPrintStream().println("  Email: "+next.getEmail());
                        }                        
                        getPrintStream().println("  CA Name: "+next.getCaName());                        
                        getPrintStream().println("  Type: "+getType(next));
                        getPrintStream().println("  Token: "+next.getTokenType());
                        getPrintStream().println("  Status: "+ getStatus(next.getStatus()));
                        getPrintStream().println("  Certificate profile: "+next.getCertificateProfileName());
                        getPrintStream().println("  End entity profile: "+next.getEndEntityProfileName());
                        if(next.getHardTokenIssuerName() == null){
                        	getPrintStream().println("  Hard Token Issuer Alias: NONE");
                        }else{
                        	getPrintStream().println("  Hard Token Issuer Alias: " + next.getHardTokenIssuerName());
                        }
            		}
            	}
            	             
            }catch(AuthorizationDeniedException_Exception e){
            	getPrintStream().println("Error : " + e.getMessage());
            }           
        } catch (Exception e) {
            throw new ErrorAdminCommandException(e);
        }
    }

	private int getMatchType(String matchtype, int matchwith) {
		if(matchtype.equalsIgnoreCase("EQUALS")){
			if(matchwith >= org.ejbca.util.query.UserMatch.MATCH_WITH_UID){
				getPrintStream().println("Error: Matchtype 'EQUALS' cannot be used with DN fields, only 'BEGINSWITH' can be used.");
				usage();				
				System.exit(-1);
			}else{
				return org.ejbca.util.query.UserMatch.MATCH_TYPE_EQUALS;
			}		
		}
		if(matchtype.equalsIgnoreCase("BEGINSWITH")){
			if(matchwith > org.ejbca.util.query.UserMatch.MATCH_WITH_UID || matchwith == org.ejbca.util.query.UserMatch.MATCH_WITH_DN
					|| matchwith == org.ejbca.util.query.UserMatch.MATCH_WITH_EMAIL){
			    return org.ejbca.util.query.UserMatch.MATCH_TYPE_BEGINSWITH;
			}else{
				getPrintStream().println("Error: Matchtype 'BEGINSWITH' can only be used with DN fields, full DN and EMAIL, use EQUALS");
				usage();				
				System.exit(-1);
			}
		}		
		if(matchtype.equalsIgnoreCase("CONTAINS")){
			if ( (matchwith == org.ejbca.util.query.UserMatch.MATCH_WITH_DN) || (matchwith == org.ejbca.util.query.UserMatch.MATCH_WITH_USERNAME) ) {
			  return org.ejbca.util.query.UserMatch.MATCH_TYPE_CONTAINS;
			}else{
			  getPrintStream().println("Error: Matchtype 'CONTAINS' can only be used with matchwith 'DN' or 'USERNAME'.");
			  usage();				
			  System.exit(-1);
			}
	}		
		usage();				
		System.exit(-1);				
		return 0; // will never happen
	}

	private int getMatchWith(String matchwith) {
		for(int i=0;i< MATCHWITHTEXTS.length; i++){
			if(MATCHWITHTEXTS[i].equalsIgnoreCase(matchwith)){
				return MATCHWITHVALUES[i];
			}
		}
		usage();
		
		System.exit(-1);
		return 0; // Will never happen
	}

	private String getStatus(int status){
		for(int i=0;i<STATUS_VALUES.length;i++){
			if(STATUS_VALUES[i] == status){
				return STATUS_TEXTS[i];
			}
		}
		return "ERROR : Status text not found";
	}
	
	private int getType(UserDataVOWS userData) {
		int type = 1;
		
    	if(userData.isSendNotification())
    		type = type | SecConst.USER_SENDNOTIFICATION;
    	else
    		type = type & (~SecConst.USER_SENDNOTIFICATION);
    	
    	if(userData.isKeyRecoverable())
    		type = type | SecConst.USER_KEYRECOVERABLE;
    	else
    		type = type & (~SecConst.USER_KEYRECOVERABLE);
    			
		return type;
	}
	
	protected void usage() {
		getPrintStream().println("Command used to find userdata, maximum of 100 users will be returned ");
		getPrintStream().println("Usage : finduser <matchwith> <matchtype> <value> \n\n");
        getPrintStream().print("Matchwith can be : ");
        for(int i=0;i<MATCHWITHTEXTS.length-1;i++){
        	getPrintStream().print(MATCHWITHTEXTS[i] +", ");
        }
        getPrintStream().println(MATCHWITHTEXTS[MATCHWITHTEXTS.length-1]);
        getPrintStream().println("Matchtype can be : EQUALS (not DN fields), BEGINSWITH (DN fields), CONTAINS (matchwith complete DN or USERNAME only)");
    }


}
