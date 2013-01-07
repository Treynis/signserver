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
import org.odftoolkit.odfdom.dom.attribute.presentation.PresentationNameAttribute;
import org.odftoolkit.odfdom.dom.attribute.presentation.PresentationSourceAttribute;
import org.odftoolkit.odfdom.dom.attribute.style.StyleDataStyleNameAttribute;


/**
 * DOM implementation of OpenDocument element  {@odf.element presentation:date-time-decl}.
 *
 */
public abstract class PresentationDateTimeDeclElement extends OdfElement
{        
    public static final OdfName ELEMENT_NAME = OdfName.get( OdfNamespace.get(OdfNamespaceNames.PRESENTATION), "date-time-decl" );


	/**
	 * Create the instance of <code>PresentationDateTimeDeclElement</code> 
	 *
	 * @param  ownerDoc     The type is <code>OdfFileDom</code>
	 */
	public PresentationDateTimeDeclElement( OdfFileDom ownerDoc )
	{
		super( ownerDoc, ELEMENT_NAME	);
	}

	/**
	 * Get the element name 
	 *
	 * @return  return   <code>OdfName</code> the name of element {@odf.element presentation:date-time-decl}.
	 */
	public OdfName getOdfName()
	{
		return ELEMENT_NAME;
	}

	/**
	 * Initialization of the mandatory attributes of {@link  PresentationDateTimeDeclElement}
	 *
     * @param presentationNameAttributeValue  The mandatory attribute {@odf.attribute  presentation:name}"
     * @param presentationSourceAttributeValue  The mandatory attribute {@odf.attribute  presentation:source}"
     *
	 */
	public void init(String presentationNameAttributeValue, String presentationSourceAttributeValue)
	{
		setPresentationNameAttribute( presentationNameAttributeValue );
		setPresentationSourceAttribute( presentationSourceAttributeValue );
	}

	/**
	 * Receives the value of the ODFDOM attribute representation <code>PresentationNameAttribute</code> , See {@odf.attribute presentation:name}
	 *
	 * @return - the <code>String</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public String getPresentationNameAttribute()
	{
		PresentationNameAttribute attr = (PresentationNameAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.PRESENTATION), "name" ) );
		if( attr != null ){
			return String.valueOf( attr.getValue() );
		}
		return null;
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>PresentationNameAttribute</code> , See {@odf.attribute presentation:name}
	 *
	 * @param presentationNameValue   The type is <code>String</code>
	 */
	public void setPresentationNameAttribute( String presentationNameValue )
	{
		PresentationNameAttribute attr =  new PresentationNameAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setValue( presentationNameValue );
	}


	/**
	 * Receives the value of the ODFDOM attribute representation <code>PresentationSourceAttribute</code> , See {@odf.attribute presentation:source}
	 *
	 * @return - the <code>String</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public String getPresentationSourceAttribute()
	{
		PresentationSourceAttribute attr = (PresentationSourceAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.PRESENTATION), "source" ) );
		if( attr != null ){
			return String.valueOf( attr.getValue() );
		}
		return null;
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>PresentationSourceAttribute</code> , See {@odf.attribute presentation:source}
	 *
	 * @param presentationSourceValue   The type is <code>String</code>
	 */
	public void setPresentationSourceAttribute( String presentationSourceValue )
	{
		PresentationSourceAttribute attr =  new PresentationSourceAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setValue( presentationSourceValue );
	}


	/**
	 * Receives the value of the ODFDOM attribute representation <code>StyleDataStyleNameAttribute</code> , See {@odf.attribute style:data-style-name}
	 *
	 * @return - the <code>String</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public String getStyleDataStyleNameAttribute()
	{
		StyleDataStyleNameAttribute attr = (StyleDataStyleNameAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.STYLE), "data-style-name" ) );
		if( attr != null ){
			return String.valueOf( attr.getValue() );
		}
		return null;
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>StyleDataStyleNameAttribute</code> , See {@odf.attribute style:data-style-name}
	 *
	 * @param styleDataStyleNameValue   The type is <code>String</code>
	 */
	public void setStyleDataStyleNameAttribute( String styleDataStyleNameValue )
	{
		StyleDataStyleNameAttribute attr =  new StyleDataStyleNameAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setValue( styleDataStyleNameValue );
	}

}
