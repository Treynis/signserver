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
package org.odftoolkit.odfdom.doc;

import org.junit.Assert;
import org.junit.Test;
import org.odftoolkit.odfdom.doc.OdfDocument;
import org.odftoolkit.odfdom.OdfFileDom;
import org.odftoolkit.odfdom.doc.OdfSpreadsheetDocument;
import org.odftoolkit.odfdom.doc.style.OdfStyle;
import org.odftoolkit.odfdom.doc.table.OdfTable;
import org.odftoolkit.odfdom.doc.table.OdfTableCell;
import org.odftoolkit.odfdom.doc.table.OdfTableRow;
import org.odftoolkit.odfdom.doc.text.OdfTextParagraph;
import org.odftoolkit.odfdom.OdfNamespace;
import org.odftoolkit.odfdom.dom.OdfNamespaceNames;
import org.odftoolkit.odfdom.dom.element.table.TableTableElement;
import org.odftoolkit.odfdom.utils.ResourceUtilities;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class TableTest {
    
    private static final String TEST_FILE_SAVE_2TABLES_OUT = "TestSave2Tables.odt";

    public TableTest() {
    }
    
    @Test
    public void testTable() {
        try {
            OdfDocument odfdoc = OdfDocument.loadDocument(ResourceUtilities.getTestResource("table.odt"));
            NodeList lst = odfdoc.getContentDom().getElementsByTagNameNS(OdfNamespaceNames.TABLE.getNamespaceUri(), "table");
            int tscount = 0;
            for (int i = 0; i < lst.getLength(); i++) {
                Node node = lst.item(i);
                Assert.assertTrue(node instanceof OdfTable);
                OdfTable te = (OdfTable) lst.item(i);

                OdfStyle ds = te.getDocumentStyle();
                Assert.assertNull(ds);

                if (te.hasAutomaticStyle()) {
                    te.getAutomaticStyle();
                    tscount++;
                }
            }
            Assert.assertTrue(tscount > 0);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testCellsAndRows() {
        try {
            OdfDocument odfdoc = OdfDocument.loadDocument(ResourceUtilities.getTestResource("table.odt"));
            NodeList lst = odfdoc.getContentDom().getElementsByTagNameNS(OdfNamespaceNames.TABLE.getNamespaceUri(), "table-cell");
            for (int i = 0; i < lst.getLength(); i++) {
                Node node = lst.item(i);
                Assert.assertTrue(node instanceof OdfTableCell);
                OdfTableCell td = (OdfTableCell) lst.item(i);
                OdfTableRow tr = td.getTableRow();
                Assert.assertNotNull(tr);

                OdfTable table = tr.getTable();
                Assert.assertNotNull(table);
                Assert.assertTrue(table == td.getTable());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void create2ndTableTab() throws Exception {
        OdfSpreadsheetDocument mysheet = OdfSpreadsheetDocument.newSpreadsheetDocument();
        OdfFileDom odt = mysheet.getContentDom();

        // find the first table in the sheet
        NodeList lst =
                odt.getElementsByTagNameNS(TableTableElement.ELEMENT_NAME.getUri(), TableTableElement.ELEMENT_NAME.getLocalName());
        OdfTable mytable = (OdfTable) lst.item(0);
        mytable.setTableNameAttribute("Cars Sheet");

        // remove first empty row of table.
        mytable.removeChild(mytable.getFirstChild().getNextSibling());

        OdfTableRow row = (OdfTableRow) mytable.appendChild(new OdfTableRow(odt));
        OdfTableCell cell = (OdfTableCell) row.appendChild(new OdfTableCell(odt));

        OdfTextParagraph p = new OdfTextParagraph(odt);
        p.appendChild(odt.createTextNode("Corvette"));
        cell.appendChild(p);

        // 2nd Table
        OdfTable my2table = new OdfTable(odt);

        Element spreadsheetElement = (Element) odt.getElementsByTagNameNS(OdfNamespaceNames.OFFICE.getNamespaceUri(), "spreadsheet").item(0);
        my2table.setAttributeNS(OdfNamespaceNames.TABLE.getNamespaceUri(), "name", "BikesSheet");

        spreadsheetElement.appendChild(my2table);

        OdfTableRow row2 = (OdfTableRow) my2table.appendChild(new OdfTableRow(odt));
        OdfTableCell cell2 = (OdfTableCell) row2.appendChild(new OdfTableCell(odt));

        OdfTextParagraph p2 = new OdfTextParagraph(odt);
        p2.appendChild(odt.createTextNode("Bandit 600"));
        cell2.appendChild(p2);

        mysheet.save(ResourceUtilities.getTestOutput(TEST_FILE_SAVE_2TABLES_OUT));
    }
}
