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

package org.ejbca.core.ejb;

import javax.ejb.CreateException;



/**
 * Base class for property entity beans implementing required methods and helpers.
 * A property entity bean extends other entity beans with propertys.
 *
 * Primary Key is a combined id and property hash.
 * id (String) primary key of entity bean using this property entity bean.
 * property String should be one of the property constants.
 * value (String) the value of the property.
 *
 * @version $Id: BasePropertyEntityBean.java 5585 2008-05-01 20:55:00Z anatom $
 *
 * @ejb.bean
 *	 generate="false"
 *   name="BaseProperty"
 *   display-name="BasePropertyEB"
 *   view-type="local"
 *   cmp-version="2.x"
 *
 * @ejb.pk
 *   class="org.ejbca.core.ejb.PropertyEntityPK"
 *   extends="java.lang.Object"
 *
 * @ejb.home
 *   generate="local"
 *   local-extends="javax.ejb.EJBLocalHome"
 *   local-class="org.ejbca.core.ejb.BasePropertyDataLocalHome"
 *
 * @ejb.interface
 *   generate="local"
 *   local-extends="javax.ejb.EJBLocalObject"
 *   local-class="org.ejbca.core.ejb.BasePropertyDataLocal"
 */
public abstract class BasePropertyEntityBean extends BaseEntityBean {

    /**
     * @ejb.persistence
     * @ejb.pk-field
     * @ejb.interface-method
     */
	public abstract String getId();

    /**
     * @ejb.persistence
     */
	public abstract void setId(String id);

    /**
     * @ejb.persistence
     * @ejb.pk-field
     * @ejb.interface-method
     */
	public abstract String getProperty();

    /**
     * @ejb.persistence
     */
	public abstract void setProperty(String property);

    /**
     * @ejb.persistence
     * @ejb.interface-method
     */
	public abstract String getValue();

    /**
     * @ejb.persistence
     * @ejb.interface-method
     */
	public abstract void setValue(String value);

    /**
     * Creates a new BasePropertyEntityBean object.
     */
    public BasePropertyEntityBean() {
        super();
    }

	/**
	 * Entity Bean holding data of a raadmin profile.
	 * @return PropertyEntityPK beeing the PrimaryKey for the created entity
     * @ejb.create-method
     */
	public PropertyEntityPK ejbCreate(String id, String property, String value)
	       throws CreateException {
	    PropertyEntityPK pk = new PropertyEntityPK(id,property);
		setId(id);
		setProperty(property);
		setValue(value);
		return pk;
	}

	public void ejbPostCreate(String id, String property, String value) {
		// Do nothing. Required.
	}

}
