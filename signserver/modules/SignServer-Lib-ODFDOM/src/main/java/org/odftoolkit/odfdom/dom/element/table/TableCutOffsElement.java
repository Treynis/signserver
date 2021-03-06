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


/**
 * DOM implementation of OpenDocument element  {@odf.element table:cut-offs}.
 *
 */
public abstract class TableCutOffsElement extends OdfElement
{        
    public static final OdfName ELEMENT_NAME = OdfName.get( OdfNamespace.get(OdfNamespaceNames.TABLE), "cut-offs" );


	/**
	 * Create the instance of <code>TableCutOffsElement</code> 
	 *
	 * @param  ownerDoc     The type is <code>OdfFileDom</code>
	 */
	public TableCutOffsElement( OdfFileDom ownerDoc )
	{
		super( ownerDoc, ELEMENT_NAME	);
	}

	/**
	 * Get the element name 
	 *
	 * @return  return   <code>OdfName</code> the name of element {@odf.element table:cut-offs}.
	 */
	public OdfName getOdfName()
	{
		return ELEMENT_NAME;
	}


	/**
	 * Create child element {@odf.element table:movement-cut-off}.
	 *
     * @param tablePositionAttributeValue  the <code>int</code> value of <code>TablePositionAttribute</code>, see {@odf.attribute  table:position} at specification
	 * @return   return  the element {@odf.element table:movement-cut-off}
	 * DifferentQName 
	 */
    
	public TableMovementCutOffElement newTableMovementCutOffElement(int tablePositionAttributeValue)
	{
		TableMovementCutOffElement  tableMovementCutOff = ((OdfFileDom)this.ownerDocument).newOdfElement(TableMovementCutOffElement.class);
		tableMovementCutOff.setTablePositionAttribute( Integer.valueOf(tablePositionAttributeValue) );
		this.appendChild( tableMovementCutOff);
		return  tableMovementCutOff;      
	}
    
	/**
	 * Create child element {@odf.element table:movement-cut-off}.
	 *
     * @param tableEndPositionAttributeValue  the <code>int</code> value of <code>TableEndPositionAttribute</code>, see {@odf.attribute  table:end-position} at specification
	 * @param tableStartPositionAttributeValue  the <code>int</code> value of <code>TableStartPositionAttribute</code>, see {@odf.attribute  table:start-position} at specification
	 * @return   return  the element {@odf.element table:movement-cut-off}
	 * DifferentQName 
	 */
    
	public TableMovementCutOffElement newTableMovementCutOffElement(int tableEndPositionAttributeValue, int tableStartPositionAttributeValue)
	{
		TableMovementCutOffElement  tableMovementCutOff = ((OdfFileDom)this.ownerDocument).newOdfElement(TableMovementCutOffElement.class);
		tableMovementCutOff.setTableEndPositionAttribute( Integer.valueOf(tableEndPositionAttributeValue) );
		tableMovementCutOff.setTableStartPositionAttribute( Integer.valueOf(tableStartPositionAttributeValue) );
		this.appendChild( tableMovementCutOff);
		return  tableMovementCutOff;      
	}
    
	/**
	 * Create child element {@odf.element table:insertion-cut-off}.
	 *
     * @param tableIdAttributeValue  the <code>String</code> value of <code>TableIdAttribute</code>, see {@odf.attribute  table:id} at specification
	 * @param tablePositionAttributeValue  the <code>int</code> value of <code>TablePositionAttribute</code>, see {@odf.attribute  table:position} at specification
	 * @return   return  the element {@odf.element table:insertion-cut-off}
	 * DifferentQName 
	 */
    
	public TableInsertionCutOffElement newTableInsertionCutOffElement(String tableIdAttributeValue, int tablePositionAttributeValue)
	{
		TableInsertionCutOffElement  tableInsertionCutOff = ((OdfFileDom)this.ownerDocument).newOdfElement(TableInsertionCutOffElement.class);
		tableInsertionCutOff.setTableIdAttribute( tableIdAttributeValue );
		tableInsertionCutOff.setTablePositionAttribute( Integer.valueOf(tablePositionAttributeValue) );
		this.appendChild( tableInsertionCutOff);
		return  tableInsertionCutOff;      
	}
    
}
