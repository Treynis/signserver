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
 
package org.ejbca.core.model.hardtoken;

import java.util.Collection;
import java.util.Date;

import org.ejbca.core.model.hardtoken.types.HardToken;
import org.ejbca.util.StringTools;


/**
 *  This is a value class containing the data relating to a hard token sent between
 *  server and clients.
 *
 * @author  TomSelleck
 * @version $Id: HardTokenData.java 5585 2008-05-01 20:55:00Z anatom $
 */

public class HardTokenData implements java.io.Serializable {

    
	// Public Constructors
	/** 
	 * Construtor of a hard token data.
	 * 
	 * @param tokensn the tokensn
	 * @param username the username owning the token
	 * @param createtime time the token was created
	 * @param modifytime time whem token was modified or a copy was made.
	 * @param tokentype the hardtokenprofile used to create the token
	 * @param significantIssuerDN of the CA that the card belongs to
	 * @param hardtoken the actual hardtoken data
	 * @param copyof tokenSN of original or null of this is an original
	 * @param copies Collention of tokensn of tokens copied from this token, null if no copies have been made.
	 * 
	 */
    public HardTokenData(String tokensn, String username, Date createtime,  Date modifytime, 
                         int tokentype, String significantIssuerDN, HardToken hardtoken, String copyof,
                         Collection copies){
      this.tokensn=tokensn;
      this.username=StringTools.strip(username);
      this.createtime=createtime;
      this.modifytime=modifytime;
      this.tokentype=tokentype;
      this.significantIssuerDN = significantIssuerDN;
      this.hardtoken=hardtoken;
      this.copyof=copyof;
      this.copies=copies;
    }

    public HardTokenData(){
    }

    // Public Methods

    public String getTokenSN(){ return this.tokensn; }
    public void setTokenSN(String tokensn){ this.tokensn=tokensn; }

    public String getUsername(){ return this.username; }
    public void setUsername(String username){ this.username=StringTools.strip(username); }

    public Date getCreateTime(){ return this.createtime; }
    public void setCreateTime(Date createtime){ this.createtime=createtime; }

    public Date getModifyTime(){ return this.modifytime; }
    public void setModifyTime(Date modifytime){ this.modifytime=modifytime; }

    public int getTokenType(){ return this.tokentype; }
    public void setTokenType(int tokentype){ this.tokentype=tokentype; }

    public HardToken getHardToken(){ return this.hardtoken; }
    public void setHardToken(HardToken hardtoken){ this.hardtoken=hardtoken; }
    
    public boolean isOriginal(){
      return copyof==null;	
    }
    
    public String getCopyOf(){
      return copyof;	
    }

	public String getSignificantIssuerDN() {
		return significantIssuerDN;
	}
    
    /** 
     * Returns a collection of (Strings) containing the tokenSN of all copies made
     * of this token.
     * 
     * @return A Collection of tokenSN or null of no copies have been made.
     * 
     */
    public Collection getCopies(){
      return copies;	
    }

    // Private fields
    private    String          tokensn;
    private    String          username;
    private    Date            createtime;
    private    Date            modifytime;
    private    int             tokentype;
    private    String          significantIssuerDN;
    private    HardToken       hardtoken;
    private    String          copyof;
    private    Collection      copies;

}
