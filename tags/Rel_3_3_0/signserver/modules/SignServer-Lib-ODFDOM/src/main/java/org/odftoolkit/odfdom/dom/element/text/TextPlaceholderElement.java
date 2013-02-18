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

package org.odftoolkit.odfdom.dom.element.text;

import org.odftoolkit.odfdom.OdfName;
import org.odftoolkit.odfdom.OdfNamespace;
import org.odftoolkit.odfdom.OdfFileDom;
import org.odftoolkit.odfdom.dom.OdfNamespaceNames;
import org.odftoolkit.odfdom.OdfElement;
import org.odftoolkit.odfdom.dom.attribute.text.TextPlaceholderTypeAttribute;
import org.odftoolkit.odfdom.dom.attribute.text.TextDescriptionAttribute;


/**
 * DOM implementation of OpenDocument element  {@odf.element text:placeholder}.
 *
 */
public abstract class TextPlaceholderElement extends OdfElement
{        
    public static final OdfName ELEMENT_NAME = OdfName.get( OdfNamespace.get(OdfNamespaceNames.TEXT), "placeholder" );


	/**
	 * Create the instance of <code>TextPlaceholderElement</code> 
	 *
	 * @param  ownerDoc     The type is <code>OdfFileDom</code>
	 */
	public TextPlaceholderElement( OdfFileDom ownerDoc )
	{
		super( ownerDoc, ELEMENT_NAME	);
	}

	/**
	 * Get the element name 
	 *
	 * @return  return   <code>OdfName</code> the name of element {@odf.element text:placeholder}.
	 */
	public OdfName getOdfName()
	{
		return ELEMENT_NAME;
	}

	/**
	 * Initialization of the mandatory attributes of {@link  TextPlaceholderElement}
	 *
     * @param textPlaceholderTypeAttributeValue  The mandatory attribute {@odf.attribute  text:placeholder-type}"
     *
	 */
	public void init(String textPlaceholderTypeAttributeValue)
	{
		setTextPlaceholderTypeAttribute( textPlaceholderTypeAttributeValue );
	}

	/**
	 * Receives the value of the ODFDOM attribute representation <code>TextPlaceholderTypeAttribute</code> , See {@odf.attribute text:placeholder-type}
	 *
	 * @return - the <code>String</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public String getTextPlaceholderTypeAttribute()
	{
		TextPlaceholderTypeAttribute attr = (TextPlaceholderTypeAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.TEXT), "placeholder-type" ) );
		if( attr != null ){
			return String.valueOf( attr.getValue() );
		}
		return null;
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>TextPlaceholderTypeAttribute</code> , See {@odf.attribute text:placeholder-type}
	 *
	 * @param textPlaceholderTypeValue   The type is <code>String</code>
	 */
	public void setTextPlaceholderTypeAttribute( String textPlaceholderTypeValue )
	{
		TextPlaceholderTypeAttribute attr =  new TextPlaceholderTypeAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setValue( textPlaceholderTypeValue );
	}


	/**
	 * Receives the value of the ODFDOM attribute representation <code>TextDescriptionAttribute</code> , See {@odf.attribute text:description}
	 *
	 * @return - the <code>String</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public String getTextDescriptionAttribute()
	{
		TextDescriptionAttribute attr = (TextDescriptionAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.TEXT), "description" ) );
		if( attr != null ){
			return String.valueOf( attr.getValue() );
		}
		return null;
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>TextDescriptionAttribute</code> , See {@odf.attribute text:description}
	 *
	 * @param textDescriptionValue   The type is <code>String</code>
	 */
	public void setTextDescriptionAttribute( String textDescriptionValue )
	{
		TextDescriptionAttribute attr =  new TextDescriptionAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setValue( textDescriptionValue );
	}

}
