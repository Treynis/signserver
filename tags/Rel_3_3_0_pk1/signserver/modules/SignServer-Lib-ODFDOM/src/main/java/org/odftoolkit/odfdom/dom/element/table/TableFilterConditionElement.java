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
import org.odftoolkit.odfdom.dom.attribute.table.TableFieldNumberAttribute;
import org.odftoolkit.odfdom.dom.attribute.table.TableValueAttribute;
import org.odftoolkit.odfdom.dom.attribute.table.TableOperatorAttribute;
import org.odftoolkit.odfdom.dom.attribute.table.TableCaseSensitiveAttribute;
import org.odftoolkit.odfdom.dom.attribute.table.TableDataTypeAttribute;


/**
 * DOM implementation of OpenDocument element  {@odf.element table:filter-condition}.
 *
 */
public abstract class TableFilterConditionElement extends OdfElement
{        
    public static final OdfName ELEMENT_NAME = OdfName.get( OdfNamespace.get(OdfNamespaceNames.TABLE), "filter-condition" );

	/**
	 * The value set of {@odf.attribute table:data-type}.
	 */
	 public enum TableDataTypeAttributeValue {
	 
	 NUMBER( TableDataTypeAttribute.Value.NUMBER.toString() ), TEXT( TableDataTypeAttribute.Value.TEXT.toString() );
              
		private String mValue;
	 	
		TableDataTypeAttributeValue( String value )
		{
			mValue = value;
		}
		
		@Override
		public String toString()
		{
			return mValue;
		}
		
		public static TableDataTypeAttributeValue enumValueOf( String value )
	    {
	        for( TableDataTypeAttributeValue aIter : values() )
	        {
	            if( value.equals( aIter.toString() ) )
	            {
	                return aIter;
	            }
	        }
	        return null;
	    }
	}
	public static final String  DEFAULT_VALUE_TABLE_CASESENSITIVE_ATTRIBUTE = TableCaseSensitiveAttribute.DEFAULT_VALUE_FALSE;

	/**
	 * Create the instance of <code>TableFilterConditionElement</code> 
	 *
	 * @param  ownerDoc     The type is <code>OdfFileDom</code>
	 */
	public TableFilterConditionElement( OdfFileDom ownerDoc )
	{
		super( ownerDoc, ELEMENT_NAME	);
	}

	/**
	 * Get the element name 
	 *
	 * @return  return   <code>OdfName</code> the name of element {@odf.element table:filter-condition}.
	 */
	public OdfName getOdfName()
	{
		return ELEMENT_NAME;
	}

	/**
	 * Initialization of the mandatory attributes of {@link  TableFilterConditionElement}
	 *
     * @param tableFieldNumberAttributeValue  The mandatory attribute {@odf.attribute  table:field-number}"
     * @param tableValueAttributeValue  The mandatory attribute {@odf.attribute  table:value}"
     * @param tableOperatorAttributeValue  The mandatory attribute {@odf.attribute  table:operator}"
     *
	 */
	public void init(int tableFieldNumberAttributeValue, String tableValueAttributeValue, String tableOperatorAttributeValue)
	{
		setTableFieldNumberAttribute( Integer.valueOf(tableFieldNumberAttributeValue) );
		setTableOperatorAttribute( tableOperatorAttributeValue );
	}

	/**
	 * Receives the value of the ODFDOM attribute representation <code>TableFieldNumberAttribute</code> , See {@odf.attribute table:field-number}
	 *
	 * @return - the <code>Integer</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public Integer getTableFieldNumberAttribute()
	{
		TableFieldNumberAttribute attr = (TableFieldNumberAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.TABLE), "field-number" ) );
		if( attr != null ){
			return Integer.valueOf( attr.intValue() );
		}
		return null;
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>TableFieldNumberAttribute</code> , See {@odf.attribute table:field-number}
	 *
	 * @param tableFieldNumberValue   The type is <code>Integer</code>
	 */
	public void setTableFieldNumberAttribute( Integer tableFieldNumberValue )
	{
		TableFieldNumberAttribute attr =  new TableFieldNumberAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setIntValue( tableFieldNumberValue.intValue() );
	}


	/**
	 * Receives the value of the ODFDOM attribute representation <code>TableValueAttribute</code> , See {@odf.attribute table:value}
	 *
	 * @return - the <code>String</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public String getTableValueAttribute()
	{
		TableValueAttribute attr = (TableValueAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.TABLE), "value" ) );
		if( attr != null ){
			return String.valueOf( attr.getValue() );
		}
		return null;
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>TableValueAttribute</code> , See {@odf.attribute table:value}
	 *
	 * @param tableValueValue   The type is <code>String</code>
	 */
	public void setTableValueAttribute( String tableValueValue )
	{
		TableValueAttribute attr =  new TableValueAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setValue( tableValueValue );
	}


	/**
	 * Receives the value of the ODFDOM attribute representation <code>TableOperatorAttribute</code> , See {@odf.attribute table:operator}
	 *
	 * @return - the <code>String</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public String getTableOperatorAttribute()
	{
		TableOperatorAttribute attr = (TableOperatorAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.TABLE), "operator" ) );
		if( attr != null ){
			return String.valueOf( attr.getValue() );
		}
		return null;
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>TableOperatorAttribute</code> , See {@odf.attribute table:operator}
	 *
	 * @param tableOperatorValue   The type is <code>String</code>
	 */
	public void setTableOperatorAttribute( String tableOperatorValue )
	{
		TableOperatorAttribute attr =  new TableOperatorAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setValue( tableOperatorValue );
	}


	/**
	 * Receives the value of the ODFDOM attribute representation <code>TableCaseSensitiveAttribute</code> , See {@odf.attribute table:case-sensitive}
	 *
	 * @return - the <code>String</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public String getTableCaseSensitiveAttribute()
	{
		TableCaseSensitiveAttribute attr = (TableCaseSensitiveAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.TABLE), "case-sensitive" ) );
		if( attr != null ){
			return String.valueOf( attr.getValue() );
		}
		return DEFAULT_VALUE_TABLE_CASESENSITIVE_ATTRIBUTE;
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>TableCaseSensitiveAttribute</code> , See {@odf.attribute table:case-sensitive}
	 *
	 * @param tableCaseSensitiveValue   The type is <code>String</code>
	 */
	public void setTableCaseSensitiveAttribute( String tableCaseSensitiveValue )
	{
		TableCaseSensitiveAttribute attr =  new TableCaseSensitiveAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setValue( tableCaseSensitiveValue );
	}


	/**
	 * Receives the value of the ODFDOM attribute representation <code>TableDataTypeAttribute</code> , See {@odf.attribute table:data-type}
	 *
	 * @return - the <code>String</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public String getTableDataTypeAttribute()
	{
		TableDataTypeAttribute attr = (TableDataTypeAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.TABLE), "data-type" ) );
		if( attr != null ){
			return String.valueOf( attr.getValue() );
		}
		return null;
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>TableDataTypeAttribute</code> , See {@odf.attribute table:data-type}
	 *
	 * @param tableDataTypeValue   The type is <code>String</code>
	 */
	public void setTableDataTypeAttribute( String tableDataTypeValue )
	{
		TableDataTypeAttribute attr =  new TableDataTypeAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setValue( tableDataTypeValue );
	}

	/**
	 * Create child element {@odf.element table:filter-set-item}.
	 *
     * @param tableValueAttributeValue  the <code>String</code> value of <code>TableValueAttribute</code>, see {@odf.attribute  table:value} at specification
	 * @return   return  the element {@odf.element table:filter-set-item}
	 * DifferentQName 
	 */
    
	public TableFilterSetItemElement newTableFilterSetItemElement(String tableValueAttributeValue)
	{
		TableFilterSetItemElement  tableFilterSetItem = ((OdfFileDom)this.ownerDocument).newOdfElement(TableFilterSetItemElement.class);
		tableFilterSetItem.setTableValueAttribute( tableValueAttributeValue );
		this.appendChild( tableFilterSetItem);
		return  tableFilterSetItem;      
	}
    
}
