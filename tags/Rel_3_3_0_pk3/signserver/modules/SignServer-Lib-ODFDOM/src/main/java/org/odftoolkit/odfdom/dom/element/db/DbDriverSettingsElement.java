/************************************************************************
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 *
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0. You can also
 * obtain a copy of the License at http://odftoolkit.org/docs/license.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ************************************************************************/

/*
 * This file is automatically generated.
 * Don't edit manually.
 */    

package org.odftoolkit.odfdom.dom.element.db;

import org.odftoolkit.odfdom.OdfName;
import org.odftoolkit.odfdom.OdfNamespace;
import org.odftoolkit.odfdom.OdfFileDom;
import org.odftoolkit.odfdom.dom.OdfNamespaceNames;
import org.odftoolkit.odfdom.OdfElement;
import org.odftoolkit.odfdom.dom.attribute.db.DbShowDeletedAttribute;
import org.odftoolkit.odfdom.dom.attribute.db.DbSystemDriverSettingsAttribute;
import org.odftoolkit.odfdom.dom.attribute.db.DbBaseDnAttribute;
import org.odftoolkit.odfdom.dom.attribute.db.DbIsFirstRowHeaderLineAttribute;
import org.odftoolkit.odfdom.dom.attribute.db.DbParameterNameSubstitutionAttribute;


/**
 * DOM implementation of OpenDocument element  {@odf.element db:driver-settings}.
 *
 */
public abstract class DbDriverSettingsElement extends OdfElement
{        
    public static final OdfName ELEMENT_NAME = OdfName.get( OdfNamespace.get(OdfNamespaceNames.DB), "driver-settings" );


	/**
	 * Create the instance of <code>DbDriverSettingsElement</code> 
	 *
	 * @param  ownerDoc     The type is <code>OdfFileDom</code>
	 */
	public DbDriverSettingsElement( OdfFileDom ownerDoc )
	{
		super( ownerDoc, ELEMENT_NAME	);
	}

	/**
	 * Get the element name 
	 *
	 * @return  return   <code>OdfName</code> the name of element {@odf.element db:driver-settings}.
	 */
	public OdfName getOdfName()
	{
		return ELEMENT_NAME;
	}



	/**
	 * Receives the value of the ODFDOM attribute representation <code>DbShowDeletedAttribute</code> , See {@odf.attribute db:show-deleted}
	 *
	 * @return - the <code>Boolean</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public Boolean getDbShowDeletedAttribute()
	{
		DbShowDeletedAttribute attr = (DbShowDeletedAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.DB), "show-deleted" ) );
		if( attr != null ){
			return Boolean.valueOf( attr.booleanValue() );
		}
		return Boolean.valueOf( DbShowDeletedAttribute.DEFAULT_VALUE );
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>DbShowDeletedAttribute</code> , See {@odf.attribute db:show-deleted}
	 *
	 * @param dbShowDeletedValue   The type is <code>Boolean</code>
	 */
	public void setDbShowDeletedAttribute( Boolean dbShowDeletedValue )
	{
		DbShowDeletedAttribute attr =  new DbShowDeletedAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setBooleanValue( dbShowDeletedValue.booleanValue() );
	}


	/**
	 * Receives the value of the ODFDOM attribute representation <code>DbSystemDriverSettingsAttribute</code> , See {@odf.attribute db:system-driver-settings}
	 *
	 * @return - the <code>String</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public String getDbSystemDriverSettingsAttribute()
	{
		DbSystemDriverSettingsAttribute attr = (DbSystemDriverSettingsAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.DB), "system-driver-settings" ) );
		if( attr != null ){
			return String.valueOf( attr.getValue() );
		}
		return null;
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>DbSystemDriverSettingsAttribute</code> , See {@odf.attribute db:system-driver-settings}
	 *
	 * @param dbSystemDriverSettingsValue   The type is <code>String</code>
	 */
	public void setDbSystemDriverSettingsAttribute( String dbSystemDriverSettingsValue )
	{
		DbSystemDriverSettingsAttribute attr =  new DbSystemDriverSettingsAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setValue( dbSystemDriverSettingsValue );
	}


	/**
	 * Receives the value of the ODFDOM attribute representation <code>DbBaseDnAttribute</code> , See {@odf.attribute db:base-dn}
	 *
	 * @return - the <code>String</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public String getDbBaseDnAttribute()
	{
		DbBaseDnAttribute attr = (DbBaseDnAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.DB), "base-dn" ) );
		if( attr != null ){
			return String.valueOf( attr.getValue() );
		}
		return null;
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>DbBaseDnAttribute</code> , See {@odf.attribute db:base-dn}
	 *
	 * @param dbBaseDnValue   The type is <code>String</code>
	 */
	public void setDbBaseDnAttribute( String dbBaseDnValue )
	{
		DbBaseDnAttribute attr =  new DbBaseDnAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setValue( dbBaseDnValue );
	}


	/**
	 * Receives the value of the ODFDOM attribute representation <code>DbIsFirstRowHeaderLineAttribute</code> , See {@odf.attribute db:is-first-row-header-line}
	 *
	 * @return - the <code>Boolean</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public Boolean getDbIsFirstRowHeaderLineAttribute()
	{
		DbIsFirstRowHeaderLineAttribute attr = (DbIsFirstRowHeaderLineAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.DB), "is-first-row-header-line" ) );
		if( attr != null ){
			return Boolean.valueOf( attr.booleanValue() );
		}
		return Boolean.valueOf( DbIsFirstRowHeaderLineAttribute.DEFAULT_VALUE );
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>DbIsFirstRowHeaderLineAttribute</code> , See {@odf.attribute db:is-first-row-header-line}
	 *
	 * @param dbIsFirstRowHeaderLineValue   The type is <code>Boolean</code>
	 */
	public void setDbIsFirstRowHeaderLineAttribute( Boolean dbIsFirstRowHeaderLineValue )
	{
		DbIsFirstRowHeaderLineAttribute attr =  new DbIsFirstRowHeaderLineAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setBooleanValue( dbIsFirstRowHeaderLineValue.booleanValue() );
	}


	/**
	 * Receives the value of the ODFDOM attribute representation <code>DbParameterNameSubstitutionAttribute</code> , See {@odf.attribute db:parameter-name-substitution}
	 *
	 * @return - the <code>Boolean</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public Boolean getDbParameterNameSubstitutionAttribute()
	{
		DbParameterNameSubstitutionAttribute attr = (DbParameterNameSubstitutionAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.DB), "parameter-name-substitution" ) );
		if( attr != null ){
			return Boolean.valueOf( attr.booleanValue() );
		}
		return Boolean.valueOf( DbParameterNameSubstitutionAttribute.DEFAULT_VALUE );
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>DbParameterNameSubstitutionAttribute</code> , See {@odf.attribute db:parameter-name-substitution}
	 *
	 * @param dbParameterNameSubstitutionValue   The type is <code>Boolean</code>
	 */
	public void setDbParameterNameSubstitutionAttribute( Boolean dbParameterNameSubstitutionValue )
	{
		DbParameterNameSubstitutionAttribute attr =  new DbParameterNameSubstitutionAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setBooleanValue( dbParameterNameSubstitutionValue.booleanValue() );
	}

	/**
	 * Create child element {@odf.element db:auto-increment}.
	 *
	 * @return   return  the element {@odf.element db:auto-increment}
	 * DifferentQName 
	 */
	public DbAutoIncrementElement newDbAutoIncrementElement()
	{
		DbAutoIncrementElement  dbAutoIncrement = ((OdfFileDom)this.ownerDocument).newOdfElement(DbAutoIncrementElement.class);
		this.appendChild( dbAutoIncrement);
		return  dbAutoIncrement;
	}                   
               
	/**
	 * Create child element {@odf.element db:delimiter}.
	 *
	 * @return   return  the element {@odf.element db:delimiter}
	 * DifferentQName 
	 */
	public DbDelimiterElement newDbDelimiterElement()
	{
		DbDelimiterElement  dbDelimiter = ((OdfFileDom)this.ownerDocument).newOdfElement(DbDelimiterElement.class);
		this.appendChild( dbDelimiter);
		return  dbDelimiter;
	}                   
               
	/**
	 * Create child element {@odf.element db:character-set}.
	 *
	 * @return   return  the element {@odf.element db:character-set}
	 * DifferentQName 
	 */
	public DbCharacterSetElement newDbCharacterSetElement()
	{
		DbCharacterSetElement  dbCharacterSet = ((OdfFileDom)this.ownerDocument).newOdfElement(DbCharacterSetElement.class);
		this.appendChild( dbCharacterSet);
		return  dbCharacterSet;
	}                   
               
	/**
	 * Create child element {@odf.element db:table-settings}.
	 *
	 * @return   return  the element {@odf.element db:table-settings}
	 * DifferentQName 
	 */
	public DbTableSettingsElement newDbTableSettingsElement()
	{
		DbTableSettingsElement  dbTableSettings = ((OdfFileDom)this.ownerDocument).newOdfElement(DbTableSettingsElement.class);
		this.appendChild( dbTableSettings);
		return  dbTableSettings;
	}                   
               
}
