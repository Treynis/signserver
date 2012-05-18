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
 
package org.ejbca.core.ejb.ra.raadmin;

import java.util.HashMap;

import javax.ejb.CreateException;

import org.apache.log4j.Logger;
import org.ejbca.core.ejb.BaseEntityBean;
import org.ejbca.core.model.ra.raadmin.AdminPreference;




/**
 * Entity bean should not be used directly, use though Session beans. Entity Bean representing
 * admin preference. Information stored:
 * <pre>
 * Id  (BigInteger SerialNumber)
 * AdminPreference
 * </pre>
 *
 * @version $Id: AdminPreferencesDataBean.java 5585 2008-05-01 20:55:00Z anatom $
 *
 * @ejb.bean description="This enterprise bean entity represents a ra admins user preference."
 * display-name="AdminPreferencesDataEB"
 * name="AdminPreferencesData"
 * jndi-name="AdminPreferencesData"
 * view-type="local"
 * type="CMP"
 * reentrant="False"
 * cmp-version="2.x"
 * transaction-type="Container"
 * schema="AdminPreferencesDataBean"
 * primkey-field="id"
 *
 * @ejb.pk class="java.lang.String"
 * generate="false"
 *
 * @ejb.persistence table-name = "AdminPreferencesData"
 * 
 * @ejb.transaction type="Required"
 * 
 * @ejb.home
 * local-extends="javax.ejb.EJBLocalHome"
 * local-class="org.ejbca.core.ejb.ra.raadmin.AdminPreferencesDataLocalHome"
 *
 * @ejb.interface
 * local-extends="javax.ejb.EJBLocalObject"
 * local-class="org.ejbca.core.ejb.ra.raadmin.AdminPreferencesDataLocal"
 *
 */
public abstract class AdminPreferencesDataBean extends BaseEntityBean {
    private static final Logger log = Logger.getLogger(AdminPreferencesDataBean.class);

    /**
     * @ejb.pk-field
     * @ejb.persistence column-name="id"
     * @ejb.interface-method
     */
    public abstract String getId();

    /**
     */
    public abstract void setId(String id);

    /**
     * @ejb.persistence  column-name="data"
     * @weblogic.ora.columntyp@
     */
    public abstract HashMap getData();
    /**
     */
    public abstract void setData(HashMap data);

    /**
     * Method that returns the admin preference and updates it if nessesary.
     *
     * @return DOCUMENT ME!
     * @ejb.interface-method
     */
    public AdminPreference getAdminPreference() {
        AdminPreference returnval = new AdminPreference();
        returnval.loadData(getData());

        return returnval;
    }

    /**
     * Method that saves the admin preference to database.
     *
     * @param adminpreference DOCUMENT ME!
     * @ejb.interface-method
     */
    public void setAdminPreference(AdminPreference adminpreference) {
        setData((HashMap) adminpreference.saveData());
    }

    //
    // Fields required by Container
    //

    /**
     * Entity Bean holding data of admin preferences.
     *
     * @param id the serialnumber.
     * @param adminpreference is the AdminPreference.
     *
     * @return the primary key
     * @ejb.create-method
     */
    public String ejbCreate(String id, AdminPreference adminpreference)
        throws CreateException {
        setId(id);
        setAdminPreference(adminpreference);

        log.debug("Created admin preference " + id);

        return id;
    }

    /**
     * DOCUMENT ME!
     *
     * @param id DOCUMENT ME!
     * @param adminpreference DOCUMENT ME!
     */
    public void ejbPostCreate(String id, AdminPreference adminpreference) {
        // Do nothing. Required.
    }
}
