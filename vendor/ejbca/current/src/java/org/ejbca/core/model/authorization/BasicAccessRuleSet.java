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
 
package org.ejbca.core.model.authorization;

/**
 * A class containing constats used when configuring Basic Access Rule Set 
 *
 * @author  herrvendil 
 * @version $Id: BasicAccessRuleSet.java 5585 2008-05-01 20:55:00Z anatom $
 */
public class BasicAccessRuleSet implements java.io.Serializable {

	public static final int ROLE_NONE                  = 0;
    public static final int ROLE_SUPERADMINISTRATOR    = 1;
    public static final int ROLE_CAADMINISTRATOR       = 2;
    public static final int ROLE_RAADMINISTRATOR       = 3;
    public static final int ROLE_SUPERVISOR            = 4;
    public static final int ROLE_HARDTOKENISSUER       = 5;

    public static final int ENDENTITY_VIEW             = 2;
    public static final int ENDENTITY_VIEWHISTORY      = 4;
    public static final int ENDENTITY_VIEWHARDTOKENS   = 8;    
    public static final int ENDENTITY_CREATE           = 16;    
    public static final int ENDENTITY_EDIT             = 32;
    public static final int ENDENTITY_DELETE           = 64;
    public static final int ENDENTITY_REVOKE           = 128;
    public static final int ENDENTITY_KEYRECOVER       = 256;
    public static final int ENDENTITY_APPROVE          = 512;
    public static final int ENDENTITY_VIEWPUK          = 1024;
    
    public static final int ENDENTITYPROFILE_ALL  = 0;
    
    public static final int CA_ALL  = 0;
    
    public static final int OTHER_VIEWLOG = 1;
    public static final int OTHER_ISSUEHARDTOKENS = 2;
    
    public static final String[]  ROLETEXTS = {"NONE","SUPERADMINISTRATOR","CAADMINISTRATOR",
    		                                                         "RAADMINISTRATOR", "SUPERVISOR",
                                                                     "HARDTOKENISSUER"};
    
    public static final String[]  ENDENTITYRULETEXTS =  {"VIEWENDENTITYRULE","VIEWHISTORYRULE","VIEWHARDTOKENRULE","VIEWPUKENDENTITYRULE",
    	                                                                                  "CREATEENDENTITYRULE","EDITENDENTITYRULE","DELETEENDENTITYRULE",
																						  "REVOKEENDENTITYRULE", "KEYRECOVERENDENTITYRULE",
																						  "APPROVEENDENTITYRULE"};
    		
    public static final String[]  OTHERTEXTS = {"","VIEWLOG","ISSUEHARDTOKENS"};
        
   /**
     * This class should not be able to be instantiated.
     */
    private BasicAccessRuleSet(){}
    
    public static String getEndEntityRuleText(int endentityrule){
    	String returnval = "";
    	
    	switch(endentityrule){
    	   case BasicAccessRuleSet.ENDENTITY_VIEW:
    	   	  returnval = ENDENTITYRULETEXTS[0];
    	   	  break;
    	   case BasicAccessRuleSet.ENDENTITY_VIEWHISTORY:
    	   	  returnval = ENDENTITYRULETEXTS[1];
    	   	  break;
    	   case BasicAccessRuleSet.ENDENTITY_VIEWHARDTOKENS:
    	      returnval = ENDENTITYRULETEXTS[2];
    	      break;
    	   case BasicAccessRuleSet.ENDENTITY_VIEWPUK:
     	      returnval = ENDENTITYRULETEXTS[3];
     	      break;
    	   case BasicAccessRuleSet.ENDENTITY_CREATE:
    	   	  returnval = ENDENTITYRULETEXTS[4];
    	   	  break;
    	   case BasicAccessRuleSet.ENDENTITY_EDIT:
    	   	  returnval = ENDENTITYRULETEXTS[5];
    	   	  break;
    	   case BasicAccessRuleSet.ENDENTITY_DELETE:
    	   	returnval = ENDENTITYRULETEXTS[6];
    	   	break;
    	   case BasicAccessRuleSet.ENDENTITY_REVOKE:
    	   	  returnval = ENDENTITYRULETEXTS[7];
    	   	  break;
    	   case BasicAccessRuleSet.ENDENTITY_KEYRECOVER:
    	   	returnval = ENDENTITYRULETEXTS[8];
    	   	break;
    	   case BasicAccessRuleSet.ENDENTITY_APPROVE:
       	   	returnval = ENDENTITYRULETEXTS[9];
       	   	break;
    	}
    	return returnval;
    }
    
   
}
