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


import org.odftoolkit.odfdom.doc.text.OdfTextSpan;
import org.odftoolkit.odfdom.doc.OdfTextDocument;
import org.odftoolkit.odfdom.OdfFileDom;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Assert;

import org.w3c.dom.Node;

/**
 *
 * @author instructor
 */
public class OdfSpanTest {
    OdfTextDocument doc;
	OdfFileDom dom;

    public OdfSpanTest() {
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
	 * Test of addContent method, of class OdfSpan.
	 */
	@Test
	public void testAddContent() {
		System.out.println("addContent");
		String content = "span content";
		OdfTextSpan instance = new OdfTextSpan(dom);
		Node node;
		Assert.assertNotNull(instance);
		instance.addContent(content);
		node = instance.getFirstChild();
		Assert.assertNotNull(node);
		Assert.assertEquals(Node.TEXT_NODE, node.getNodeType());
		Assert.assertEquals(content, node.getTextContent());
	}
    
	/**
	 * Test of addContent method, of class OdfSpan.
	 */
	@Test
	public void testAddContentWhitespace() {
		System.out.println("text:span addContentWhitespace");
		String content = "span\tcontent";
		String part1 = "span";
		String part2 = "content";
		OdfTextSpan instance = new OdfTextSpan(dom);
		Node node;
		Assert.assertNotNull(instance);
		instance.addContentWhitespace(content);
		node = instance.getFirstChild();
		Assert.assertNotNull(node);
		Assert.assertEquals(Node.TEXT_NODE, node.getNodeType());
		Assert.assertEquals(part1, node.getTextContent());
		node = node.getNextSibling();
		Assert.assertNotNull(node);
		Assert.assertEquals(Node.ELEMENT_NODE, node.getNodeType());
		Assert.assertEquals("tab", node.getLocalName());
		node = node.getNextSibling();
		Assert.assertNotNull(node);
		Assert.assertEquals(Node.TEXT_NODE, node.getNodeType());
		Assert.assertEquals(part2, node.getTextContent());
	}

	/**
	 * Test of addStyledContent method, of class OdfSpan.
	 */
	@Test
	public void testAddStyledContent() {
		System.out.println("addStyleContent");
		String content = "span content";
		String styleName = "testStyle";
		OdfTextSpan instance = new OdfTextSpan(dom);
		Node node;
		Assert.assertNotNull(instance);
		instance.addStyledContent(styleName, content);
		node = instance.getFirstChild();
		Assert.assertNotNull(node);
		Assert.assertEquals(Node.TEXT_NODE, node.getNodeType());
		Assert.assertEquals(node.getTextContent(), content);
		Assert.assertEquals(instance.getStyleName(), styleName);
	}

	/**
	 * Test of addStyledContent method, of class OdfSpan.
	 */
	@Test
	public void testAddStyledContentWhitespace() {
		System.out.println("text:span addStyledContentWhitespace");
		String content = "span\ncontent";
		String styleName = "testStyle";
		String part1 = "span";
		String part2 = "content";
		OdfTextSpan instance = new OdfTextSpan(dom);
		Node node;
		Assert.assertNotNull(instance);
		instance.addStyledContentWhitespace(styleName, content);
		Assert.assertEquals(instance.getStyleName(), styleName);
		node = instance.getFirstChild();
		Assert.assertNotNull(node);
		Assert.assertEquals(Node.TEXT_NODE, node.getNodeType());
		Assert.assertEquals(part1, node.getTextContent());
		node = node.getNextSibling();
		Assert.assertNotNull(node);
		Assert.assertEquals(Node.ELEMENT_NODE, node.getNodeType());
		Assert.assertEquals("line-break", node.getLocalName());
		node = node.getNextSibling();
		Assert.assertNotNull(node);
		Assert.assertEquals(Node.TEXT_NODE, node.getNodeType());
		Assert.assertEquals(part2, node.getTextContent());
	}

}