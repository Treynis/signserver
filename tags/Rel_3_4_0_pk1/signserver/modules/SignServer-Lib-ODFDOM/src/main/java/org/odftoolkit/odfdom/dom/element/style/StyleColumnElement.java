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

package org.odftoolkit.odfdom.dom.element.style;

import org.odftoolkit.odfdom.OdfName;
import org.odftoolkit.odfdom.OdfNamespace;
import org.odftoolkit.odfdom.OdfFileDom;
import org.odftoolkit.odfdom.dom.OdfNamespaceNames;
import org.odftoolkit.odfdom.OdfElement;
import org.odftoolkit.odfdom.dom.attribute.style.StyleRelWidthAttribute;
import org.odftoolkit.odfdom.dom.attribute.fo.FoStartIndentAttribute;
import org.odftoolkit.odfdom.dom.attribute.fo.FoEndIndentAttribute;
import org.odftoolkit.odfdom.dom.attribute.fo.FoSpaceBeforeAttribute;
import org.odftoolkit.odfdom.dom.attribute.fo.FoSpaceAfterAttribute;


/**
 * DOM implementation of OpenDocument element  {@odf.element style:column}.
 *
 */
public abstract class StyleColumnElement extends OdfElement
{        
    public static final OdfName ELEMENT_NAME = OdfName.get( OdfNamespace.get(OdfNamespaceNames.STYLE), "column" );


	/**
	 * Create the instance of <code>StyleColumnElement</code> 
	 *
	 * @param  ownerDoc     The type is <code>OdfFileDom</code>
	 */
	public StyleColumnElement( OdfFileDom ownerDoc )
	{
		super( ownerDoc, ELEMENT_NAME	);
	}

	/**
	 * Get the element name 
	 *
	 * @return  return   <code>OdfName</code> the name of element {@odf.element style:column}.
	 */
	public OdfName getOdfName()
	{
		return ELEMENT_NAME;
	}

	/**
	 * Initialization of the mandatory attributes of {@link  StyleColumnElement}
	 *
     * @param styleRelWidthAttributeValue  The mandatory attribute {@odf.attribute  style:rel-width}"
     *
	 */
	public void init(String styleRelWidthAttributeValue)
	{
	}

	/**
	 * Receives the value of the ODFDOM attribute representation <code>StyleRelWidthAttribute</code> , See {@odf.attribute style:rel-width}
	 *
	 * @return - the <code>String</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public String getStyleRelWidthAttribute()
	{
		StyleRelWidthAttribute attr = (StyleRelWidthAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.STYLE), "rel-width" ) );
		if( attr != null ){
			return String.valueOf( attr.getValue() );
		}
		return null;
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>StyleRelWidthAttribute</code> , See {@odf.attribute style:rel-width}
	 *
	 * @param styleRelWidthValue   The type is <code>String</code>
	 */
	public void setStyleRelWidthAttribute( String styleRelWidthValue )
	{
		StyleRelWidthAttribute attr =  new StyleRelWidthAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setValue( styleRelWidthValue );
	}


	/**
	 * Receives the value of the ODFDOM attribute representation <code>FoStartIndentAttribute</code> , See {@odf.attribute fo:start-indent}
	 *
	 * @return - the <code>String</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public String getFoStartIndentAttribute()
	{
		FoStartIndentAttribute attr = (FoStartIndentAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.FO), "start-indent" ) );
		if( attr != null ){
			return String.valueOf( attr.getValue() );
		}
		return FoStartIndentAttribute.DEFAULT_VALUE;
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>FoStartIndentAttribute</code> , See {@odf.attribute fo:start-indent}
	 *
	 * @param foStartIndentValue   The type is <code>String</code>
	 */
	public void setFoStartIndentAttribute( String foStartIndentValue )
	{
		FoStartIndentAttribute attr =  new FoStartIndentAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setValue( foStartIndentValue );
	}


	/**
	 * Receives the value of the ODFDOM attribute representation <code>FoEndIndentAttribute</code> , See {@odf.attribute fo:end-indent}
	 *
	 * @return - the <code>String</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public String getFoEndIndentAttribute()
	{
		FoEndIndentAttribute attr = (FoEndIndentAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.FO), "end-indent" ) );
		if( attr != null ){
			return String.valueOf( attr.getValue() );
		}
		return FoEndIndentAttribute.DEFAULT_VALUE;
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>FoEndIndentAttribute</code> , See {@odf.attribute fo:end-indent}
	 *
	 * @param foEndIndentValue   The type is <code>String</code>
	 */
	public void setFoEndIndentAttribute( String foEndIndentValue )
	{
		FoEndIndentAttribute attr =  new FoEndIndentAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setValue( foEndIndentValue );
	}


	/**
	 * Receives the value of the ODFDOM attribute representation <code>FoSpaceBeforeAttribute</code> , See {@odf.attribute fo:space-before}
	 *
	 * @return - the <code>String</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public String getFoSpaceBeforeAttribute()
	{
		FoSpaceBeforeAttribute attr = (FoSpaceBeforeAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.FO), "space-before" ) );
		if( attr != null ){
			return String.valueOf( attr.getValue() );
		}
		return FoSpaceBeforeAttribute.DEFAULT_VALUE;
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>FoSpaceBeforeAttribute</code> , See {@odf.attribute fo:space-before}
	 *
	 * @param foSpaceBeforeValue   The type is <code>String</code>
	 */
	public void setFoSpaceBeforeAttribute( String foSpaceBeforeValue )
	{
		FoSpaceBeforeAttribute attr =  new FoSpaceBeforeAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setValue( foSpaceBeforeValue );
	}


	/**
	 * Receives the value of the ODFDOM attribute representation <code>FoSpaceAfterAttribute</code> , See {@odf.attribute fo:space-after}
	 *
	 * @return - the <code>String</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public String getFoSpaceAfterAttribute()
	{
		FoSpaceAfterAttribute attr = (FoSpaceAfterAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.FO), "space-after" ) );
		if( attr != null ){
			return String.valueOf( attr.getValue() );
		}
		return FoSpaceAfterAttribute.DEFAULT_VALUE;
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>FoSpaceAfterAttribute</code> , See {@odf.attribute fo:space-after}
	 *
	 * @param foSpaceAfterValue   The type is <code>String</code>
	 */
	public void setFoSpaceAfterAttribute( String foSpaceAfterValue )
	{
		FoSpaceAfterAttribute attr =  new FoSpaceAfterAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setValue( foSpaceAfterValue );
	}

}
