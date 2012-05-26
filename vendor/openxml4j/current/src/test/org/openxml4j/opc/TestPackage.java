/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== 

 * Copyright (c) 2006, Wygwam
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, 
 * are permitted provided that the following conditions are met: 
 * 
 * - Redistributions of source code must retain the above copyright notice, 
 * this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, 
 * this list of conditions and the following disclaimer in the documentation and/or 
 * other materials provided with the distribution.
 * - Neither the name of Wygwam nor the names of its contributors may be 
 * used to endorse or promote products derived from this software without 
 * specific prior written permission. 
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY 
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES 
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
 * IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, 
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT 
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package test.org.openxml4j.opc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.TreeMap;

import junit.framework.TestCase;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.openxml4j.exceptions.InvalidFormatException;
import org.openxml4j.opc.ContentTypes;
import org.openxml4j.opc.Package;
import org.openxml4j.opc.PackageAccess;
import org.openxml4j.opc.PackagePart;
import org.openxml4j.opc.PackagePartName;
import org.openxml4j.opc.PackageRelationship;
import org.openxml4j.opc.PackageRelationshipCollection;
import org.openxml4j.opc.PackageRelationshipTypes;
import org.openxml4j.opc.PackagingURIHelper;
import org.openxml4j.opc.StreamHelper;
import org.openxml4j.opc.TargetMode;
import org.openxml4j.opc.internal.ContentTypeManager;
import org.openxml4j.opc.internal.FileHelper;

import test.TestCore;
import test.junitx.framework.ZipFileAssert;

public class TestPackage extends TestCase {

	TestCore testCore = new TestCore(this.getClass());

	/**
	 * Test that just opening and closing the file doesn't alter the document.
	 */
	public void testOpenSave() throws Exception {
		File originalFile = new File(testCore.getTestRootPath()
				+ File.separator + "INPUT" + File.separator
				+ "TestPackageCommon.docx");
		File targetFile = new File(testCore.getTestRootPath() + File.separator
				+ "OUTPUT" + File.separator + "TestPackageOpenSaveTMP.docx");
		assertTrue("Source file " + originalFile + " doesn't exist!", originalFile.exists());

		Package p = Package.open(originalFile.getAbsolutePath(),
				PackageAccess.READ_WRITE);
		p.save(targetFile.getAbsoluteFile());

		// Compare the original and newly saved document
		assertTrue(targetFile.exists());
		ZipFileAssert.assertEquals(originalFile, targetFile);
		assertTrue(targetFile.delete());
	}
	
	/**
	 * Test that when we create a new Package, we give it
	 *  the correct default content types
	 */
	public void testCreateGetsContentTypes() throws Exception {
		File targetFile = new File(testCore.getTestRootPath()
				+ File.separator + "OUTPUT" + File.separator 
				+ "TestCreatePackageTMP.docx");
		
		// Zap the target file, in case of an earlier run
		if(targetFile.exists()) targetFile.delete();
		
		Package pkg = Package.create(targetFile, true);
		
		// Check it has content types for rels and xml
		ContentTypeManager ctm = getContentTypeManager(pkg);
		assertEquals(
				"application/xml",
				ctm.getContentType(
						PackagingURIHelper.createPartName("/foo.xml")
				)
		);
		assertEquals(
				ContentTypes.RELATIONSHIPS_PART,
				ctm.getContentType(
						PackagingURIHelper.createPartName("/foo.rels")
				)
		);
		assertNull(
				ctm.getContentType(
						PackagingURIHelper.createPartName("/foo.txt")
				)
		);
	}

	/**
	 * Test package creation.
	 */
	public void testCreatePackageAddPart() throws Exception {
		File targetFile = new File(testCore.getTestRootPath()
				+ File.separator + "OUTPUT" + File.separator 
				+ "TestCreatePackageTMP.docx");

		File expectedFile = new File(testCore.getTestRootPath()
				+ File.separator + "OUTPUT" + File.separator
				+ "TestCreatePackageOUTPUT.docx");

		// Zap the target file, in case of an earlier run
		if(targetFile.exists()) targetFile.delete();
		
		// Create a package
		Package pkg = Package.create(targetFile, true);
		PackagePartName corePartName = PackagingURIHelper
				.createPartName("/word/document.xml");

		pkg.addRelationship(corePartName, TargetMode.INTERNAL,
				PackageRelationshipTypes.CORE_DOCUMENT, "rId1");

		PackagePart corePart = pkg
				.createPart(
						corePartName,
						"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml");

		Document doc = DocumentHelper.createDocument();
		Namespace nsWordprocessinML = new Namespace("w",
				"http://schemas.openxmlformats.org/wordprocessingml/2006/main");
		Element elDocument = doc.addElement(new QName("document",
				nsWordprocessinML));
		Element elBody = elDocument.addElement(new QName("body",
				nsWordprocessinML));
		Element elParagraph = elBody.addElement(new QName("p",
				nsWordprocessinML));
		Element elRun = elParagraph
				.addElement(new QName("r", nsWordprocessinML));
		Element elText = elRun.addElement(new QName("t", nsWordprocessinML));
		elText.setText("Hello Open XML !");

		StreamHelper.saveXmlInStream(doc, corePart.getOutputStream());
		pkg.close();

		ZipFileAssert.assertEquals(expectedFile, targetFile);
		assertTrue(targetFile.delete());
	}
	
	/**
	 * Tests that we can create a new package, add a core
	 *  document and another part, save and re-load and
	 *  have everything setup as expected
	 */
	public void testCreatePackageWithCoreDocument() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Package pkg = Package.create(baos);
		
		// Add a core document
        PackagePartName corePartName = PackagingURIHelper.createPartName("/xl/workbook.xml");
        // Create main part relationship
        pkg.addRelationship(corePartName, TargetMode.INTERNAL, PackageRelationshipTypes.CORE_DOCUMENT, "rId1");
        // Create main document part
        PackagePart corePart = pkg.createPart(corePartName, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml");
        // Put in some dummy content
        OutputStream coreOut = corePart.getOutputStream();
        coreOut.write("<dummy-xml />".getBytes());
        coreOut.close();
		
		// And another bit
        PackagePartName sheetPartName = PackagingURIHelper.createPartName("/xl/worksheets/sheet1.xml");
        PackageRelationship rel =
        	 corePart.addRelationship(sheetPartName, TargetMode.INTERNAL, "http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet", "rSheet1");
        PackagePart part = pkg.createPart(sheetPartName, "application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml");
        // Dummy content again
        coreOut = corePart.getOutputStream();
        coreOut.write("<dummy-xml2 />".getBytes());
        coreOut.close();
        
        
        // Check things are as expected
        PackageRelationshipCollection coreRels =
        	pkg.getRelationshipsByType(PackageRelationshipTypes.CORE_DOCUMENT);
        assertEquals(1, coreRels.size());
        PackageRelationship coreRel = coreRels.getRelationship(0);
        assertEquals("/", coreRel.getSourceURI().toString());
        assertEquals("/xl/workbook.xml", coreRel.getTargetURI().toString());
        assertNotNull(pkg.getPart(coreRel));
        
        
        // Save and re-load
        pkg.close();
        FileOutputStream fout = new FileOutputStream("/tmp/new.zip");
        fout.write(baos.toByteArray());
        fout.close();
        pkg = Package.open(new ByteArrayInputStream(baos.toByteArray()));
        
        
        // Check still right
        coreRels = pkg.getRelationshipsByType(PackageRelationshipTypes.CORE_DOCUMENT);
        assertEquals(1, coreRels.size());
        coreRel = coreRels.getRelationship(0);
        assertEquals("/", coreRel.getSourceURI().toString());
        assertEquals("/xl/workbook.xml", coreRel.getTargetURI().toString());
        assertNotNull(pkg.getPart(coreRel));
	}

	/**
	 * Test package opening.
	 */
	public void testOpenPackage() throws Exception {
		File targetFile = new File(testCore.getTestRootPath() + File.separator
				+ "OUTPUT" + File.separator + "TestOpenPackageTMP.docx");

		File inputFile = new File(testCore.getTestRootPath() + File.separator
				+ "INPUT" + File.separator + "TestOpenPackageINPUT.docx");

		File expectedFile = new File(testCore.getTestRootPath()
				+ File.separator + "OUTPUT" + File.separator
				+ "TestOpenPackageOUTPUT.docx");

		// Copy the input file in the output directory
		FileHelper.copyFile(inputFile, targetFile);

		// Create a package
		Package pkg = Package.open(targetFile.getAbsolutePath());

		// Modify core part
		PackagePartName corePartName = PackagingURIHelper
				.createPartName("/word/document.xml");

		PackagePart corePart = pkg.getPart(corePartName);

		// Delete some part to have a valid document
		for (PackageRelationship rel : corePart.getRelationships()) {
			corePart.removeRelationship(rel.getId());
			pkg.removePart(PackagingURIHelper.createPartName(PackagingURIHelper
					.resolvePartUri(corePart.getPartName().getURI(), rel
							.getTargetURI())));
		}

		// Create a content
		Document doc = DocumentHelper.createDocument();
		Namespace nsWordprocessinML = new Namespace("w",
				"http://schemas.openxmlformats.org/wordprocessingml/2006/main");
		Element elDocument = doc.addElement(new QName("document",
				nsWordprocessinML));
		Element elBody = elDocument.addElement(new QName("body",
				nsWordprocessinML));
		Element elParagraph = elBody.addElement(new QName("p",
				nsWordprocessinML));
		Element elRun = elParagraph
				.addElement(new QName("r", nsWordprocessinML));
		Element elText = elRun.addElement(new QName("t", nsWordprocessinML));
		elText.setText("Hello Open XML !");

		StreamHelper.saveXmlInStream(doc, corePart.getOutputStream());

		// Save and close
		try {
			pkg.close();
		} catch (IOException e) {
			fail();
		}

		ZipFileAssert.assertEquals(expectedFile, targetFile);
		assertTrue(targetFile.delete());
	}
	
	/**
	 * Checks that we can write a package to a simple
	 *  OutputStream, in addition to the normal writing
	 *  to a file
	 */
	public void testSaveToOutputStream() throws Exception {
		File originalFile = new File(testCore.getTestRootPath()
				+ File.separator + "INPUT" + File.separator
				+ "TestPackageCommon.docx");
		File targetFile = new File(testCore.getTestRootPath() + File.separator
				+ "OUTPUT" + File.separator + "TestPackageOpenSaveTMP.docx");
		assertTrue("Source file " + originalFile + " doesn't exist!", originalFile.exists());

		Package p = Package.open(originalFile.getAbsolutePath(),
				PackageAccess.READ_WRITE);
		FileOutputStream fout = new FileOutputStream(targetFile);
		p.save(fout);
		fout.close();

		// Compare the original and newly saved document
		assertTrue(targetFile.exists());
		ZipFileAssert.assertEquals(originalFile, targetFile);
		assertTrue(targetFile.delete());
	}

	/**
	 * Checks that we can open+read a package from a
	 *  simple InputStream, in addition to the normal
	 *  reading from a file
	 */
	public void testOpenFromInputStream() throws Exception {
		File originalFile = new File(testCore.getTestRootPath()
				+ File.separator + "INPUT" + File.separator
				+ "TestPackageCommon.docx");
		assertTrue("Source file " + originalFile + " doesn't exist!", originalFile.exists());
		
		FileInputStream finp = new FileInputStream(originalFile);
		
		Package p = Package.open(finp);
		
		assertNotNull(p);
		assertNotNull(p.getRelationships());
		assertEquals(12, p.getParts().size());
		
		// Check it has the usual bits
		assertTrue(p.hasRelationships());
		assertTrue(p.containPart(PackagingURIHelper.createPartName("/_rels/.rels")));
	}

	public void testRemovePartRecursive() throws Exception {
		File originalFile = new File(testCore.getTestRootPath()
				+ File.separator + "INPUT" + File.separator
				+ "TestPackageCommon.docx");
		File targetFile = new File(testCore.getTestRootPath() + File.separator
				+ "OUTPUT" + File.separator
				+ "TestPackageRemovePartRecursiveOUTPUT.docx");
		File tempFile = new File(testCore.getTestRootPath() + File.separator
				+ "OUTPUT" + File.separator
				+ "TestPackageRemovePartRecursiveTMP.docx");

		Package p = Package.open(originalFile.getAbsolutePath(),
				PackageAccess.READ_WRITE);
		p.removePartRecursive(PackagingURIHelper.createPartName(new URI(
				"/word/document.xml")));
		p.save(tempFile.getAbsoluteFile());

		// Compare the original and newly saved document
		assertTrue(targetFile.exists());
		ZipFileAssert.assertEquals(targetFile, tempFile);
		assertTrue(targetFile.delete());
	}

	public void testDeletePart() throws InvalidFormatException {
		TreeMap<PackagePartName, String> expectedValues;
		TreeMap<PackagePartName, String> values;

		values = new TreeMap<PackagePartName, String>();

		// Expected values
		expectedValues = new TreeMap<PackagePartName, String>();
		expectedValues.put(PackagingURIHelper.createPartName("/_rels/.rels"),
				"application/vnd.openxmlformats-package.relationships+xml");

		expectedValues
				.put(PackagingURIHelper.createPartName("/docProps/app.xml"),
						"application/vnd.openxmlformats-officedocument.extended-properties+xml");
		expectedValues.put(PackagingURIHelper
				.createPartName("/docProps/core.xml"),
				"application/vnd.openxmlformats-package.core-properties+xml");
		expectedValues
				.put(PackagingURIHelper.createPartName("/word/fontTable.xml"),
						"application/vnd.openxmlformats-officedocument.wordprocessingml.fontTable+xml");
		expectedValues.put(PackagingURIHelper
				.createPartName("/word/media/image1.gif"), "image/gif");
		expectedValues
				.put(PackagingURIHelper.createPartName("/word/settings.xml"),
						"application/vnd.openxmlformats-officedocument.wordprocessingml.settings+xml");
		expectedValues
				.put(PackagingURIHelper.createPartName("/word/styles.xml"),
						"application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml");
		expectedValues.put(PackagingURIHelper
				.createPartName("/word/theme/theme1.xml"),
				"application/vnd.openxmlformats-officedocument.theme+xml");
		expectedValues
				.put(
						PackagingURIHelper
								.createPartName("/word/webSettings.xml"),
						"application/vnd.openxmlformats-officedocument.wordprocessingml.webSettings+xml");

		String filepath = testCore.getTestRootPath() + "INPUT" + File.separator
				+ "sample.docx";

		Package p = Package.open(filepath, PackageAccess.READ_WRITE);
		// Remove the core part
		p.deletePart(PackagingURIHelper.createPartName("/word/document.xml"));

		for (PackagePart part : p.getParts()) {
			values.put(part.getPartName(), part.getContentType());
			System.out.println(part.getPartName());
		}

		// Compare expected values with values return by the package
		for (PackagePartName partName : expectedValues.keySet()) {
			assertNotNull(values.get(partName));
			assertEquals(expectedValues.get(partName), values.get(partName));
		}
		// Don't save modfications
		p.revert();
	}
	
	public void testDeletePartRecursive() throws InvalidFormatException {
		TreeMap<PackagePartName, String> expectedValues;
		TreeMap<PackagePartName, String> values;

		values = new TreeMap<PackagePartName, String>();

		// Expected values
		expectedValues = new TreeMap<PackagePartName, String>();
		expectedValues.put(PackagingURIHelper.createPartName("/_rels/.rels"),
				"application/vnd.openxmlformats-package.relationships+xml");

		expectedValues
				.put(PackagingURIHelper.createPartName("/docProps/app.xml"),
						"application/vnd.openxmlformats-officedocument.extended-properties+xml");
		expectedValues.put(PackagingURIHelper
				.createPartName("/docProps/core.xml"),
				"application/vnd.openxmlformats-package.core-properties+xml");

		String filepath = testCore.getTestRootPath() + "INPUT" + File.separator
				+ "sample.docx";

		Package p = Package.open(filepath, PackageAccess.READ_WRITE);
		// Remove the core part
		p.deletePartRecursive(PackagingURIHelper.createPartName("/word/document.xml"));

		for (PackagePart part : p.getParts()) {
			values.put(part.getPartName(), part.getContentType());
			System.out.println(part.getPartName());
		}

		// Compare expected values with values return by the package
		for (PackagePartName partName : expectedValues.keySet()) {
			assertNotNull(values.get(partName));
			assertEquals(expectedValues.get(partName), values.get(partName));
		}
		// Don't save modfications
		p.revert();
	}
	
	private static ContentTypeManager getContentTypeManager(Package pkg) throws Exception {
		Field f = Package.class.getDeclaredField("contentTypeManager");
		f.setAccessible(true);
		return (ContentTypeManager)f.get(pkg);
	}
}
