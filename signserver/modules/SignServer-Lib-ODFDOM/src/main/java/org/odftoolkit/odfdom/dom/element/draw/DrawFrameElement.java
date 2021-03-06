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

package org.odftoolkit.odfdom.dom.element.draw;

import org.odftoolkit.odfdom.OdfName;
import org.odftoolkit.odfdom.OdfNamespace;
import org.odftoolkit.odfdom.OdfFileDom;
import org.odftoolkit.odfdom.dom.OdfNamespaceNames;
import org.odftoolkit.odfdom.dom.attribute.draw.DrawLayerAttribute;
import org.odftoolkit.odfdom.dom.attribute.draw.DrawTransformAttribute;
import org.odftoolkit.odfdom.dom.attribute.draw.DrawTextStyleNameAttribute;
import org.odftoolkit.odfdom.dom.attribute.svg.SvgXAttribute;
import org.odftoolkit.odfdom.dom.attribute.svg.SvgYAttribute;
import org.odftoolkit.odfdom.dom.attribute.svg.SvgWidthAttribute;
import org.odftoolkit.odfdom.dom.attribute.svg.SvgHeightAttribute;
import org.odftoolkit.odfdom.dom.attribute.style.StyleRelWidthAttribute;
import org.odftoolkit.odfdom.dom.attribute.style.StyleRelHeightAttribute;
import org.odftoolkit.odfdom.dom.attribute.draw.DrawCaptionIdAttribute;
import org.odftoolkit.odfdom.dom.attribute.presentation.PresentationClassAttribute;
import org.odftoolkit.odfdom.dom.attribute.presentation.PresentationPlaceholderAttribute;
import org.odftoolkit.odfdom.dom.attribute.presentation.PresentationUserTransformedAttribute;
import org.odftoolkit.odfdom.dom.attribute.draw.DrawCopyOfAttribute;

import org.odftoolkit.odfdom.dom.element.table.TableTableElement;
import org.odftoolkit.odfdom.dom.element.office.OfficeEventListenersElement;
import org.odftoolkit.odfdom.dom.element.svg.SvgTitleElement;
import org.odftoolkit.odfdom.dom.element.svg.SvgDescElement;

/**
 * DOM implementation of OpenDocument element  {@odf.element draw:frame}.
 *
 */
public abstract class DrawFrameElement extends DrawShapeElementBase
{        
    public static final OdfName ELEMENT_NAME = OdfName.get( OdfNamespace.get(OdfNamespaceNames.DRAW), "frame" );


	/**
	 * Create the instance of <code>DrawFrameElement</code> 
	 *
	 * @param  ownerDoc     The type is <code>OdfFileDom</code>
	 */
	public DrawFrameElement( OdfFileDom ownerDoc )
	{
		super( ownerDoc, ELEMENT_NAME	);
	}

	/**
	 * Get the element name 
	 *
	 * @return  return   <code>OdfName</code> the name of element {@odf.element draw:frame}.
	 */
	public OdfName getOdfName()
	{
		return ELEMENT_NAME;
	}



	/**
	 * Receives the value of the ODFDOM attribute representation <code>DrawLayerAttribute</code> , See {@odf.attribute draw:layer}
	 *
	 * @return - the <code>String</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public String getDrawLayerAttribute()
	{
		DrawLayerAttribute attr = (DrawLayerAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.DRAW), "layer" ) );
		if( attr != null ){
			return String.valueOf( attr.getValue() );
		}
		return null;
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>DrawLayerAttribute</code> , See {@odf.attribute draw:layer}
	 *
	 * @param drawLayerValue   The type is <code>String</code>
	 */
	public void setDrawLayerAttribute( String drawLayerValue )
	{
		DrawLayerAttribute attr =  new DrawLayerAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setValue( drawLayerValue );
	}


	/**
	 * Receives the value of the ODFDOM attribute representation <code>DrawTransformAttribute</code> , See {@odf.attribute draw:transform}
	 *
	 * @return - the <code>String</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public String getDrawTransformAttribute()
	{
		DrawTransformAttribute attr = (DrawTransformAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.DRAW), "transform" ) );
		if( attr != null ){
			return String.valueOf( attr.getValue() );
		}
		return null;
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>DrawTransformAttribute</code> , See {@odf.attribute draw:transform}
	 *
	 * @param drawTransformValue   The type is <code>String</code>
	 */
	public void setDrawTransformAttribute( String drawTransformValue )
	{
		DrawTransformAttribute attr =  new DrawTransformAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setValue( drawTransformValue );
	}


	/**
	 * Receives the value of the ODFDOM attribute representation <code>DrawTextStyleNameAttribute</code> , See {@odf.attribute draw:text-style-name}
	 *
	 * @return - the <code>String</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public String getDrawTextStyleNameAttribute()
	{
		DrawTextStyleNameAttribute attr = (DrawTextStyleNameAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.DRAW), "text-style-name" ) );
		if( attr != null ){
			return String.valueOf( attr.getValue() );
		}
		return null;
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>DrawTextStyleNameAttribute</code> , See {@odf.attribute draw:text-style-name}
	 *
	 * @param drawTextStyleNameValue   The type is <code>String</code>
	 */
	public void setDrawTextStyleNameAttribute( String drawTextStyleNameValue )
	{
		DrawTextStyleNameAttribute attr =  new DrawTextStyleNameAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setValue( drawTextStyleNameValue );
	}


	/**
	 * Receives the value of the ODFDOM attribute representation <code>SvgXAttribute</code> , See {@odf.attribute svg:x}
	 *
	 * @return - the <code>String</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public String getSvgXAttribute()
	{
		SvgXAttribute attr = (SvgXAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.SVG), "x" ) );
		if( attr != null ){
			return String.valueOf( attr.getValue() );
		}
		return null;
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>SvgXAttribute</code> , See {@odf.attribute svg:x}
	 *
	 * @param svgXValue   The type is <code>String</code>
	 */
	public void setSvgXAttribute( String svgXValue )
	{
		SvgXAttribute attr =  new SvgXAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setValue( svgXValue );
	}


	/**
	 * Receives the value of the ODFDOM attribute representation <code>SvgYAttribute</code> , See {@odf.attribute svg:y}
	 *
	 * @return - the <code>String</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public String getSvgYAttribute()
	{
		SvgYAttribute attr = (SvgYAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.SVG), "y" ) );
		if( attr != null ){
			return String.valueOf( attr.getValue() );
		}
		return null;
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>SvgYAttribute</code> , See {@odf.attribute svg:y}
	 *
	 * @param svgYValue   The type is <code>String</code>
	 */
	public void setSvgYAttribute( String svgYValue )
	{
		SvgYAttribute attr =  new SvgYAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setValue( svgYValue );
	}


	/**
	 * Receives the value of the ODFDOM attribute representation <code>SvgWidthAttribute</code> , See {@odf.attribute svg:width}
	 *
	 * @return - the <code>String</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public String getSvgWidthAttribute()
	{
		SvgWidthAttribute attr = (SvgWidthAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.SVG), "width" ) );
		if( attr != null ){
			return String.valueOf( attr.getValue() );
		}
		return null;
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>SvgWidthAttribute</code> , See {@odf.attribute svg:width}
	 *
	 * @param svgWidthValue   The type is <code>String</code>
	 */
	public void setSvgWidthAttribute( String svgWidthValue )
	{
		SvgWidthAttribute attr =  new SvgWidthAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setValue( svgWidthValue );
	}


	/**
	 * Receives the value of the ODFDOM attribute representation <code>SvgHeightAttribute</code> , See {@odf.attribute svg:height}
	 *
	 * @return - the <code>String</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public String getSvgHeightAttribute()
	{
		SvgHeightAttribute attr = (SvgHeightAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.SVG), "height" ) );
		if( attr != null ){
			return String.valueOf( attr.getValue() );
		}
		return null;
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>SvgHeightAttribute</code> , See {@odf.attribute svg:height}
	 *
	 * @param svgHeightValue   The type is <code>String</code>
	 */
	public void setSvgHeightAttribute( String svgHeightValue )
	{
		SvgHeightAttribute attr =  new SvgHeightAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setValue( svgHeightValue );
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
	 * Receives the value of the ODFDOM attribute representation <code>StyleRelHeightAttribute</code> , See {@odf.attribute style:rel-height}
	 *
	 * @return - the <code>String</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public String getStyleRelHeightAttribute()
	{
		StyleRelHeightAttribute attr = (StyleRelHeightAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.STYLE), "rel-height" ) );
		if( attr != null ){
			return String.valueOf( attr.getValue() );
		}
		return null;
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>StyleRelHeightAttribute</code> , See {@odf.attribute style:rel-height}
	 *
	 * @param styleRelHeightValue   The type is <code>String</code>
	 */
	public void setStyleRelHeightAttribute( String styleRelHeightValue )
	{
		StyleRelHeightAttribute attr =  new StyleRelHeightAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setValue( styleRelHeightValue );
	}


	/**
	 * Receives the value of the ODFDOM attribute representation <code>DrawCaptionIdAttribute</code> , See {@odf.attribute draw:caption-id}
	 *
	 * @return - the <code>String</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public String getDrawCaptionIdAttribute()
	{
		DrawCaptionIdAttribute attr = (DrawCaptionIdAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.DRAW), "caption-id" ) );
		if( attr != null ){
			return String.valueOf( attr.getValue() );
		}
		return null;
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>DrawCaptionIdAttribute</code> , See {@odf.attribute draw:caption-id}
	 *
	 * @param drawCaptionIdValue   The type is <code>String</code>
	 */
	public void setDrawCaptionIdAttribute( String drawCaptionIdValue )
	{
		DrawCaptionIdAttribute attr =  new DrawCaptionIdAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setValue( drawCaptionIdValue );
	}


	/**
	 * Receives the value of the ODFDOM attribute representation <code>PresentationClassAttribute</code> , See {@odf.attribute presentation:class}
	 *
	 * @return - the <code>String</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public String getPresentationClassAttribute()
	{
		PresentationClassAttribute attr = (PresentationClassAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.PRESENTATION), "class" ) );
		if( attr != null ){
			return String.valueOf( attr.getValue() );
		}
		return null;
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>PresentationClassAttribute</code> , See {@odf.attribute presentation:class}
	 *
	 * @param presentationClassValue   The type is <code>String</code>
	 */
	public void setPresentationClassAttribute( String presentationClassValue )
	{
		PresentationClassAttribute attr =  new PresentationClassAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setValue( presentationClassValue );
	}


	/**
	 * Receives the value of the ODFDOM attribute representation <code>PresentationPlaceholderAttribute</code> , See {@odf.attribute presentation:placeholder}
	 *
	 * @return - the <code>Boolean</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public Boolean getPresentationPlaceholderAttribute()
	{
		PresentationPlaceholderAttribute attr = (PresentationPlaceholderAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.PRESENTATION), "placeholder" ) );
		if( attr != null ){
			return Boolean.valueOf( attr.booleanValue() );
		}
		return null;
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>PresentationPlaceholderAttribute</code> , See {@odf.attribute presentation:placeholder}
	 *
	 * @param presentationPlaceholderValue   The type is <code>Boolean</code>
	 */
	public void setPresentationPlaceholderAttribute( Boolean presentationPlaceholderValue )
	{
		PresentationPlaceholderAttribute attr =  new PresentationPlaceholderAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setBooleanValue( presentationPlaceholderValue.booleanValue() );
	}


	/**
	 * Receives the value of the ODFDOM attribute representation <code>PresentationUserTransformedAttribute</code> , See {@odf.attribute presentation:user-transformed}
	 *
	 * @return - the <code>Boolean</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public Boolean getPresentationUserTransformedAttribute()
	{
		PresentationUserTransformedAttribute attr = (PresentationUserTransformedAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.PRESENTATION), "user-transformed" ) );
		if( attr != null ){
			return Boolean.valueOf( attr.booleanValue() );
		}
		return null;
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>PresentationUserTransformedAttribute</code> , See {@odf.attribute presentation:user-transformed}
	 *
	 * @param presentationUserTransformedValue   The type is <code>Boolean</code>
	 */
	public void setPresentationUserTransformedAttribute( Boolean presentationUserTransformedValue )
	{
		PresentationUserTransformedAttribute attr =  new PresentationUserTransformedAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setBooleanValue( presentationUserTransformedValue.booleanValue() );
	}


	/**
	 * Receives the value of the ODFDOM attribute representation <code>DrawCopyOfAttribute</code> , See {@odf.attribute draw:copy-of}
	 *
	 * @return - the <code>String</code> , the value or <code>null</code>, if the attribute is not set and no default value defined.
	 */
	public String getDrawCopyOfAttribute()
	{
		DrawCopyOfAttribute attr = (DrawCopyOfAttribute) getOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.DRAW), "copy-of" ) );
		if( attr != null ){
			return String.valueOf( attr.getValue() );
		}
		return null;
	}
		 
	/**
	 * Sets the value of ODFDOM attribute representation <code>DrawCopyOfAttribute</code> , See {@odf.attribute draw:copy-of}
	 *
	 * @param drawCopyOfValue   The type is <code>String</code>
	 */
	public void setDrawCopyOfAttribute( String drawCopyOfValue )
	{
		DrawCopyOfAttribute attr =  new DrawCopyOfAttribute( (OdfFileDom)this.ownerDocument );
		setOdfAttribute( attr );
		attr.setValue( drawCopyOfValue );
	}

	/**
	 * Create child element {@odf.element draw:text-box}.
	 *
	 * @return   return  the element {@odf.element draw:text-box}
	 * DifferentQName 
	 */
	public DrawTextBoxElement newDrawTextBoxElement()
	{
		DrawTextBoxElement  drawTextBox = ((OdfFileDom)this.ownerDocument).newOdfElement(DrawTextBoxElement.class);
		this.appendChild( drawTextBox);
		return  drawTextBox;
	}                   
               
	/**
	 * Create child element {@odf.element draw:image}.
	 *
	 * @return   return  the element {@odf.element draw:image}
	 * DifferentQName 
	 */
	public DrawImageElement newDrawImageElement()
	{
		DrawImageElement  drawImage = ((OdfFileDom)this.ownerDocument).newOdfElement(DrawImageElement.class);
		this.appendChild( drawImage);
		return  drawImage;
	}                   
               
	/**
	 * Create child element {@odf.element draw:object}.
	 *
	 * @return   return  the element {@odf.element draw:object}
	 * DifferentQName 
	 */
	public DrawObjectElement newDrawObjectElement()
	{
		DrawObjectElement  drawObject = ((OdfFileDom)this.ownerDocument).newOdfElement(DrawObjectElement.class);
		this.appendChild( drawObject);
		return  drawObject;
	}                   
               
	/**
	 * Create child element {@odf.element draw:object-ole}.
	 *
	 * @return   return  the element {@odf.element draw:object-ole}
	 * DifferentQName 
	 */
	public DrawObjectOleElement newDrawObjectOleElement()
	{
		DrawObjectOleElement  drawObjectOle = ((OdfFileDom)this.ownerDocument).newOdfElement(DrawObjectOleElement.class);
		this.appendChild( drawObjectOle);
		return  drawObjectOle;
	}                   
               
	/**
	 * Create child element {@odf.element draw:applet}.
	 *
	 * @return   return  the element {@odf.element draw:applet}
	 * DifferentQName 
	 */
	public DrawAppletElement newDrawAppletElement()
	{
		DrawAppletElement  drawApplet = ((OdfFileDom)this.ownerDocument).newOdfElement(DrawAppletElement.class);
		this.appendChild( drawApplet);
		return  drawApplet;
	}                   
               
	/**
	 * Create child element {@odf.element draw:floating-frame}.
	 *
	 * @return   return  the element {@odf.element draw:floating-frame}
	 * DifferentQName 
	 */
	public DrawFloatingFrameElement newDrawFloatingFrameElement()
	{
		DrawFloatingFrameElement  drawFloatingFrame = ((OdfFileDom)this.ownerDocument).newOdfElement(DrawFloatingFrameElement.class);
		this.appendChild( drawFloatingFrame);
		return  drawFloatingFrame;
	}                   
               
	/**
	 * Create child element {@odf.element draw:plugin}.
	 *
	 * @return   return  the element {@odf.element draw:plugin}
	 * DifferentQName 
	 */
	public DrawPluginElement newDrawPluginElement()
	{
		DrawPluginElement  drawPlugin = ((OdfFileDom)this.ownerDocument).newOdfElement(DrawPluginElement.class);
		this.appendChild( drawPlugin);
		return  drawPlugin;
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
	 * Create child element {@odf.element office:event-listeners}.
	 *
	 * @return   return  the element {@odf.element office:event-listeners}
	 * DifferentQName 
	 */
	public OfficeEventListenersElement newOfficeEventListenersElement()
	{
		OfficeEventListenersElement  officeEventListeners = ((OdfFileDom)this.ownerDocument).newOdfElement(OfficeEventListenersElement.class);
		this.appendChild( officeEventListeners);
		return  officeEventListeners;
	}                   
               
	/**
	 * Create child element {@odf.element draw:glue-point}.
	 *
     * @param drawEscapeDirectionAttributeValue  the <code>String</code> value of <code>DrawEscapeDirectionAttribute</code>, see {@odf.attribute  draw:escape-direction} at specification
	 * @param drawIdAttributeValue  the <code>String</code> value of <code>DrawIdAttribute</code>, see {@odf.attribute  draw:id} at specification
	 * @param svgXAttributeValue  the <code>String</code> value of <code>SvgXAttribute</code>, see {@odf.attribute  svg:x} at specification
	 * @param svgYAttributeValue  the <code>String</code> value of <code>SvgYAttribute</code>, see {@odf.attribute  svg:y} at specification
	 * @return   return  the element {@odf.element draw:glue-point}
	 * DifferentQName 
	 */
    
	public DrawGluePointElement newDrawGluePointElement(String drawEscapeDirectionAttributeValue, String drawIdAttributeValue, String svgXAttributeValue, String svgYAttributeValue)
	{
		DrawGluePointElement  drawGluePoint = ((OdfFileDom)this.ownerDocument).newOdfElement(DrawGluePointElement.class);
		drawGluePoint.setDrawEscapeDirectionAttribute( drawEscapeDirectionAttributeValue );
		drawGluePoint.setDrawIdAttribute( drawIdAttributeValue );
		drawGluePoint.setSvgXAttribute( svgXAttributeValue );
		drawGluePoint.setSvgYAttribute( svgYAttributeValue );
		this.appendChild( drawGluePoint);
		return  drawGluePoint;      
	}
    
	/**
	 * Create child element {@odf.element draw:image-map}.
	 *
	 * @return   return  the element {@odf.element draw:image-map}
	 * DifferentQName 
	 */
	public DrawImageMapElement newDrawImageMapElement()
	{
		DrawImageMapElement  drawImageMap = ((OdfFileDom)this.ownerDocument).newOdfElement(DrawImageMapElement.class);
		this.appendChild( drawImageMap);
		return  drawImageMap;
	}                   
               
	/**
	 * Create child element {@odf.element svg:title}.
	 *
	 * @return   return  the element {@odf.element svg:title}
	 * DifferentQName 
	 */
	public SvgTitleElement newSvgTitleElement()
	{
		SvgTitleElement  svgTitle = ((OdfFileDom)this.ownerDocument).newOdfElement(SvgTitleElement.class);
		this.appendChild( svgTitle);
		return  svgTitle;
	}                   
               
	/**
	 * Create child element {@odf.element svg:desc}.
	 *
	 * @return   return  the element {@odf.element svg:desc}
	 * DifferentQName 
	 */
	public SvgDescElement newSvgDescElement()
	{
		SvgDescElement  svgDesc = ((OdfFileDom)this.ownerDocument).newOdfElement(SvgDescElement.class);
		this.appendChild( svgDesc);
		return  svgDesc;
	}                   
               
	/**
	 * Create child element {@odf.element draw:contour-polygon}.
	 *
     * @param drawPointsAttributeValue  the <code>String</code> value of <code>DrawPointsAttribute</code>, see {@odf.attribute  draw:points} at specification
	 * @param drawRecreateOnEditAttributeValue  the <code>boolean</code> value of <code>DrawRecreateOnEditAttribute</code>, see {@odf.attribute  draw:recreate-on-edit} at specification
	 * @param svgViewBoxAttributeValue  the <code>String</code> value of <code>SvgViewBoxAttribute</code>, see {@odf.attribute  svg:viewBox} at specification
	 * @return   return  the element {@odf.element draw:contour-polygon}
	 * DifferentQName 
	 */
    
	public DrawContourPolygonElement newDrawContourPolygonElement(String drawPointsAttributeValue, boolean drawRecreateOnEditAttributeValue, String svgViewBoxAttributeValue)
	{
		DrawContourPolygonElement  drawContourPolygon = ((OdfFileDom)this.ownerDocument).newOdfElement(DrawContourPolygonElement.class);
		drawContourPolygon.setDrawPointsAttribute( drawPointsAttributeValue );
		drawContourPolygon.setDrawRecreateOnEditAttribute( Boolean.valueOf(drawRecreateOnEditAttributeValue) );
		drawContourPolygon.setSvgViewBoxAttribute( svgViewBoxAttributeValue );
		this.appendChild( drawContourPolygon);
		return  drawContourPolygon;      
	}
    
	/**
	 * Create child element {@odf.element draw:contour-path}.
	 *
     * @param drawRecreateOnEditAttributeValue  the <code>boolean</code> value of <code>DrawRecreateOnEditAttribute</code>, see {@odf.attribute  draw:recreate-on-edit} at specification
	 * @param svgDAttributeValue  the <code>String</code> value of <code>SvgDAttribute</code>, see {@odf.attribute  svg:d} at specification
	 * @param svgViewBoxAttributeValue  the <code>String</code> value of <code>SvgViewBoxAttribute</code>, see {@odf.attribute  svg:viewBox} at specification
	 * @return   return  the element {@odf.element draw:contour-path}
	 * DifferentQName 
	 */
    
	public DrawContourPathElement newDrawContourPathElement(boolean drawRecreateOnEditAttributeValue, String svgDAttributeValue, String svgViewBoxAttributeValue)
	{
		DrawContourPathElement  drawContourPath = ((OdfFileDom)this.ownerDocument).newOdfElement(DrawContourPathElement.class);
		drawContourPath.setDrawRecreateOnEditAttribute( Boolean.valueOf(drawRecreateOnEditAttributeValue) );
		drawContourPath.setSvgDAttribute( svgDAttributeValue );
		drawContourPath.setSvgViewBoxAttribute( svgViewBoxAttributeValue );
		this.appendChild( drawContourPath);
		return  drawContourPath;      
	}
    
}
