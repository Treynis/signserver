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

package org.odftoolkit.odfdom.doc.text;

import org.odftoolkit.odfdom.doc.text.OdfTextSpace;
import org.odftoolkit.odfdom.doc.text.OdfWhitespaceProcessor;
import org.odftoolkit.odfdom.doc.text.OdfTextParagraph;
import org.odftoolkit.odfdom.doc.text.OdfTextLineBreak;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.odftoolkit.odfdom.doc.style.OdfStyleTabStop;

import org.odftoolkit.odfdom.doc.OdfTextDocument;
import org.odftoolkit.odfdom.OdfFileDom;
import org.odftoolkit.odfdom.type.NonNegativeInteger;


/**
 *
 * @author J David Eisenberg
 */
public class OdfWhitespaceProcessorTest {

    OdfTextDocument doc;
	OdfFileDom dom;
    String[] plainText = {
        "nospace",
        "one space",
        "two  spaces",
        "three   spaces",
		"   three leading spaces",
		"three trailing spaces   ",
        "one\ttab",
		"two\t\ttabs",
		"\tleading tab",
		"trailing tab\t",
		"mixed   \t   spaces and tabs",
		"line\nbreak"
    };

	String[][] elementResult = {
		{ "nospace" },
		{ "one space" },
		{ "two ", "*s1", "spaces" },
		{ "three ", "*s2", "spaces" },
		{ " ", "*s2", "three leading spaces" },
		{ "three trailing spaces ", "*s2" },
		{ "one", "*t", "tab" },
		{ "two", "*t", "*t", "tabs" },
		{ "*t", "leading tab" },
		{ "trailing tab", "*t" },
		{ "mixed ", "*s2", "*t", " ", "*s2", "spaces and tabs" },
		{ "line", "*n", "break" }
	};

    public OdfWhitespaceProcessorTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        try
        {
            doc = OdfTextDocument.newTextDocument();
			dom = doc.getContentDom();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of append method, of class OdfWhitespaceProcessor.
     */
    @Test
    public void testAppend() {
        System.out.println("append");
        Element element = null;
        OdfWhitespaceProcessor instance = new OdfWhitespaceProcessor();
		int i;
		for (i = 0; i < plainText.length; i++)
		{
			element = new OdfTextParagraph(dom);
			instance.append(element, plainText[i]);
			compareResults(element, plainText[i], elementResult[i]);
		}
    }

	private void compareResults(Element element, String input, String[] output)
	{
		int i;
		int nSpaces;
		int nSpacesInAttribute;
		Node node = element.getFirstChild();
		for (i = 0; i < output.length; i++)
		{
			if (output[i].startsWith("*"))
			{
				Assert.assertEquals(Node.ELEMENT_NODE, node.getNodeType());
				if (output[i].equals("*t"))
				{
					Assert.assertEquals("tab", node.getLocalName());
				}
				else if (output[i].equals("*n"))
				{
					Assert.assertEquals("line-break", node.getLocalName());
				}
				else
				{
					nSpaces = Integer.parseInt(output[i].substring(2));
					Assert.assertEquals(node.getLocalName(), "s");
					nSpacesInAttribute = Integer.parseInt(
						((Element)node).getAttribute("text:c"));
					Assert.assertEquals(nSpaces, nSpacesInAttribute);
				}
			}
			else
			{
				Assert.assertEquals(Node.TEXT_NODE, node.getNodeType());
				Assert.assertEquals(output[i], node.getTextContent());
			}
			node = node.getNextSibling();
		}
		Assert.assertEquals(node, null);
	}

    /**
     * Test of getText method, of class OdfWhitespaceProcessor.
     */
    @Test
    public void testGetText() {
        System.out.println("getText");
        Node element = null;
        OdfWhitespaceProcessor instance = new OdfWhitespaceProcessor();
		int i;
        String expResult = "";
        String result;
		for (i = 0; i < plainText.length; i++)
		{
			element = new OdfTextParagraph(dom);
			constructElement(element, elementResult[i]);
			result = plainText[i];
			expResult = instance.getText(element);
			Assert.assertEquals(expResult, result);
		}
    }

	private void constructElement(Node element, String[] expected)
	{
		int i;
		int nSpaces;
		OdfTextSpace spaceElement;

		for (i = 0; i < expected.length; i++)
		{
			if (expected[i].startsWith("*"))
			{
				if (expected[i].equals("*t"))
				{
					element.appendChild(new OdfTextTab(dom));
				}
				else if (expected[i].equals("*n"))
				{
					element.appendChild(new OdfTextLineBreak(dom));
				}
				else
				{
					nSpaces = Integer.parseInt(expected[i].substring(2));
					spaceElement = new OdfTextSpace(dom);
					spaceElement.setTextCAttribute(nSpaces);
					element.appendChild(spaceElement);
				}
			}
			else
			{
				element.appendChild( dom.createTextNode(expected[i]));
			}
		}
	}
    /**
     * Test of appendText method, of class OdfWhitespaceProcessor.
     */
    @Test
    public void testAppendText() {
        System.out.println("appendText");
        Element element = null;
		int i;
		for (i = 0; i < plainText.length; i++)
		{
			element = new OdfTextParagraph(dom);
			OdfWhitespaceProcessor.appendText(element, plainText[i]);
			compareResults(element, plainText[i], elementResult[i]);
		}
    }

}