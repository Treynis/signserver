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

package org.odftoolkit.odfdom.dom.element.presentation;

import org.odftoolkit.odfdom.OdfName;
import org.odftoolkit.odfdom.OdfNamespace;
import org.odftoolkit.odfdom.OdfFileDom;
import org.odftoolkit.odfdom.dom.OdfNamespaceNames;
import org.odftoolkit.odfdom.OdfElement;
import org.odftoolkit.odfdom.dom.attribute.draw.DrawShapeIdAttribute;
import org.odftoolkit.odfdom.dom.attribute.presentation.PresentationSpeedAttribute;


/**
 * DOM implementation of OpenDocument element  {@odf.element presentation:play}.
 *
 */
public abstract class PresentationPlayElement extends OdfElement
{        
    public static final OdfName ELEMENT_NAME = OdfName.get( OdfNamespace.get(OdfNamespaceNames.PRESENTATION), "play" );


	/**
	 * Create the instance of <code>PresentationPlayElement</code> 
	 *
	 * @param  ownerDoc     The type is <code>OdfFileDom</code>
	 */
	public PresentationPlayElement( OdfFileDom ownerDoc )
	{
		super( ownerDoc, ELEMENT_NAME	);
	}

	/**
	 * Get the element name 
	 *
	 * @return  return   <code>OdfName</code> the name of element {@odf.element presentation:play}.
	 */
	public OdfName getOdfName()
	{
		return ELEMENT_NAME;
	}

	/**
	 * Initialization of the mandatory attributes of {@link  PresentationPlayElement}
	 *
     * @param drawShapeIdAttributeValue  The mandatory attribute {@odf.attribute  draw:shape-id}"
     *
	 */
	public void init(String drawShapeIdAttributeValue)
	{
		setDrawShapeIdAttribute( drawShapeIdAttributeValue );
	}

	/**
	 * Receives the value of the ODFDOM attribute representation <code>DrawShapeIdAttribute</code> , See {@odf.attribute draw:shape-id}
	 *
	 * @return - the <code>String</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public String getDrawShapeIdAttribute()
	{
		DrawShapeIdAttribute attr = (DrawShapeIdAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.DRAW), "shape-id" ) );
		if( attr != null ){
			return String.valueOf( attr.getValue() );
		}
		return null;
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>DrawShapeIdAttribute</code> , See {@odf.attribute draw:shape-id}
	 *
	 * @param drawShapeIdValue   The type is <code>String</code>
	 */
	public void setDrawShapeIdAttribute( String drawShapeIdValue )
	{
		DrawShapeIdAttribute attr =  new DrawShapeIdAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setValue( drawShapeIdValue );
	}


	/**
	 * Receives the value of the ODFDOM attribute representation <code>PresentationSpeedAttribute</code> , See {@odf.attribute presentation:speed}
	 *
	 * @return - the <code>String</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public String getPresentationSpeedAttribute()
	{
		PresentationSpeedAttribute attr = (PresentationSpeedAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.PRESENTATION), "speed" ) );
		if( attr != null ){
			return String.valueOf( attr.getValue() );
		}
		return PresentationSpeedAttribute.DEFAULT_VALUE;
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>PresentationSpeedAttribute</code> , See {@odf.attribute presentation:speed}
	 *
	 * @param presentationSpeedValue   The type is <code>String</code>
	 */
	public void setPresentationSpeedAttribute( String presentationSpeedValue )
	{
		PresentationSpeedAttribute attr =  new PresentationSpeedAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setValue( presentationSpeedValue );
	}

}
