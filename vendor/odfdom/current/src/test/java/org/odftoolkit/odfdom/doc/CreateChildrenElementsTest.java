package org.odftoolkit.odfdom.doc;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.Assert;
import org.junit.Test;
import org.odftoolkit.odfdom.OdfFileDom;
import org.odftoolkit.odfdom.doc.chart.OdfChart;
import org.odftoolkit.odfdom.doc.chart.OdfChartPlotArea;
import org.odftoolkit.odfdom.doc.draw.OdfDrawPage;
import org.odftoolkit.odfdom.doc.office.OdfOfficeBody;
import org.odftoolkit.odfdom.doc.office.OdfOfficeDocumentContent;
import org.odftoolkit.odfdom.doc.office.OdfOfficePresentation;
import org.odftoolkit.odfdom.doc.office.OdfOfficeSpreadsheet;
import org.odftoolkit.odfdom.doc.office.OdfOfficeStyles;
import org.odftoolkit.odfdom.doc.table.OdfTable;
import org.odftoolkit.odfdom.doc.text.OdfTextHeading;
import org.odftoolkit.odfdom.doc.text.OdfTextList;
import org.odftoolkit.odfdom.doc.text.OdfTextListItem;
import org.odftoolkit.odfdom.doc.text.OdfTextParagraph;
import org.odftoolkit.odfdom.doc.text.OdfTextSoftPageBreak;
import org.odftoolkit.odfdom.OdfNamespace;
import org.odftoolkit.odfdom.OdfElement;
import org.odftoolkit.odfdom.dom.element.anim.AnimAnimateElement;
import org.odftoolkit.odfdom.dom.element.chart.ChartChartElement;
import org.odftoolkit.odfdom.dom.element.chart.ChartPlotAreaElement;
import org.odftoolkit.odfdom.dom.element.draw.DrawLineElement;
import org.odftoolkit.odfdom.dom.element.draw.DrawPageElement;
import org.odftoolkit.odfdom.dom.element.form.FormFormElement;
import org.odftoolkit.odfdom.dom.element.office.OfficeSpreadsheetElement;
import org.odftoolkit.odfdom.dom.element.office.OfficeTextElement;
import org.odftoolkit.odfdom.dom.element.style.StyleDefaultStyleElement;
import org.odftoolkit.odfdom.dom.element.style.StyleStyleElement;
import org.odftoolkit.odfdom.dom.element.style.StyleTableColumnPropertiesElement;
import org.odftoolkit.odfdom.dom.element.style.StyleTablePropertiesElement;
import org.odftoolkit.odfdom.dom.element.style.StyleTextPropertiesElement;
import org.odftoolkit.odfdom.dom.element.table.TableTableCellElement;
import org.odftoolkit.odfdom.dom.element.table.TableTableColumnElement;
import org.odftoolkit.odfdom.dom.element.table.TableTableElement;
import org.odftoolkit.odfdom.dom.element.table.TableTableRowElement;
import org.odftoolkit.odfdom.dom.element.text.TextHElement;
import org.odftoolkit.odfdom.dom.element.text.TextListElement;
import org.odftoolkit.odfdom.dom.element.text.TextListItemElement;
import org.odftoolkit.odfdom.dom.element.text.TextPElement;
import org.odftoolkit.odfdom.dom.element.text.TextSoftPageBreakElement;
import org.odftoolkit.odfdom.dom.style.OdfStyleFamily;
import org.odftoolkit.odfdom.utils.ResourceUtilities;
import org.w3c.dom.NodeList;

public class CreateChildrenElementsTest {
	
	private XPath xpath;

	public CreateChildrenElementsTest() {
		xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new OdfNamespace());
	}

	@Test
	public void testCreatChildrenForPresentation() {
		try {
			
			OdfDocument odfdoc = OdfDocument.loadDocument(ResourceUtilities.getTestResource("presentation.odp"));
            
			OdfOfficePresentation presentation = OdfElement.findFirstChildNode( OdfOfficePresentation.class, odfdoc.getOfficeBody() );
            Assert.assertNotNull(presentation);
            
            DrawPageElement page = presentation.newDrawPageElement("NewPage");
            

            DrawPageElement presentationTest = (DrawPageElement) xpath.evaluate("//draw:page[last()]", odfdoc.getContentDom() , XPathConstants.NODE);
            
            Assert.assertTrue(presentationTest instanceof DrawPageElement);
            Assert.assertEquals(page,presentationTest);
            Assert.assertEquals(presentationTest.getNodeName(), "draw:page");
            Assert.assertEquals(presentationTest.getDrawMasterPageNameAttribute(), "NewPage");
			
            odfdoc.save(ResourceUtilities.createTestResource("CreatChildrenForPresentationTest.odp"));

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testCreatChildrenForChart() {
		try {
			
            OdfFileDom doc = OdfDocument.loadDocument(ResourceUtilities.getTestResource("empty.odt")).getContentDom();
            
            // find the last paragraph
            NodeList lst = doc.getElementsByTagNameNS(
                    OdfTextParagraph.ELEMENT_NAME.getUri(),
                    OdfTextParagraph.ELEMENT_NAME.getLocalName());
            Assert.assertTrue(lst.getLength() > 0);
            OdfTextParagraph p0 = (OdfTextParagraph) lst.item(lst.getLength() - 1);

            OdfOfficeDocumentContent content= (OdfOfficeDocumentContent) doc.newOdfElement(OdfOfficeDocumentContent.class);
            OdfOfficeBody body = (OdfOfficeBody)doc.newOdfElement(OdfOfficeBody.class);
            content.appendChild(body);
            OdfChart chart = doc.newOdfElement(OdfChart.class);
            //create children element
            ChartPlotAreaElement plotArea = chart.newChartPlotAreaElement();
            body.appendChild(chart);         
            p0.getParentNode().insertBefore(content, p0);
            
            
            ChartChartElement chartTest = (ChartChartElement) xpath.evaluate("//chart:chart[last()]", doc , XPathConstants.NODE);
            
            Assert.assertNotNull(chartTest.getChildNodes());

            Assert.assertTrue(chartTest.getChildNodes().item(0) instanceof OdfChartPlotArea);
            Assert.assertEquals(plotArea,chartTest.getChildNodes().item(0));
            Assert.assertEquals(chartTest.getChildNodes().item(0).getNodeName(), "chart:plot-area");
			
            doc.getOdfDocument().save(ResourceUtilities.createTestResource("CreatChildrenForChartTest.odt"));

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
	
    @Test
    public void testCreateChildrenForTable() {
        try {            
            OdfFileDom doc = OdfDocument.loadDocument(ResourceUtilities.getTestResource("empty.odt")).getContentDom();
            
            // find the last paragraph
            NodeList lst = doc.getElementsByTagNameNS(
                    TextPElement.ELEMENT_NAME.getUri(),
                    TextPElement.ELEMENT_NAME.getLocalName());
            Assert.assertTrue(lst.getLength() > 0);
            OdfTextParagraph p0 = (OdfTextParagraph) lst.item(lst.getLength() - 1);

            OdfTable table = doc.newOdfElement(OdfTable.class);
            
            
            TableTableRowElement tr = table.newTableTableRowElement();
            
            TableTableCellElement td1 =tr.newTableTableCellElement();
            
            TextPElement p1 = td1.newTextPElement();
            p1.appendChild(doc.createTextNode("content 1"));
 
            p0.getParentNode().insertBefore(table, p0);

            table.setProperty(StyleTablePropertiesElement.Width, "12cm");
            table.setProperty(StyleTablePropertiesElement.Align, "left");

            td1.setProperty(StyleTableColumnPropertiesElement.ColumnWidth, "2cm");
            
            TableTableRowElement tableRowTest = (TableTableRowElement) xpath.evaluate("//table:table-row [last()]", doc , XPathConstants.NODE);
            Assert.assertNotNull(tableRowTest.getChildNodes());
            
            Assert.assertTrue(tableRowTest.getChildNodes().item(0) instanceof TableTableCellElement);
            Assert.assertEquals(tableRowTest.getChildNodes().item(0).getNodeName(), "table:table-cell");
                                    
            doc.getOdfDocument().save(ResourceUtilities.createTestResource("CreateChildrenForTableTest.odt"));

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Failed with " + e.getClass().getName() + ": '" + e.getMessage() + "'");
        }
    }
    
	@Test
	public void testCreatChildrenForText() {
		try {
			
            OdfFileDom doc = OdfDocument.loadDocument(ResourceUtilities.getTestResource("empty.odt")).getContentDom();
            
            // find the last paragraph
            NodeList lst = doc.getElementsByTagNameNS(
            		OdfTextParagraph.ELEMENT_NAME.getUri(),
            		OdfTextParagraph.ELEMENT_NAME.getLocalName());
            Assert.assertTrue(lst.getLength() > 0);
            OdfTextParagraph p0 = (OdfTextParagraph) lst.item(lst.getLength() - 1);

            OdfTextListItem listItem = doc.newOdfElement(OdfTextListItem.class);
            //create children elements
            TextHElement heading = listItem.newTextHElement("1");
            TextListElement list = listItem.newTextListElement();
            TextPElement paragraph = listItem.newTextPElement();
            TextSoftPageBreakElement softPageBreak = listItem.newTextSoftPageBreakElement();
                       
            p0.getParentNode().insertBefore(listItem, p0);
            
            TextListItemElement listItemTest = (TextListItemElement) xpath.evaluate("//text:list-item[last()]", doc , XPathConstants.NODE);
            Assert.assertNotNull(listItemTest.getChildNodes());
            
            Assert.assertTrue(listItemTest.getChildNodes().item(0) instanceof OdfTextHeading);
            Assert.assertEquals(heading,listItemTest.getChildNodes().item(0));
            Assert.assertEquals(listItemTest.getChildNodes().item(0).getNodeName(), "text:h");
            
            Assert.assertTrue(listItemTest.getChildNodes().item(1) instanceof OdfTextList);
            Assert.assertEquals(list,listItemTest.getChildNodes().item(1));
            Assert.assertEquals(listItemTest.getChildNodes().item(1).getNodeName(), "text:list");
            
            Assert.assertTrue(listItemTest.getChildNodes().item(2) instanceof OdfTextParagraph);
            Assert.assertEquals(paragraph,listItemTest.getChildNodes().item(2));
            Assert.assertEquals(listItemTest.getChildNodes().item(2).getNodeName(), "text:p");
            
            Assert.assertTrue(listItemTest.getChildNodes().item(3) instanceof OdfTextSoftPageBreak);
            Assert.assertEquals(softPageBreak,listItemTest.getChildNodes().item(3));
            Assert.assertEquals(listItemTest.getChildNodes().item(3).getNodeName(), "text:soft-page-break");
                        
       
            doc.getOdfDocument().save(ResourceUtilities.createTestResource("CreatChildrenForTextTable.odt"));

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testCreatChildrenForGraphic() {
		try {
			
            OdfGraphicsDocument odgDoc1 = OdfGraphicsDocument.newGraphicsDocument();
            OdfFileDom doc = odgDoc1.getContentDom();

            NodeList lst = doc.getElementsByTagNameNS(
                    OdfDrawPage.ELEMENT_NAME.getUri(),
                    OdfDrawPage.ELEMENT_NAME.getLocalName());
            OdfDrawPage page = (OdfDrawPage) lst.item(lst.getLength() - 1);
            //page.setOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.DRAW), "name" ), "page1" );
            //page.setOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.DRAW), "style-name" ), "dp1" );
            //page.setOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.DRAW), "master-page-name" ), "Default" );
            page.setDrawNameAttribute("page1");
            page.setDrawStyleNameAttribute("dp1");
            page.setDrawMasterPageNameAttribute("Default");
            
            DrawLineElement line = page.newDrawLineElement("6cm", "10cm","15cm","20cm");
            //line.setOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.DRAW), "style-name" ), "gr1" );
            //line.setOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.DRAW), "text-style-name" ), "P1" );
            //line.setOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.DRAW), "layer" ), "layout" );
            line.setDrawStyleNameAttribute("gr1");
            line.setDrawTextStyleNameAttribute("P1");
            line.setDrawLayerAttribute("layer");

            DrawPageElement graphicTest = (DrawPageElement) xpath.evaluate("//draw:page[last()]", doc , XPathConstants.NODE);
            Assert.assertNotNull(graphicTest.getChildNodes());
            
            Assert.assertTrue(graphicTest.getChildNodes().item(0) instanceof DrawLineElement);
            Assert.assertEquals(line,graphicTest.getChildNodes().item(0));
            Assert.assertEquals(graphicTest.getChildNodes().item(0).getNodeName(), "draw:line");
            
            Assert.assertEquals(((DrawLineElement) graphicTest.getChildNodes().item(0)).getSvgX1Attribute().toString(),"6cm");
            Assert.assertEquals(((DrawLineElement) graphicTest.getChildNodes().item(0)).getSvgX2Attribute().toString(),"10cm");
            Assert.assertEquals(((DrawLineElement) graphicTest.getChildNodes().item(0)).getSvgY1Attribute().toString(),"15cm");
            Assert.assertEquals(((DrawLineElement) graphicTest.getChildNodes().item(0)).getSvgY2Attribute().toString(),"20cm");
            
            doc.getOdfDocument().save(ResourceUtilities.createTestResource("CreatChildrenForGraphic.odg"));

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
	
    @Test
    public void testCreatChildrenForStyles() {
        try {
            OdfDocument doc = OdfTextDocument.newTextDocument();

            OdfOfficeStyles styles = doc.getOrCreateDocumentStyles();
            StyleDefaultStyleElement def = styles.newStyleDefaultStyleElement();
            def.setStyleFamilyAttribute(OdfStyleFamily.Paragraph.toString());
            def.setProperty(StyleTextPropertiesElement.TextUnderlineColor, "#00FF00");
            
            StyleStyleElement parent =  styles.newStyleStyleElement("TheParent");
            parent.setStyleFamilyAttribute(OdfStyleFamily.Paragraph.toString());
            
            parent.setProperty(StyleTextPropertiesElement.FontSize, "17pt");
            parent.setProperty(StyleTextPropertiesElement.Color, "#FF0000");

            StyleStyleElement styleTest = (StyleStyleElement) xpath.evaluate("//style:style[last()]", doc.getStylesDom() , XPathConstants.NODE);
            Assert.assertEquals(styleTest, parent);
            
            doc.getContentDom().getOdfDocument().save(ResourceUtilities.createTestResource("CreatChildrenForStyles.odt"));
            
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Failed with " + e.getClass().getName() + ": '" + e.getMessage() + "'");
        }
    }

    @Test
    public void testCreatChildrenForEmbeddedDoc(){
    	try {
			OdfDocument document = OdfTextDocument.newTextDocument();
			document.embedDocument("Object1/", OdfTextDocument.newTextDocument());
			OdfDocument embeddedObject1 = document.getEmbeddedDocument("Object1/");
			OdfFileDom doc = embeddedObject1.getContentDom();
            // find the last paragraph
            NodeList lst = doc.getElementsByTagNameNS(
                    OdfTextParagraph.ELEMENT_NAME.getUri(),
                    OdfTextParagraph.ELEMENT_NAME.getLocalName());
            Assert.assertTrue(lst.getLength() > 0);
            OdfTextParagraph p0 = (OdfTextParagraph) lst.item(lst.getLength() - 1);

            OdfTextListItem listItem = doc.newOdfElement(OdfTextListItem.class);
            //create children elements
            TextHElement heading = listItem.newTextHElement("1");
            TextListElement list = listItem.newTextListElement();
            TextPElement paragraph = listItem.newTextPElement();
            TextSoftPageBreakElement softPageBreak = listItem.newTextSoftPageBreakElement();
                       
            p0.getParentNode().insertBefore(listItem, p0);
            
            TextListItemElement listItemTest = (TextListItemElement) xpath.evaluate("//text:list-item[last()]", doc , XPathConstants.NODE);
            Assert.assertNotNull(listItemTest.getChildNodes());
            
            Assert.assertTrue(listItemTest.getChildNodes().item(0) instanceof OdfTextHeading);
            Assert.assertEquals(heading,listItemTest.getChildNodes().item(0));
            Assert.assertEquals(listItemTest.getChildNodes().item(0).getNodeName(), "text:h");
            
            Assert.assertTrue(listItemTest.getChildNodes().item(1) instanceof OdfTextList);
            Assert.assertEquals(list,listItemTest.getChildNodes().item(1));
            Assert.assertEquals(listItemTest.getChildNodes().item(1).getNodeName(), "text:list");
            
            Assert.assertTrue(listItemTest.getChildNodes().item(2) instanceof OdfTextParagraph);
            Assert.assertEquals(paragraph,listItemTest.getChildNodes().item(2));
            Assert.assertEquals(listItemTest.getChildNodes().item(2).getNodeName(), "text:p");
            
            Assert.assertTrue(listItemTest.getChildNodes().item(3) instanceof OdfTextSoftPageBreak);
            Assert.assertEquals(softPageBreak,listItemTest.getChildNodes().item(3));
            Assert.assertEquals(listItemTest.getChildNodes().item(3).getNodeName(), "text:soft-page-break");
                        
       
            doc.getOdfDocument().save(ResourceUtilities.createTestResource("CreatChildrenForEmbedded.odt"));
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    }
    
	@Test
	public void testCreatChildrenForSpreadSheet() {
		try {
			
			OdfSpreadsheetDocument odfSpreadSheet = OdfSpreadsheetDocument.newSpreadsheetDocument();
            OdfFileDom doc = odfSpreadSheet.getContentDom();

            NodeList lst = doc.getElementsByTagNameNS(
            		OfficeSpreadsheetElement.ELEMENT_NAME.getUri(),
            		OfficeSpreadsheetElement.ELEMENT_NAME.getLocalName());
            OdfOfficeSpreadsheet sheet = (OdfOfficeSpreadsheet) lst.item(lst.getLength() - 1);
            TableTableElement table = sheet.newTableTableElement();
            //table.setOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.TABLE), "name" ), "newtable" );
            //table.setOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.TABLE), "style-name" ), "ta1" );
            table.setTableNameAttribute("newtable");
            table.setTableStyleNameAttribute("ta1");
            TableTableColumnElement column = table.newTableTableColumnElement();
            //column.setOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.TABLE), "style-name" ), "co1" );
            //column.setOdfAttribute( OdfName.get( OdfNamespace.get(OdfNamespaceNames.TABLE), "default-cell-style-name" ), "Default" );
            column.setTableStyleNameAttribute("co1");
            column.setTableDefaultCellStyleNameAttribute("Default");
            
            TableTableElement spreadsheetTest = (TableTableElement) xpath.evaluate("//table:table[last()]", doc , XPathConstants.NODE);
            Assert.assertNotNull(spreadsheetTest.getChildNodes());
            
            Assert.assertTrue(spreadsheetTest.getChildNodes().item(0) instanceof TableTableColumnElement);
            Assert.assertEquals(column, spreadsheetTest.getChildNodes().item(0));
            Assert.assertEquals(spreadsheetTest.getChildNodes().item(0).getNodeName(), "table:table-column");
            
            Assert.assertEquals(((TableTableColumnElement) spreadsheetTest.getChildNodes().item(0)).getAttribute("table:style-name"),"co1");
            Assert.assertEquals(((TableTableColumnElement) spreadsheetTest.getChildNodes().item(0)).getAttribute("table:default-cell-style-name"),"Default");
       
            doc.getOdfDocument().save(ResourceUtilities.createTestResource("CreatChildrenForSpreadsheet.ods"));

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
	
	   @Test
	    public void testCreatChildrenForForm() {
	        try {
	            OdfDocument doc = OdfTextDocument.newTextDocument();
	            OdfOfficeBody body = doc.getOfficeBody();
	  	        OfficeTextElement text =  body.newOfficeTextElement();
	            FormFormElement form = text.newFormFormElement();
	            form.setFormNameAttribute("NewFrom");
	            FormFormElement formTest = (FormFormElement) xpath.evaluate("//form:form[last()]", doc.getContentDom() , XPathConstants.NODE);
	            Assert.assertEquals(formTest, form);
	            doc.getContentDom().getOdfDocument().save(ResourceUtilities.createTestResource("CreatChildrenForForm.odt"));
	            
	        } catch (Exception e) {
	            e.printStackTrace();
	            Assert.fail("Failed with " + e.getClass().getName() + ": '" + e.getMessage() + "'");
	        }
	    }
	   
		@Test
		public void testCreatChildrenForAnimation() {
			try {
				
				OdfDocument odfdoc = OdfPresentationDocument.newPresentationDocument();
	            
	            OdfOfficePresentation presentation = OdfElement.findFirstChildNode( OdfOfficePresentation.class, odfdoc.getOfficeBody() );
	            Assert.assertNotNull(presentation);
	            
	            DrawPageElement page = presentation.newDrawPageElement("NewPage");
	            
	            AnimAnimateElement anim = page.newAnimAnimateElement("new");
	           

	            AnimAnimateElement animTest = (AnimAnimateElement) xpath.evaluate("//anim:animate[last()]", odfdoc.getContentDom() , XPathConstants.NODE);
	            
	            Assert.assertTrue(animTest instanceof AnimAnimateElement);
	            
	            Assert.assertEquals(anim,animTest);
				
	            odfdoc.save(ResourceUtilities.createTestResource("CreatChildrenForAnimateTest.odp"));

			} catch (Exception e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		}
}
