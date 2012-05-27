/************************************************************************
*
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
*
* Copyright 2009 IBM. All rights reserved.
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
package org.odftoolkit.odfdom.type;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.datatype.DatatypeFactory;

import junit.framework.Assert;

import org.junit.Test;
import org.odftoolkit.odfdom.type.Length.Unit;

public class DataTypeTest {

	@Test
	public void testDataType() {
		// AnyURI
		AnyURI anyURI = AnyURI.valueOf("./Object 1");
		URI uri = anyURI.getURI();
		Assert.assertTrue(AnyURI.isValid(uri));
		try {
			uri = new URI(URITransformer.encodePath("http://www.sina.com"));
			Assert.assertTrue(AnyURI.isValid(uri));
		} catch (URISyntaxException e) {
			e.printStackTrace();
			Assert.fail("Failed with " + e.getClass().getName() + ": '" + e.getMessage() + "'");
		}

		// Base64Binary
		Base64Binary base64Binary = Base64Binary.valueOf("GVCC9H6p8LeqecY96ggY680uoZA=");
		byte[] bytes = base64Binary.getBytes();
		System.out.println("bytes:" + bytes.length);
		Assert.assertTrue(Base64Binary.isValid("KWy1spZbKcHOunnKMB6dVA=="));

		// CellAddress
		CellAddress cellAddress = new CellAddress("Sheet1.A3");
		Assert.assertEquals(cellAddress.toString(), "Sheet1.A3");
		Assert.assertFalse(CellAddress.isValid("33"));
		Assert.assertTrue(CellAddress.isValid("$.$Z11"));

		// CellRangeAddress
		CellRangeAddress cellRangeAddress1 = CellRangeAddress.valueOf("A.A1:A.F19");
		CellRangeAddress cellRangeAddress2 = new CellRangeAddress(
				"$(first).8:$(second).19");
		CellRangeAddress cellRangeAddress3 = new CellRangeAddress("$.$8:$.19");
		Assert.assertTrue(CellRangeAddress.isValid("$Sheet1.B12:$Sheet1.E35"));

		// CellRangeAddressList
		CellRangeAddressList addressList = CellRangeAddressList.valueOf(cellRangeAddress1.toString() + " " + cellRangeAddress2.toString());
		Assert.assertEquals(addressList.getCellRangesAddressList().get(0).toString(), cellRangeAddress1.toString());
		CellRangeAddressList addressList2 = null;
		try {
			addressList2 = CellRangeAddressList.valueOf("");
		} catch (IllegalArgumentException ex) {
			// CellRangeAddressList is not allowed to have a empty string
			Assert.assertNull(addressList2);
		}

		// Color
		Color color = new Color("#ff00ff");
		String hexColor = Color.mapColorFromRgbToHex("rgb(123,214,23)");
		Assert.assertTrue(Color.isValid(hexColor));

		// DateOrDateTime
		DateTime time1 = DateTime.valueOf("2007-09-28T22:01:13");
		Assert.assertNotNull(time1);
		Date time2 = null;
		try {
			time2 = Date.valueOf("2007-09-28T22:01:13");
		} catch (IllegalArgumentException ex) {
			Assert.assertNull(time2);
			time2 = Date.valueOf("2007-09-28");
			Assert.assertNotNull(time2);
		}

		DatatypeFactory aFactory = new org.apache.xerces.jaxp.datatype.DatatypeFactoryImpl();
		GregorianCalendar calendar = new GregorianCalendar();
		System.out.println(aFactory.newXMLGregorianCalendar(calendar).toString());
		DateOrDateTime time3 = new DateOrDateTime(aFactory.newXMLGregorianCalendar(calendar));
		Assert.assertNotNull(time3.getXMLGregorianCalendar());

		// StyleName,StyleNameRef,StyleNameList
		StyleName styleName1 = new StyleName("ta1");
		StyleName styleName2 = new StyleName("_22");
		StyleName styleName3 = StyleName.valueOf("cc_a");
		Assert.assertFalse(StyleName.isValid(""));
		Assert.assertFalse(StyleName.isValid("t:1"));
		StyleNameRef styleNameRef1 = StyleNameRef.valueOf("ce1");
		Assert.assertTrue(StyleNameRef.isValid(""));
		List<StyleName> styleList = new ArrayList<StyleName>();
		styleList.add(StyleName.valueOf(styleNameRef1.toString()));
		styleList.add(styleName1);
		styleList.add(styleName2);
		StyleNameRefs styleRefs = new StyleNameRefs(styleList);
		Assert.assertEquals(styleRefs.getStyleNameRefList().get(2).toString(),
				styleName2.toString());
		// StyleNameRefs is allowed to be empty string, it is defined to have
		// zero or more NCName
		Assert.assertTrue(StyleNameRefs.isValid(""));
		styleList = StyleNameRefs.valueOf("").getStyleNameRefList();
		Assert.assertTrue(styleList.size() == 0);

		// Integer,Percent
		PositiveInteger positiveInt = new PositiveInteger(1);
		NonNegativeInteger nnInt = new NonNegativeInteger(positiveInt.intValue());
		Assert.assertFalse(NonNegativeInteger.isValid(-23));
		Percent percent = new Percent(0.3);
		Percent percent1 = Percent.valueOf("30.0%");
		Assert.assertTrue(percent1.doubleValue() == percent.doubleValue());

		// Measurement
		String inchMeasure = "-4.354in";
		Length length = new Length(inchMeasure);
		String cmMeasure = length.mapToUnit(Unit.CENTIMETER);
		NonNegativeLength nnLength = NonNegativeLength.valueOf(cmMeasure.substring(1));
		Assert.assertEquals(nnLength.mapToUnit(Unit.INCH), "4.354in");
		Assert.assertTrue(PositiveLength.isValid("0.01pt"));
		Assert.assertTrue(NonNegativeLength.isValid("0.00pt"));
		Assert.assertFalse(NonNegativeLength.isValid("-0.00pt"));
		
		@SuppressWarnings("static-access")
		int mmValue = length.parseInt(length.toString(), Unit.MILLIMETER);

		NonNegativePixelLength pixelLength = NonNegativePixelLength.valueOf("1240px");
		NonNegativePixelLength pixelLength1 = null;
		try {
			pixelLength1 = new NonNegativePixelLength("234cm");
		} catch (NumberFormatException ex) {
			Assert.assertNull(pixelLength1);
		}
	}
}
