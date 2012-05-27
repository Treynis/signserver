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
import org.odftoolkit.odfdom.dom.attribute.table.TableShowEmptyAttribute;


/**
 * DOM implementation of OpenDocument element  {@odf.element table:data-pilot-level}.
 *
 */
public abstract class TableDataPilotLevelElement extends OdfElement
{        
    public static final OdfName ELEMENT_NAME = OdfName.get( OdfNamespace.get(OdfNamespaceNames.TABLE), "data-pilot-level" );


	/**
	 * Create the instance of <code>TableDataPilotLevelElement</code> 
	 *
	 * @param  ownerDoc     The type is <code>OdfFileDom</code>
	 */
	public TableDataPilotLevelElement( OdfFileDom ownerDoc )
	{
		super( ownerDoc, ELEMENT_NAME	);
	}

	/**
	 * Get the element name 
	 *
	 * @return  return   <code>OdfName</code> the name of element {@odf.element table:data-pilot-level}.
	 */
	public OdfName getOdfName()
	{
		return ELEMENT_NAME;
	}



	/**
	 * Receives the value of the ODFDOM attribute representation <code>TableShowEmptyAttribute</code> , See {@odf.attribute table:show-empty}
	 *
	 * @return - the <code>Boolean</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public Boolean getTableShowEmptyAttribute()
	{
		TableShowEmptyAttribute attr = (TableShowEmptyAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.TABLE), "show-empty" ) );
		if( attr != null ){
			return Boolean.valueOf( attr.booleanValue() );
		}
		return null;
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>TableShowEmptyAttribute</code> , See {@odf.attribute table:show-empty}
	 *
	 * @param tableShowEmptyValue   The type is <code>Boolean</code>
	 */
	public void setTableShowEmptyAttribute( Boolean tableShowEmptyValue )
	{
		TableShowEmptyAttribute attr =  new TableShowEmptyAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setBooleanValue( tableShowEmptyValue.booleanValue() );
	}

	/**
	 * Create child element {@odf.element table:data-pilot-subtotals}.
	 *
	 * @return   return  the element {@odf.element table:data-pilot-subtotals}
	 * DifferentQName 
	 */
	public TableDataPilotSubtotalsElement newTableDataPilotSubtotalsElement()
	{
		TableDataPilotSubtotalsElement  tableDataPilotSubtotals = ((OdfFileDom)this.ownerDocument).newOdfElement(TableDataPilotSubtotalsElement.class);
		this.appendChild( tableDataPilotSubtotals);
		return  tableDataPilotSubtotals;
	}                   
               
	/**
	 * Create child element {@odf.element table:data-pilot-members}.
	 *
	 * @return   return  the element {@odf.element table:data-pilot-members}
	 * DifferentQName 
	 */
	public TableDataPilotMembersElement newTableDataPilotMembersElement()
	{
		TableDataPilotMembersElement  tableDataPilotMembers = ((OdfFileDom)this.ownerDocument).newOdfElement(TableDataPilotMembersElement.class);
		this.appendChild( tableDataPilotMembers);
		return  tableDataPilotMembers;
	}                   
               
	/**
	 * Create child element {@odf.element table:data-pilot-display-info}.
	 *
     * @param tableDataFieldAttributeValue  the <code>String</code> value of <code>TableDataFieldAttribute</code>, see {@odf.attribute  table:data-field} at specification
	 * @param tableDisplayMemberModeAttributeValue  the <code>String</code> value of <code>TableDisplayMemberModeAttribute</code>, see {@odf.attribute  table:display-member-mode} at specification
	 * @param tableEnabledAttributeValue  the <code>boolean</code> value of <code>TableEnabledAttribute</code>, see {@odf.attribute  table:enabled} at specification
	 * @param tableMemberCountAttributeValue  the <code>int</code> value of <code>TableMemberCountAttribute</code>, see {@odf.attribute  table:member-count} at specification
	 * @return   return  the element {@odf.element table:data-pilot-display-info}
	 * DifferentQName 
	 */
    
	public TableDataPilotDisplayInfoElement newTableDataPilotDisplayInfoElement(String tableDataFieldAttributeValue, String tableDisplayMemberModeAttributeValue, boolean tableEnabledAttributeValue, int tableMemberCountAttributeValue)
	{
		TableDataPilotDisplayInfoElement  tableDataPilotDisplayInfo = ((OdfFileDom)this.ownerDocument).newOdfElement(TableDataPilotDisplayInfoElement.class);
		tableDataPilotDisplayInfo.setTableDataFieldAttribute( tableDataFieldAttributeValue );
		tableDataPilotDisplayInfo.setTableDisplayMemberModeAttribute( tableDisplayMemberModeAttributeValue );
		tableDataPilotDisplayInfo.setTableEnabledAttribute( Boolean.valueOf(tableEnabledAttributeValue) );
		tableDataPilotDisplayInfo.setTableMemberCountAttribute( Integer.valueOf(tableMemberCountAttributeValue) );
		this.appendChild( tableDataPilotDisplayInfo);
		return  tableDataPilotDisplayInfo;      
	}
    
	/**
	 * Create child element {@odf.element table:data-pilot-sort-info}.
	 *
     * @param tableOrderAttributeValue  the <code>String</code> value of <code>TableOrderAttribute</code>, see {@odf.attribute  table:order} at specification
	 * @param tableSortModeAttributeValue  the <code>String</code> value of <code>TableSortModeAttribute</code>, see {@odf.attribute  table:sort-mode} at specification
	 * @return   return  the element {@odf.element table:data-pilot-sort-info}
	 * DifferentQName 
	 */
    
	public TableDataPilotSortInfoElement newTableDataPilotSortInfoElement(String tableOrderAttributeValue, String tableSortModeAttributeValue)
	{
		TableDataPilotSortInfoElement  tableDataPilotSortInfo = ((OdfFileDom)this.ownerDocument).newOdfElement(TableDataPilotSortInfoElement.class);
		tableDataPilotSortInfo.setTableOrderAttribute( tableOrderAttributeValue );
		tableDataPilotSortInfo.setTableSortModeAttribute( tableSortModeAttributeValue );
		this.appendChild( tableDataPilotSortInfo);
		return  tableDataPilotSortInfo;      
	}
    
	/**
	 * Create child element {@odf.element table:data-pilot-sort-info}.
	 *
     * @param tableDataFieldAttributeValue  the <code>String</code> value of <code>TableDataFieldAttribute</code>, see {@odf.attribute  table:data-field} at specification
	 * @param tableOrderAttributeValue  the <code>String</code> value of <code>TableOrderAttribute</code>, see {@odf.attribute  table:order} at specification
	 * @param tableSortModeAttributeValue  the <code>String</code> value of <code>TableSortModeAttribute</code>, see {@odf.attribute  table:sort-mode} at specification
	 * @return   return  the element {@odf.element table:data-pilot-sort-info}
	 * DifferentQName 
	 */
    
	public TableDataPilotSortInfoElement newTableDataPilotSortInfoElement(String tableDataFieldAttributeValue, String tableOrderAttributeValue, String tableSortModeAttributeValue)
	{
		TableDataPilotSortInfoElement  tableDataPilotSortInfo = ((OdfFileDom)this.ownerDocument).newOdfElement(TableDataPilotSortInfoElement.class);
		tableDataPilotSortInfo.setTableDataFieldAttribute( tableDataFieldAttributeValue );
		tableDataPilotSortInfo.setTableOrderAttribute( tableOrderAttributeValue );
		tableDataPilotSortInfo.setTableSortModeAttribute( tableSortModeAttributeValue );
		this.appendChild( tableDataPilotSortInfo);
		return  tableDataPilotSortInfo;      
	}
    
	/**
	 * Create child element {@odf.element table:data-pilot-layout-info}.
	 *
     * @param tableAddEmptyLinesAttributeValue  the <code>boolean</code> value of <code>TableAddEmptyLinesAttribute</code>, see {@odf.attribute  table:add-empty-lines} at specification
	 * @param tableLayoutModeAttributeValue  the <code>String</code> value of <code>TableLayoutModeAttribute</code>, see {@odf.attribute  table:layout-mode} at specification
	 * @return   return  the element {@odf.element table:data-pilot-layout-info}
	 * DifferentQName 
	 */
    
	public TableDataPilotLayoutInfoElement newTableDataPilotLayoutInfoElement(boolean tableAddEmptyLinesAttributeValue, String tableLayoutModeAttributeValue)
	{
		TableDataPilotLayoutInfoElement  tableDataPilotLayoutInfo = ((OdfFileDom)this.ownerDocument).newOdfElement(TableDataPilotLayoutInfoElement.class);
		tableDataPilotLayoutInfo.setTableAddEmptyLinesAttribute( Boolean.valueOf(tableAddEmptyLinesAttributeValue) );
		tableDataPilotLayoutInfo.setTableLayoutModeAttribute( tableLayoutModeAttributeValue );
		this.appendChild( tableDataPilotLayoutInfo);
		return  tableDataPilotLayoutInfo;      
	}
    
}
