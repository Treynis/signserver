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

package org.ejbca.core.ejb.hardtoken;

import java.util.HashMap;

import javax.ejb.CreateException;

import org.apache.log4j.Logger;
import org.ejbca.core.ejb.BaseEntityBean;
import org.ejbca.core.model.hardtoken.HardTokenIssuer;



/** Entity bean should not be used directly, use though Session beans.
 *
 * Entity Bean representing a hard token issuer in the ra.
 * Information stored:
 * <pre>
 *  id (Primary key)
 *  alias (of the hard token issuer)
 *  admingroupid (Integer pointing to administrator group associated to this issuer)
 *  hardtokenissuer (Data saved concerning the hard token issuer)
 * </pre>
 *
 * @ejb.bean
 *	 xxxgenerate="false"
 *   description="This enterprise bean entity represents a hard token issuer with accompanying data"
 *   display-name="HardTokenIssuerDataEB"
 *   name="HardTokenIssuerData"
 *   jndi-name="HardTokenIssuerData"
 *   local-jndi-name="HardTokenIssuerDataLocal"
 *   view-type="local"
 *   type="CMP"
 *   reentrant="False"
 *   cmp-version="2.x"
 *   transaction-type="Container"
 *   schema="HardTokenIssuerDataBean"
 *   primkey-field="id"
 *
 * @ejb.pk generate="false"
 *   class="java.lang.Integer"
 *
 * @ejb.persistence table-name = "HardTokenIssuerData"
 * 
 * @ejb.home
 *   generate="local"
 *   local-extends="javax.ejb.EJBLocalHome"
 *   local-class="org.ejbca.core.ejb.hardtoken.HardTokenIssuerDataLocalHome"
 *
 * @ejb.interface
 *   generate="local"
 *   local-extends="javax.ejb.EJBLocalObject"
 *   local-class="org.ejbca.core.ejb.hardtoken.HardTokenIssuerDataLocal"
 *
 * @ejb.finder
 *   description="findByAlias"
 *   signature="org.ejbca.core.ejb.hardtoken.HardTokenIssuerDataLocal findByAlias(java.lang.String alias)"
 *   query="SELECT OBJECT(a) from HardTokenIssuerDataBean a WHERE a.alias=?1"
 *
 * @ejb.finder
 *   description="findAll"
 *   signature="java.util.Collection findAll()"
 *   query="SELECT OBJECT(a) from HardTokenIssuerDataBean AS a"
 *
 * @ejb.transaction type="Required"
 *
 * @jonas.jdbc-mapping
 *   jndi-name="${datasource.jndi-name}"
 *
 */
public abstract class HardTokenIssuerDataBean extends BaseEntityBean {

    private static final Logger log = Logger.getLogger(HardTokenIssuerDataBean.class);

	/**
     * @ejb.pk-field
	 * @ejb.persistence column-name="id"
     * @ejb.interface-method view-type="local"
     */
    public abstract Integer getId();

    /**
     */
    public abstract void setId(Integer id);

    /**
     * @ejb.persistence column-name="alias"
     * @ejb.interface-method view-type="local"
     */
    public abstract String getAlias();

    /**
     * @ejb.interface-method view-type="local"
     */
    public abstract void setAlias(String alias);

    /**
     * @ejb.persistence column-name="adminGroupId"
     * @ejb.interface-method view-type="local"
     */
    public abstract int getAdminGroupId();

    /**
     * @ejb.interface-method view-type="local"
     */
    public abstract void setAdminGroupId(int groupid);

    /**
     * @ejb.persistence column-name="data"
     * @weblogic.ora.columntyp@
     */
    public abstract HashMap getData();

    /**
     */
    public abstract void setData(HashMap data);


    /**
     * Method that returns the hard token issuer data and updates it if nessesary.
     * @ejb.interface-method view-type="local"
     */
    public HardTokenIssuer getHardTokenIssuer(){
      HardTokenIssuer returnval = new HardTokenIssuer();
      returnval.loadData(getData());
      return returnval;
    }

    /**
     * Method that saves the hard token issuer data to database.
     * @ejb.interface-method view-type="local"
     */
    public void setHardTokenIssuer(HardTokenIssuer hardtokenissuer){
       setData((HashMap) hardtokenissuer.saveData());
    }


    //
    // Fields required by Container
    //

    /**
     * Entity Bean holding data of a ahrd token issuer.
     *
     * @return null
     * @ejb.create-method view-type="local"
	 */
    public Integer ejbCreate(Integer id, String alias, int admingroupid,  HardTokenIssuer issuerdata) throws CreateException {
        setId(id);
        setAlias(alias);
        setAdminGroupId(admingroupid);
        setHardTokenIssuer(issuerdata);

        log.debug("Created Hard Token Issuer "+ alias );
        return id;
    }

    public void ejbPostCreate(Integer id, String alias, int admingroupid,  HardTokenIssuer issuerdata) {
        // Do nothing. Required.
    }
}
