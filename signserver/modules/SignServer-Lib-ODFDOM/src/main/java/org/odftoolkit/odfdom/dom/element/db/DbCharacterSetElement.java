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
import org.odftoolkit.odfdom.dom.attribute.db.DbEncodingAttribute;


/**
 * DOM implementation of OpenDocument element  {@odf.element db:character-set}.
 *
 */
public abstract class DbCharacterSetElement extends OdfElement
{        
    public static final OdfName ELEMENT_NAME = OdfName.get( OdfNamespace.get(OdfNamespaceNames.DB), "character-set" );


	/**
	 * Create the instance of <code>DbCharacterSetElement</code> 
	 *
	 * @param  ownerDoc     The type is <code>OdfFileDom</code>
	 */
	public DbCharacterSetElement( OdfFileDom ownerDoc )
	{
		super( ownerDoc, ELEMENT_NAME	);
	}

	/**
	 * Get the element name 
	 *
	 * @return  return   <code>OdfName</code> the name of element {@odf.element db:character-set}.
	 */
	public OdfName getOdfName()
	{
		return ELEMENT_NAME;
	}



	/**
	 * Receives the value of the ODFDOM attribute representation <code>DbEncodingAttribute</code> , See {@odf.attribute db:encoding}
	 *
	 * @return - the <code>String</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public String getDbEncodingAttribute()
	{
		DbEncodingAttribute attr = (DbEncodingAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.DB), "encoding" ) );
		if( attr != null ){
			return String.valueOf( attr.getValue() );
		}
		return DbEncodingAttribute.DEFAULT_VALUE;
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>DbEncodingAttribute</code> , See {@odf.attribute db:encoding}
	 *
	 * @param dbEncodingValue   The type is <code>String</code>
	 */
	public void setDbEncodingAttribute( String dbEncodingValue )
	{
		DbEncodingAttribute attr =  new DbEncodingAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setValue( dbEncodingValue );
	}

}
