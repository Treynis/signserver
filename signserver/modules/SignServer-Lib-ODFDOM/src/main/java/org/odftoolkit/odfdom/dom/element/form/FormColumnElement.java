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

package org.odftoolkit.odfdom.dom.element.form;

import org.odftoolkit.odfdom.OdfName;
import org.odftoolkit.odfdom.OdfNamespace;
import org.odftoolkit.odfdom.OdfFileDom;
import org.odftoolkit.odfdom.dom.OdfNamespaceNames;
import org.odftoolkit.odfdom.OdfElement;
import org.odftoolkit.odfdom.dom.attribute.form.FormNameAttribute;
import org.odftoolkit.odfdom.dom.attribute.form.FormControlImplementationAttribute;
import org.odftoolkit.odfdom.dom.attribute.form.FormLabelAttribute;
import org.odftoolkit.odfdom.dom.attribute.form.FormTextStyleNameAttribute;


/**
 * DOM implementation of OpenDocument element  {@odf.element form:column}.
 *
 */
public abstract class FormColumnElement extends OdfElement
{        
    public static final OdfName ELEMENT_NAME = OdfName.get( OdfNamespace.get(OdfNamespaceNames.FORM), "column" );


	/**
	 * Create the instance of <code>FormColumnElement</code> 
	 *
	 * @param  ownerDoc     The type is <code>OdfFileDom</code>
	 */
	public FormColumnElement( OdfFileDom ownerDoc )
	{
		super( ownerDoc, ELEMENT_NAME	);
	}

	/**
	 * Get the element name 
	 *
	 * @return  return   <code>OdfName</code> the name of element {@odf.element form:column}.
	 */
	public OdfName getOdfName()
	{
		return ELEMENT_NAME;
	}



	/**
	 * Receives the value of the ODFDOM attribute representation <code>FormNameAttribute</code> , See {@odf.attribute form:name}
	 *
	 * @return - the <code>String</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public String getFormNameAttribute()
	{
		FormNameAttribute attr = (FormNameAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.FORM), "name" ) );
		if( attr != null ){
			return String.valueOf( attr.getValue() );
		}
		return null;
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>FormNameAttribute</code> , See {@odf.attribute form:name}
	 *
	 * @param formNameValue   The type is <code>String</code>
	 */
	public void setFormNameAttribute( String formNameValue )
	{
		FormNameAttribute attr =  new FormNameAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setValue( formNameValue );
	}


	/**
	 * Receives the value of the ODFDOM attribute representation <code>FormControlImplementationAttribute</code> , See {@odf.attribute form:control-implementation}
	 *
	 * @return - the <code>String</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public String getFormControlImplementationAttribute()
	{
		FormControlImplementationAttribute attr = (FormControlImplementationAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.FORM), "control-implementation" ) );
		if( attr != null ){
			return String.valueOf( attr.getValue() );
		}
		return null;
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>FormControlImplementationAttribute</code> , See {@odf.attribute form:control-implementation}
	 *
	 * @param formControlImplementationValue   The type is <code>String</code>
	 */
	public void setFormControlImplementationAttribute( String formControlImplementationValue )
	{
		FormControlImplementationAttribute attr =  new FormControlImplementationAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setValue( formControlImplementationValue );
	}


	/**
	 * Receives the value of the ODFDOM attribute representation <code>FormLabelAttribute</code> , See {@odf.attribute form:label}
	 *
	 * @return - the <code>String</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public String getFormLabelAttribute()
	{
		FormLabelAttribute attr = (FormLabelAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.FORM), "label" ) );
		if( attr != null ){
			return String.valueOf( attr.getValue() );
		}
		return null;
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>FormLabelAttribute</code> , See {@odf.attribute form:label}
	 *
	 * @param formLabelValue   The type is <code>String</code>
	 */
	public void setFormLabelAttribute( String formLabelValue )
	{
		FormLabelAttribute attr =  new FormLabelAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setValue( formLabelValue );
	}


	/**
	 * Receives the value of the ODFDOM attribute representation <code>FormTextStyleNameAttribute</code> , See {@odf.attribute form:text-style-name}
	 *
	 * @return - the <code>String</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public String getFormTextStyleNameAttribute()
	{
		FormTextStyleNameAttribute attr = (FormTextStyleNameAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.FORM), "text-style-name" ) );
		if( attr != null ){
			return String.valueOf( attr.getValue() );
		}
		return null;
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>FormTextStyleNameAttribute</code> , See {@odf.attribute form:text-style-name}
	 *
	 * @param formTextStyleNameValue   The type is <code>String</code>
	 */
	public void setFormTextStyleNameAttribute( String formTextStyleNameValue )
	{
		FormTextStyleNameAttribute attr =  new FormTextStyleNameAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setValue( formTextStyleNameValue );
	}

	/**
	 * Create child element {@odf.element form:text}.
	 *
	 * @return   return  the element {@odf.element form:text}
	 * DifferentQName 
	 */
	public FormTextElement newFormTextElement()
	{
		FormTextElement  formText = ((OdfFileDom)this.ownerDocument).newOdfElement(FormTextElement.class);
		this.appendChild( formText);
		return  formText;
	}                   
               
	/**
	 * Create child element {@odf.element form:textarea}.
	 *
	 * @return   return  the element {@odf.element form:textarea}
	 * DifferentQName 
	 */
	public FormTextareaElement newFormTextareaElement()
	{
		FormTextareaElement  formTextarea = ((OdfFileDom)this.ownerDocument).newOdfElement(FormTextareaElement.class);
		this.appendChild( formTextarea);
		return  formTextarea;
	}                   
               
	/**
	 * Create child element {@odf.element form:formatted-text}.
	 *
	 * @return   return  the element {@odf.element form:formatted-text}
	 * DifferentQName 
	 */
	public FormFormattedTextElement newFormFormattedTextElement()
	{
		FormFormattedTextElement  formFormattedText = ((OdfFileDom)this.ownerDocument).newOdfElement(FormFormattedTextElement.class);
		this.appendChild( formFormattedText);
		return  formFormattedText;
	}                   
               
	/**
	 * Create child element {@odf.element form:number}.
	 *
	 * @return   return  the element {@odf.element form:number}
	 * DifferentQName 
	 */
	public FormNumberElement newFormNumberElement()
	{
		FormNumberElement  formNumber = ((OdfFileDom)this.ownerDocument).newOdfElement(FormNumberElement.class);
		this.appendChild( formNumber);
		return  formNumber;
	}                   
               
	/**
	 * Create child element {@odf.element form:date}.
	 *
	 * @return   return  the element {@odf.element form:date}
	 * DifferentQName 
	 */
	public FormDateElement newFormDateElement()
	{
		FormDateElement  formDate = ((OdfFileDom)this.ownerDocument).newOdfElement(FormDateElement.class);
		this.appendChild( formDate);
		return  formDate;
	}                   
               
	/**
	 * Create child element {@odf.element form:time}.
	 *
	 * @return   return  the element {@odf.element form:time}
	 * DifferentQName 
	 */
	public FormTimeElement newFormTimeElement()
	{
		FormTimeElement  formTime = ((OdfFileDom)this.ownerDocument).newOdfElement(FormTimeElement.class);
		this.appendChild( formTime);
		return  formTime;
	}                   
               
	/**
	 * Create child element {@odf.element form:combobox}.
	 *
	 * @return   return  the element {@odf.element form:combobox}
	 * DifferentQName 
	 */
	public FormComboboxElement newFormComboboxElement()
	{
		FormComboboxElement  formCombobox = ((OdfFileDom)this.ownerDocument).newOdfElement(FormComboboxElement.class);
		this.appendChild( formCombobox);
		return  formCombobox;
	}                   
               
	/**
	 * Create child element {@odf.element form:listbox}.
	 *
	 * @return   return  the element {@odf.element form:listbox}
	 * DifferentQName 
	 */
	public FormListboxElement newFormListboxElement()
	{
		FormListboxElement  formListbox = ((OdfFileDom)this.ownerDocument).newOdfElement(FormListboxElement.class);
		this.appendChild( formListbox);
		return  formListbox;
	}                   
               
	/**
	 * Create child element {@odf.element form:checkbox}.
	 *
    
	 * @return   return  the element {@odf.element form:checkbox}
	 * DifferentQName 
	 */
    
	public FormCheckboxElement newFormCheckboxElement()
	{
		FormCheckboxElement  formCheckbox = ((OdfFileDom)this.ownerDocument).newOdfElement(FormCheckboxElement.class);
		this.appendChild( formCheckbox);
		return  formCheckbox;      
	}
    
	/**
	 * Create child element {@odf.element form:checkbox}.
	 *
     * @param formImagePositionAttributeValue  the <code>String</code> value of <code>FormImagePositionAttribute</code>, see {@odf.attribute  form:image-position} at specification
	 * @return   return  the element {@odf.element form:checkbox}
	 * DifferentQName 
	 */
    
	public FormCheckboxElement newFormCheckboxElement(String formImagePositionAttributeValue)
	{
		FormCheckboxElement  formCheckbox = ((OdfFileDom)this.ownerDocument).newOdfElement(FormCheckboxElement.class);
		formCheckbox.setFormImagePositionAttribute( formImagePositionAttributeValue );
		this.appendChild( formCheckbox);
		return  formCheckbox;      
	}
    
}
