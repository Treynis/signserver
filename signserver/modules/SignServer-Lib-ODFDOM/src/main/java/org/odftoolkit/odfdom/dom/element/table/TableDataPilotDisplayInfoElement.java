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

package org.odftoolkit.odfdom.dom.element.table;

import org.odftoolkit.odfdom.OdfName;
import org.odftoolkit.odfdom.OdfNamespace;
import org.odftoolkit.odfdom.OdfFileDom;
import org.odftoolkit.odfdom.dom.OdfNamespaceNames;
import org.odftoolkit.odfdom.OdfElement;
import org.odftoolkit.odfdom.dom.attribute.table.TableEnabledAttribute;
import org.odftoolkit.odfdom.dom.attribute.table.TableDataFieldAttribute;
import org.odftoolkit.odfdom.dom.attribute.table.TableMemberCountAttribute;
import org.odftoolkit.odfdom.dom.attribute.table.TableDisplayMemberModeAttribute;


/**
 * DOM implementation of OpenDocument element  {@odf.element table:data-pilot-display-info}.
 *
 */
public abstract class TableDataPilotDisplayInfoElement extends OdfElement
{        
    public static final OdfName ELEMENT_NAME = OdfName.get( OdfNamespace.get(OdfNamespaceNames.TABLE), "data-pilot-display-info" );


	/**
	 * Create the instance of <code>TableDataPilotDisplayInfoElement</code> 
	 *
	 * @param  ownerDoc     The type is <code>OdfFileDom</code>
	 */
	public TableDataPilotDisplayInfoElement( OdfFileDom ownerDoc )
	{
		super( ownerDoc, ELEMENT_NAME	);
	}

	/**
	 * Get the element name 
	 *
	 * @return  return   <code>OdfName</code> the name of element {@odf.element table:data-pilot-display-info}.
	 */
	public OdfName getOdfName()
	{
		return ELEMENT_NAME;
	}

	/**
	 * Initialization of the mandatory attributes of {@link  TableDataPilotDisplayInfoElement}
	 *
     * @param tableEnabledAttributeValue  The mandatory attribute {@odf.attribute  table:enabled}"
     * @param tableDataFieldAttributeValue  The mandatory attribute {@odf.attribute  table:data-field}"
     * @param tableMemberCountAttributeValue  The mandatory attribute {@odf.attribute  table:member-count}"
     * @param tableDisplayMemberModeAttributeValue  The mandatory attribute {@odf.attribute  table:display-member-mode}"
     *
	 */
	public void init(boolean tableEnabledAttributeValue, String tableDataFieldAttributeValue, int tableMemberCountAttributeValue, String tableDisplayMemberModeAttributeValue)
	{
		setTableEnabledAttribute( Boolean.valueOf(tableEnabledAttributeValue) );
		setTableDataFieldAttribute( tableDataFieldAttributeValue );
		setTableMemberCountAttribute( Integer.valueOf(tableMemberCountAttributeValue) );
		setTableDisplayMemberModeAttribute( tableDisplayMemberModeAttributeValue );
	}

	/**
	 * Receives the value of the ODFDOM attribute representation <code>TableEnabledAttribute</code> , See {@odf.attribute table:enabled}
	 *
	 * @return - the <code>Boolean</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public Boolean getTableEnabledAttribute()
	{
		TableEnabledAttribute attr = (TableEnabledAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.TABLE), "enabled" ) );
		if( attr != null ){
			return Boolean.valueOf( attr.booleanValue() );
		}
		return null;
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>TableEnabledAttribute</code> , See {@odf.attribute table:enabled}
	 *
	 * @param tableEnabledValue   The type is <code>Boolean</code>
	 */
	public void setTableEnabledAttribute( Boolean tableEnabledValue )
	{
		TableEnabledAttribute attr =  new TableEnabledAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setBooleanValue( tableEnabledValue.booleanValue() );
	}


	/**
	 * Receives the value of the ODFDOM attribute representation <code>TableDataFieldAttribute</code> , See {@odf.attribute table:data-field}
	 *
	 * @return - the <code>String</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public String getTableDataFieldAttribute()
	{
		TableDataFieldAttribute attr = (TableDataFieldAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.TABLE), "data-field" ) );
		if( attr != null ){
			return String.valueOf( attr.getValue() );
		}
		return null;
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>TableDataFieldAttribute</code> , See {@odf.attribute table:data-field}
	 *
	 * @param tableDataFieldValue   The type is <code>String</code>
	 */
	public void setTableDataFieldAttribute( String tableDataFieldValue )
	{
		TableDataFieldAttribute attr =  new TableDataFieldAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setValue( tableDataFieldValue );
	}


	/**
	 * Receives the value of the ODFDOM attribute representation <code>TableMemberCountAttribute</code> , See {@odf.attribute table:member-count}
	 *
	 * @return - the <code>Integer</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public Integer getTableMemberCountAttribute()
	{
		TableMemberCountAttribute attr = (TableMemberCountAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.TABLE), "member-count" ) );
		if( attr != null ){
			return Integer.valueOf( attr.intValue() );
		}
		return null;
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>TableMemberCountAttribute</code> , See {@odf.attribute table:member-count}
	 *
	 * @param tableMemberCountValue   The type is <code>Integer</code>
	 */
	public void setTableMemberCountAttribute( Integer tableMemberCountValue )
	{
		TableMemberCountAttribute attr =  new TableMemberCountAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setIntValue( tableMemberCountValue.intValue() );
	}


	/**
	 * Receives the value of the ODFDOM attribute representation <code>TableDisplayMemberModeAttribute</code> , See {@odf.attribute table:display-member-mode}
	 *
	 * @return - the <code>String</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public String getTableDisplayMemberModeAttribute()
	{
		TableDisplayMemberModeAttribute attr = (TableDisplayMemberModeAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.TABLE), "display-member-mode" ) );
		if( attr != null ){
			return String.valueOf( attr.getValue() );
		}
		return null;
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>TableDisplayMemberModeAttribute</code> , See {@odf.attribute table:display-member-mode}
	 *
	 * @param tableDisplayMemberModeValue   The type is <code>String</code>
	 */
	public void setTableDisplayMemberModeAttribute( String tableDisplayMemberModeValue )
	{
		TableDisplayMemberModeAttribute attr =  new TableDisplayMemberModeAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setValue( tableDisplayMemberModeValue );
	}

}
