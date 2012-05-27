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

package org.odftoolkit.odfdom.dom.element.office;

import org.odftoolkit.odfdom.OdfName;
import org.odftoolkit.odfdom.OdfNamespace;
import org.odftoolkit.odfdom.OdfFileDom;
import org.odftoolkit.odfdom.dom.OdfNamespaceNames;
import org.odftoolkit.odfdom.OdfElement;
import org.odftoolkit.odfdom.dom.attribute.table.TableStructureProtectedAttribute;
import org.odftoolkit.odfdom.dom.attribute.table.TableProtectionKeyAttribute;
import org.odftoolkit.odfdom.dom.attribute.table.TableProtectionKeyDigestAlgorithmAttribute;

import org.odftoolkit.odfdom.dom.element.table.TableTrackedChangesElement;
import org.odftoolkit.odfdom.dom.element.text.TextVariableDeclElement;
import org.odftoolkit.odfdom.dom.element.text.TextSequenceDeclElement;
import org.odftoolkit.odfdom.dom.element.text.TextUserFieldDeclElement;
import org.odftoolkit.odfdom.dom.element.text.TextDdeConnectionDeclElement;
import org.odftoolkit.odfdom.dom.element.text.TextAlphabeticalIndexAutoMarkFileElement;
import org.odftoolkit.odfdom.dom.element.table.TableCalculationSettingsElement;
import org.odftoolkit.odfdom.dom.element.table.TableContentValidationsElement;
import org.odftoolkit.odfdom.dom.element.table.TableLabelRangesElement;
import org.odftoolkit.odfdom.dom.element.table.TableTableElement;
import org.odftoolkit.odfdom.dom.element.table.TableNamedExpressionsElement;
import org.odftoolkit.odfdom.dom.element.table.TableDatabaseRangesElement;
import org.odftoolkit.odfdom.dom.element.table.TableDataPilotTablesElement;
import org.odftoolkit.odfdom.dom.element.table.TableConsolidationElement;
import org.odftoolkit.odfdom.dom.element.table.TableDdeLinksElement;

/**
 * DOM implementation of OpenDocument element  {@odf.element office:spreadsheet}.
 *
 */
public abstract class OfficeSpreadsheetElement extends OdfElement
{        
    public static final OdfName ELEMENT_NAME = OdfName.get( OdfNamespace.get(OdfNamespaceNames.OFFICE), "spreadsheet" );


	/**
	 * Create the instance of <code>OfficeSpreadsheetElement</code> 
	 *
	 * @param  ownerDoc     The type is <code>OdfFileDom</code>
	 */
	public OfficeSpreadsheetElement( OdfFileDom ownerDoc )
	{
		super( ownerDoc, ELEMENT_NAME	);
	}

	/**
	 * Get the element name 
	 *
	 * @return  return   <code>OdfName</code> the name of element {@odf.element office:spreadsheet}.
	 */
	public OdfName getOdfName()
	{
		return ELEMENT_NAME;
	}



	/**
	 * Receives the value of the ODFDOM attribute representation <code>TableStructureProtectedAttribute</code> , See {@odf.attribute table:structure-protected}
	 *
	 * @return - the <code>Boolean</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public Boolean getTableStructureProtectedAttribute()
	{
		TableStructureProtectedAttribute attr = (TableStructureProtectedAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.TABLE), "structure-protected" ) );
		if( attr != null ){
			return Boolean.valueOf( attr.booleanValue() );
		}
		return Boolean.valueOf( TableStructureProtectedAttribute.DEFAULT_VALUE );
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>TableStructureProtectedAttribute</code> , See {@odf.attribute table:structure-protected}
	 *
	 * @param tableStructureProtectedValue   The type is <code>Boolean</code>
	 */
	public void setTableStructureProtectedAttribute( Boolean tableStructureProtectedValue )
	{
		TableStructureProtectedAttribute attr =  new TableStructureProtectedAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setBooleanValue( tableStructureProtectedValue.booleanValue() );
	}


	/**
	 * Receives the value of the ODFDOM attribute representation <code>TableProtectionKeyAttribute</code> , See {@odf.attribute table:protection-key}
	 *
	 * @return - the <code>String</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public String getTableProtectionKeyAttribute()
	{
		TableProtectionKeyAttribute attr = (TableProtectionKeyAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.TABLE), "protection-key" ) );
		if( attr != null ){
			return String.valueOf( attr.getValue() );
		}
		return null;
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>TableProtectionKeyAttribute</code> , See {@odf.attribute table:protection-key}
	 *
	 * @param tableProtectionKeyValue   The type is <code>String</code>
	 */
	public void setTableProtectionKeyAttribute( String tableProtectionKeyValue )
	{
		TableProtectionKeyAttribute attr =  new TableProtectionKeyAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setValue( tableProtectionKeyValue );
	}


	/**
	 * Receives the value of the ODFDOM attribute representation <code>TableProtectionKeyDigestAlgorithmAttribute</code> , See {@odf.attribute table:protection-key-digest-algorithm}
	 *
	 * @return - the <code>String</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public String getTableProtectionKeyDigestAlgorithmAttribute()
	{
		TableProtectionKeyDigestAlgorithmAttribute attr = (TableProtectionKeyDigestAlgorithmAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.TABLE), "protection-key-digest-algorithm" ) );
		if( attr != null ){
			return String.valueOf( attr.getValue() );
		}
		return TableProtectionKeyDigestAlgorithmAttribute.DEFAULT_VALUE;
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>TableProtectionKeyDigestAlgorithmAttribute</code> , See {@odf.attribute table:protection-key-digest-algorithm}
	 *
	 * @param tableProtectionKeyDigestAlgorithmValue   The type is <code>String</code>
	 */
	public void setTableProtectionKeyDigestAlgorithmAttribute( String tableProtectionKeyDigestAlgorithmValue )
	{
		TableProtectionKeyDigestAlgorithmAttribute attr =  new TableProtectionKeyDigestAlgorithmAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setValue( tableProtectionKeyDigestAlgorithmValue );
	}

	/**
	 * Create child element {@odf.element table:tracked-changes}.
	 *
	 * @return   return  the element {@odf.element table:tracked-changes}
	 * DifferentQName 
	 */
	public TableTrackedChangesElement newTableTrackedChangesElement()
	{
		TableTrackedChangesElement  tableTrackedChanges = ((OdfFileDom)this.ownerDocument).newOdfElement(TableTrackedChangesElement.class);
		this.appendChild( tableTrackedChanges);
		return  tableTrackedChanges;
	}                   
               
	/**
	 * Create child element {@odf.element text:variable-decl}.
	 *
     * @param officeValueTypeAttributeValue  the <code>String</code> value of <code>OfficeValueTypeAttribute</code>, see {@odf.attribute  office:value-type} at specification
	 * @param textNameAttributeValue  the <code>String</code> value of <code>TextNameAttribute</code>, see {@odf.attribute  text:name} at specification
	 * @return   return  the element {@odf.element text:variable-decl}
	 * DifferentQName 
	 */
    
	public TextVariableDeclElement newTextVariableDeclElement(String officeValueTypeAttributeValue, String textNameAttributeValue)
	{
		TextVariableDeclElement  textVariableDecl = ((OdfFileDom)this.ownerDocument).newOdfElement(TextVariableDeclElement.class);
		textVariableDecl.setOfficeValueTypeAttribute( officeValueTypeAttributeValue );
		textVariableDecl.setTextNameAttribute( textNameAttributeValue );
		this.appendChild( textVariableDecl);
		return  textVariableDecl;      
	}
    
	/**
	 * Create child element {@odf.element text:sequence-decl}.
	 *
     * @param textDisplayOutlineLevelAttributeValue  the <code>int</code> value of <code>TextDisplayOutlineLevelAttribute</code>, see {@odf.attribute  text:display-outline-level} at specification
	 * @param textNameAttributeValue  the <code>String</code> value of <code>TextNameAttribute</code>, see {@odf.attribute  text:name} at specification
	 * @return   return  the element {@odf.element text:sequence-decl}
	 * DifferentQName 
	 */
    
	public TextSequenceDeclElement newTextSequenceDeclElement(int textDisplayOutlineLevelAttributeValue, String textNameAttributeValue)
	{
		TextSequenceDeclElement  textSequenceDecl = ((OdfFileDom)this.ownerDocument).newOdfElement(TextSequenceDeclElement.class);
		textSequenceDecl.setTextDisplayOutlineLevelAttribute( Integer.valueOf(textDisplayOutlineLevelAttributeValue) );
		textSequenceDecl.setTextNameAttribute( textNameAttributeValue );
		this.appendChild( textSequenceDecl);
		return  textSequenceDecl;      
	}
    
	/**
	 * Create child element {@odf.element text:user-field-decl}.
	 *
     * @param officeValueAttributeValue  the <code>double</code> value of <code>OfficeValueAttribute</code>, see {@odf.attribute  office:value} at specification
	 * @param officeValueTypeAttributeValue  the <code>String</code> value of <code>OfficeValueTypeAttribute</code>, see {@odf.attribute  office:value-type} at specification
	 * @param textNameAttributeValue  the <code>String</code> value of <code>TextNameAttribute</code>, see {@odf.attribute  text:name} at specification
	 * @return   return  the element {@odf.element text:user-field-decl}
	 * DifferentQName 
	 */
    
	public TextUserFieldDeclElement newTextUserFieldDeclElement(double officeValueAttributeValue, String officeValueTypeAttributeValue, String textNameAttributeValue)
	{
		TextUserFieldDeclElement  textUserFieldDecl = ((OdfFileDom)this.ownerDocument).newOdfElement(TextUserFieldDeclElement.class);
		textUserFieldDecl.setOfficeValueAttribute( Double.valueOf(officeValueAttributeValue) );
		textUserFieldDecl.setOfficeValueTypeAttribute( officeValueTypeAttributeValue );
		textUserFieldDecl.setTextNameAttribute( textNameAttributeValue );
		this.appendChild( textUserFieldDecl);
		return  textUserFieldDecl;      
	}
    
	/**
	 * Create child element {@odf.element text:user-field-decl}.
	 *
     * @param officeDateValueAttributeValue  the <code>String</code> value of <code>OfficeDateValueAttribute</code>, see {@odf.attribute  office:date-value} at specification
	 * @param officeValueTypeAttributeValue  the <code>String</code> value of <code>OfficeValueTypeAttribute</code>, see {@odf.attribute  office:value-type} at specification
	 * @param textNameAttributeValue  the <code>String</code> value of <code>TextNameAttribute</code>, see {@odf.attribute  text:name} at specification
	 * @return   return  the element {@odf.element text:user-field-decl}
	 * DifferentQName 
	 */
    
	public TextUserFieldDeclElement newTextUserFieldDeclElement(String officeDateValueAttributeValue, String officeValueTypeAttributeValue, String textNameAttributeValue)
	{
		TextUserFieldDeclElement  textUserFieldDecl = ((OdfFileDom)this.ownerDocument).newOdfElement(TextUserFieldDeclElement.class);
		textUserFieldDecl.setOfficeDateValueAttribute( officeDateValueAttributeValue );
		textUserFieldDecl.setOfficeValueTypeAttribute( officeValueTypeAttributeValue );
		textUserFieldDecl.setTextNameAttribute( textNameAttributeValue );
		this.appendChild( textUserFieldDecl);
		return  textUserFieldDecl;      
	}
    
	/**
	 * Create child element {@odf.element text:user-field-decl}.
	 *
     * @param officeTimeValueAttributeValue  the <code>String</code> value of <code>OfficeTimeValueAttribute</code>, see {@odf.attribute  office:time-value} at specification
	 * @param officeValueTypeAttributeValue  the <code>String</code> value of <code>OfficeValueTypeAttribute</code>, see {@odf.attribute  office:value-type} at specification
	 * @param textNameAttributeValue  the <code>String</code> value of <code>TextNameAttribute</code>, see {@odf.attribute  text:name} at specification
	 * @return   return  the element {@odf.element text:user-field-decl}
	 * DifferentQName office:time-value
	 */
    
	public TextUserFieldDeclElement newTextUserFieldDeclElementByTimeValue(String officeTimeValueAttributeValue, String officeValueTypeAttributeValue, String textNameAttributeValue)
	{
		TextUserFieldDeclElement  textUserFieldDecl = ((OdfFileDom)this.ownerDocument).newOdfElement(TextUserFieldDeclElement.class);
		textUserFieldDecl.setOfficeTimeValueAttribute( officeTimeValueAttributeValue );
		textUserFieldDecl.setOfficeValueTypeAttribute( officeValueTypeAttributeValue );
		textUserFieldDecl.setTextNameAttribute( textNameAttributeValue );
		this.appendChild( textUserFieldDecl);
		return  textUserFieldDecl;      
	}
    
	/**
	 * Create child element {@odf.element text:user-field-decl}.
	 *
     * @param officeBooleanValueAttributeValue  the <code>boolean</code> value of <code>OfficeBooleanValueAttribute</code>, see {@odf.attribute  office:boolean-value} at specification
	 * @param officeValueTypeAttributeValue  the <code>String</code> value of <code>OfficeValueTypeAttribute</code>, see {@odf.attribute  office:value-type} at specification
	 * @param textNameAttributeValue  the <code>String</code> value of <code>TextNameAttribute</code>, see {@odf.attribute  text:name} at specification
	 * @return   return  the element {@odf.element text:user-field-decl}
	 * DifferentQName 
	 */
    
	public TextUserFieldDeclElement newTextUserFieldDeclElement(boolean officeBooleanValueAttributeValue, String officeValueTypeAttributeValue, String textNameAttributeValue)
	{
		TextUserFieldDeclElement  textUserFieldDecl = ((OdfFileDom)this.ownerDocument).newOdfElement(TextUserFieldDeclElement.class);
		textUserFieldDecl.setOfficeBooleanValueAttribute( Boolean.valueOf(officeBooleanValueAttributeValue) );
		textUserFieldDecl.setOfficeValueTypeAttribute( officeValueTypeAttributeValue );
		textUserFieldDecl.setTextNameAttribute( textNameAttributeValue );
		this.appendChild( textUserFieldDecl);
		return  textUserFieldDecl;      
	}
    
	/**
	 * Create child element {@odf.element text:user-field-decl}.
	 *
     * @param officeValueTypeAttributeValue  the <code>String</code> value of <code>OfficeValueTypeAttribute</code>, see {@odf.attribute  office:value-type} at specification
	 * @param textNameAttributeValue  the <code>String</code> value of <code>TextNameAttribute</code>, see {@odf.attribute  text:name} at specification
	 * @return   return  the element {@odf.element text:user-field-decl}
	 * DifferentQName 
	 */
    
	public TextUserFieldDeclElement newTextUserFieldDeclElement(String officeValueTypeAttributeValue, String textNameAttributeValue)
	{
		TextUserFieldDeclElement  textUserFieldDecl = ((OdfFileDom)this.ownerDocument).newOdfElement(TextUserFieldDeclElement.class);
		textUserFieldDecl.setOfficeValueTypeAttribute( officeValueTypeAttributeValue );
		textUserFieldDecl.setTextNameAttribute( textNameAttributeValue );
		this.appendChild( textUserFieldDecl);
		return  textUserFieldDecl;      
	}
    
	/**
	 * Create child element {@odf.element text:dde-connection-decl}.
	 *
     * @param officeDdeApplicationAttributeValue  the <code>String</code> value of <code>OfficeDdeApplicationAttribute</code>, see {@odf.attribute  office:dde-application} at specification
	 * @param officeDdeItemAttributeValue  the <code>String</code> value of <code>OfficeDdeItemAttribute</code>, see {@odf.attribute  office:dde-item} at specification
	 * @param officeDdeTopicAttributeValue  the <code>String</code> value of <code>OfficeDdeTopicAttribute</code>, see {@odf.attribute  office:dde-topic} at specification
	 * @param officeNameAttributeValue  the <code>String</code> value of <code>OfficeNameAttribute</code>, see {@odf.attribute  office:name} at specification
	 * @return   return  the element {@odf.element text:dde-connection-decl}
	 * DifferentQName 
	 */
    
	public TextDdeConnectionDeclElement newTextDdeConnectionDeclElement(String officeDdeApplicationAttributeValue, String officeDdeItemAttributeValue, String officeDdeTopicAttributeValue, String officeNameAttributeValue)
	{
		TextDdeConnectionDeclElement  textDdeConnectionDecl = ((OdfFileDom)this.ownerDocument).newOdfElement(TextDdeConnectionDeclElement.class);
		textDdeConnectionDecl.setOfficeDdeApplicationAttribute( officeDdeApplicationAttributeValue );
		textDdeConnectionDecl.setOfficeDdeItemAttribute( officeDdeItemAttributeValue );
		textDdeConnectionDecl.setOfficeDdeTopicAttribute( officeDdeTopicAttributeValue );
		textDdeConnectionDecl.setOfficeNameAttribute( officeNameAttributeValue );
		this.appendChild( textDdeConnectionDecl);
		return  textDdeConnectionDecl;      
	}
    
	/**
	 * Create child element {@odf.element text:alphabetical-index-auto-mark-file}.
	 *
     * @param xlinkHrefAttributeValue  the <code>String</code> value of <code>XlinkHrefAttribute</code>, see {@odf.attribute  xlink:href} at specification
	 * @param xlinkTypeAttributeValue  the <code>String</code> value of <code>XlinkTypeAttribute</code>, see {@odf.attribute  xlink:type} at specification
	 * @return   return  the element {@odf.element text:alphabetical-index-auto-mark-file}
	 * DifferentQName 
	 */
    
	public TextAlphabeticalIndexAutoMarkFileElement newTextAlphabeticalIndexAutoMarkFileElement(String xlinkHrefAttributeValue, String xlinkTypeAttributeValue)
	{
		TextAlphabeticalIndexAutoMarkFileElement  textAlphabeticalIndexAutoMarkFile = ((OdfFileDom)this.ownerDocument).newOdfElement(TextAlphabeticalIndexAutoMarkFileElement.class);
		textAlphabeticalIndexAutoMarkFile.setXlinkHrefAttribute( xlinkHrefAttributeValue );
		textAlphabeticalIndexAutoMarkFile.setXlinkTypeAttribute( xlinkTypeAttributeValue );
		this.appendChild( textAlphabeticalIndexAutoMarkFile);
		return  textAlphabeticalIndexAutoMarkFile;      
	}
    
	/**
	 * Create child element {@odf.element table:calculation-settings}.
	 *
	 * @return   return  the element {@odf.element table:calculation-settings}
	 * DifferentQName 
	 */
	public TableCalculationSettingsElement newTableCalculationSettingsElement()
	{
		TableCalculationSettingsElement  tableCalculationSettings = ((OdfFileDom)this.ownerDocument).newOdfElement(TableCalculationSettingsElement.class);
		this.appendChild( tableCalculationSettings);
		return  tableCalculationSettings;
	}                   
               
	/**
	 * Create child element {@odf.element table:content-validations}.
	 *
	 * @return   return  the element {@odf.element table:content-validations}
	 * DifferentQName 
	 */
	public TableContentValidationsElement newTableContentValidationsElement()
	{
		TableContentValidationsElement  tableContentValidations = ((OdfFileDom)this.ownerDocument).newOdfElement(TableContentValidationsElement.class);
		this.appendChild( tableContentValidations);
		return  tableContentValidations;
	}                   
               
	/**
	 * Create child element {@odf.element table:label-ranges}.
	 *
	 * @return   return  the element {@odf.element table:label-ranges}
	 * DifferentQName 
	 */
	public TableLabelRangesElement newTableLabelRangesElement()
	{
		TableLabelRangesElement  tableLabelRanges = ((OdfFileDom)this.ownerDocument).newOdfElement(TableLabelRangesElement.class);
		this.appendChild( tableLabelRanges);
		return  tableLabelRanges;
	}                   
               
	/**
	 * Create child element {@odf.element table:table}.
	 *
	 * @return   return  the element {@odf.element table:table}
	 * DifferentQName 
	 */
	public TableTableElement newTableTableElement()
	{
		TableTableElement  tableTable = ((OdfFileDom)this.ownerDocument).newOdfElement(TableTableElement.class);
		this.appendChild( tableTable);
		return  tableTable;
	}                   
               
	/**
	 * Create child element {@odf.element table:named-expressions}.
	 *
	 * @return   return  the element {@odf.element table:named-expressions}
	 * DifferentQName 
	 */
	public TableNamedExpressionsElement newTableNamedExpressionsElement()
	{
		TableNamedExpressionsElement  tableNamedExpressions = ((OdfFileDom)this.ownerDocument).newOdfElement(TableNamedExpressionsElement.class);
		this.appendChild( tableNamedExpressions);
		return  tableNamedExpressions;
	}                   
               
	/**
	 * Create child element {@odf.element table:database-ranges}.
	 *
	 * @return   return  the element {@odf.element table:database-ranges}
	 * DifferentQName 
	 */
	public TableDatabaseRangesElement newTableDatabaseRangesElement()
	{
		TableDatabaseRangesElement  tableDatabaseRanges = ((OdfFileDom)this.ownerDocument).newOdfElement(TableDatabaseRangesElement.class);
		this.appendChild( tableDatabaseRanges);
		return  tableDatabaseRanges;
	}                   
               
	/**
	 * Create child element {@odf.element table:data-pilot-tables}.
	 *
	 * @return   return  the element {@odf.element table:data-pilot-tables}
	 * DifferentQName 
	 */
	public TableDataPilotTablesElement newTableDataPilotTablesElement()
	{
		TableDataPilotTablesElement  tableDataPilotTables = ((OdfFileDom)this.ownerDocument).newOdfElement(TableDataPilotTablesElement.class);
		this.appendChild( tableDataPilotTables);
		return  tableDataPilotTables;
	}                   
               
	/**
	 * Create child element {@odf.element table:consolidation}.
	 *
     * @param tableFunctionAttributeValue  the <code>String</code> value of <code>TableFunctionAttribute</code>, see {@odf.attribute  table:function} at specification
	 * @param tableSourceCellRangeAddressesAttributeValue  the <code>String</code> value of <code>TableSourceCellRangeAddressesAttribute</code>, see {@odf.attribute  table:source-cell-range-addresses} at specification
	 * @param tableTargetCellAddressAttributeValue  the <code>String</code> value of <code>TableTargetCellAddressAttribute</code>, see {@odf.attribute  table:target-cell-address} at specification
	 * @return   return  the element {@odf.element table:consolidation}
	 * DifferentQName 
	 */
    
	public TableConsolidationElement newTableConsolidationElement(String tableFunctionAttributeValue, String tableSourceCellRangeAddressesAttributeValue, String tableTargetCellAddressAttributeValue)
	{
		TableConsolidationElement  tableConsolidation = ((OdfFileDom)this.ownerDocument).newOdfElement(TableConsolidationElement.class);
		tableConsolidation.setTableFunctionAttribute( tableFunctionAttributeValue );
		tableConsolidation.setTableSourceCellRangeAddressesAttribute( tableSourceCellRangeAddressesAttributeValue );
		tableConsolidation.setTableTargetCellAddressAttribute( tableTargetCellAddressAttributeValue );
		this.appendChild( tableConsolidation);
		return  tableConsolidation;      
	}
    
	/**
	 * Create child element {@odf.element table:dde-links}.
	 *
	 * @return   return  the element {@odf.element table:dde-links}
	 * DifferentQName 
	 */
	public TableDdeLinksElement newTableDdeLinksElement()
	{
		TableDdeLinksElement  tableDdeLinks = ((OdfFileDom)this.ownerDocument).newOdfElement(TableDdeLinksElement.class);
		this.appendChild( tableDdeLinks);
		return  tableDdeLinks;
	}                   
               
}
