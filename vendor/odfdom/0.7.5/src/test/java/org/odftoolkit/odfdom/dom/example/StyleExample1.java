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
package org.odftoolkit.odfdom.dom.example;


public class StyleExample1 {
    
    public static void main(String[] args) {
/* todo: refactor            
        try {
            OdfDocument odfdoc = OdfDocument.OdfDocument("test/resources/test1.odt");
            System.out.println("parsed document.");
            
            OdfElement e = (OdfElement) odfdoc.getContentDom().getDocumentElement();
            NodeAction dumpStyles = new NodeAction() {
                protected void apply(Node node, Object arg, int depth) {
                    String indent = new String();
                    for (int i=0; i<depth; i++) indent += "  ";
                    System.out.print(indent + node.getNodeName());
                    if (node.getNodeType() == Node.TEXT_NODE) {
                        System.out.print(": " + node.getNodeValue());
                    }
                    System.out.println();
                    if (node instanceof OdfStylableElement) {
                        try {
                            System.out.println(indent + "-style info...");
                            OdfStylableElement se = (OdfStylableElement) node;
                            OdfStyle ds = se.getDocumentStyle();
                            OdfStyle ls = se.getAutomaticStyle();
                            if (ls != null) {
                                System.out.println(indent + "-OdfLocalStyle: " + ls);
                            }
                            if (ds != null) {
                                System.out.println(indent + "-OdfDocumentStyle: " + ds);
                            }
                        } catch (Exception ex) {
                            Logger.getLogger(StyleExample1.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            };       
            dumpStyles.performAction(e, null);                
            // serializeXml(e, System.out);                                    
        } catch (Exception e) {
            e.printStackTrace();
        }
 */
    }
}
