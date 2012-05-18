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

package org.ejbca.core.ejb.authorization;

import javax.ejb.CreateException;

import org.ejbca.core.ejb.BaseEntityBean;


/** Entity bean should not be used directly, use though Session beans.
 *
 * Entity Bean representing  accessrules in EJBCA authorization module
 * Information stored:
 * <pre>
 * Access rule
 * rule (accept of decline)
 * isrecursive
 *
 * </pre>
 *
 * @ejb.bean
 *   description="This enterprise bean entity represents an access rule"
 *   display-name="AuthorizationTreeUpdateDataEB"
 *   name="AuthorizationTreeUpdateData"
 *   jndi-name="AuthorizationTreeUpdateData"
 *   view-type="local"
 *   type="CMP"
 *   reentrant="False"
 *   cmp-version="2.x"
 *   transaction-type="Container"
 *   schema="AuthorizationTreeUpdateDataBean"
 *   primkey-field="primKey"
 *
 * @ejb.pk
 *   generate="false"
 *   class="java.lang.Integer"
 *
 * @ejb.persistence table-name = "AuthorizationTreeUpdateData"
 * 
 * @ejb.transaction type="Required"
 * 
 * @ejb.home
 *   generate="local"
 *   local-extends="javax.ejb.EJBLocalHome"
 *   local-class="org.ejbca.core.ejb.authorization.AuthorizationTreeUpdateDataLocalHome"
 *
 * @ejb.interface
 *   generate="local"
 *   local-extends="javax.ejb.EJBLocalObject"
 *   local-class="org.ejbca.core.ejb.authorization.AuthorizationTreeUpdateDataLocal"
 *   
 * @jboss.method-attributes
 *   pattern = "get*"
 *   read-only = "true"
 *   
 *  @jboss.method-attributes
 *   pattern = "update*"
 *   read-only = "true"

 */
public abstract class AuthorizationTreeUpdateDataBean extends BaseEntityBean
{
    public static final Integer AUTHORIZATIONTREEUPDATEDATA = new Integer(1);

	/**
     * @ejb.persistence column-name="pK"
     * @ejb.pk-field
     */
    public abstract Integer getPrimKey();
    public abstract void setPrimKey(Integer primKey);

	/**
     * Method returning the newest authorizationtreeupdatenumber. Should be used after each
     * time the authorization tree is built.
     *
     * @return the newest accessruleset number.
     * @ejb.persistence column-name="authorizationTreeUpdateNumber"
     * @ejb.interface-method
     */
    public abstract int getAuthorizationTreeUpdateNumber();

    /**
     */
    public abstract void setAuthorizationTreeUpdateNumber(int authorizationtreeupdatenumber);

	/**
     * @ejb.create-method
	 */
    public Integer ejbCreate() throws CreateException {
      setPrimKey(AUTHORIZATIONTREEUPDATEDATA);
      setAuthorizationTreeUpdateNumber(0);
      return null;
    }

    public void ejbPostCreate() {
        // Do nothing. Required method.
    }

     /**
     * Method used check if a reconstruction of authorization tree is needed in the
     * authorization beans. It is used to avoid desyncronisation of authorization structures
     * in a distibuted environment.
     *
     * @param currentauthorizationtreeupdatenumber indicates which authorizationtreeupdatenumber is currently used.
     * @return true if update is needed.
     * @ejb.interface-method
     */
    public boolean updateNeccessary(int currentauthorizationtreeupdatenumber){
      return getAuthorizationTreeUpdateNumber() != currentauthorizationtreeupdatenumber;
    } // updateNeccessary


     /**
     * Method incrementing the authorizationtreeupdatenumber and thereby signaling
     * to other beans that they should reconstruct their accesstrees.
     * @ejb.interface-method
     */
    public void incrementAuthorizationTreeUpdateNumber(){
      setAuthorizationTreeUpdateNumber(getAuthorizationTreeUpdateNumber() +1);
    }  // incrementAuthorizationTreeUpdateNumber

}
