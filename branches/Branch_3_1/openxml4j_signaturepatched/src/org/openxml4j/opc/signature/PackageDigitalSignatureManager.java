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

package org.openxml4j.opc.signature;

import java.util.ArrayList;
import java.util.List;

import org.openxml4j.exceptions.InvalidFormatException;
import org.openxml4j.exceptions.OpenXML4JException;
import org.openxml4j.exceptions.OpenXML4JRuntimeException;
import org.openxml4j.opc.CertificateEmbeddingOption;
import org.openxml4j.opc.Package;
import org.openxml4j.opc.PackagePart;
import org.openxml4j.opc.PackagePartName;
import org.openxml4j.opc.PackageRelationship;
import org.openxml4j.opc.PackageRelationshipTypes;
import org.openxml4j.opc.PackagingURIHelper;
import org.openxml4j.opc.RelationshipSource;
import org.openxml4j.opc.TargetMode;
import org.openxml4j.opc.internal.signature.DigitalSignatureOriginPart;

public final class PackageDigitalSignatureManager {

	private final static String defaultHashAlgorithm = "http://www.w3.org/2000/09/xmldsig#sha1";

	private final static PackagePartName defaultOriginPartName;

	private CertificateEmbeddingOption embeddingOption;

	private Package _container;

	private String hashAlgorithm = defaultHashAlgorithm;

	/**
	 * The digital signature origin part.
	 */
	private DigitalSignatureOriginPart originPart;

	/**
	 * Signatures currently in the package.
	 */
	private List<PackageDigitalSignature> signatures;

	/**
	 * Flag that indicates if the search for the origin has already be done for
	 * speed up matter.
	 */
	private boolean originPartSearchDone = false;

	static {
		try {
			defaultOriginPartName = PackagingURIHelper
					.createPartName("/digital-signature/origin.psdsor");
		} catch (InvalidFormatException e) {
			throw new OpenXML4JRuntimeException("");
		}
	}

	public void addSignaturePart(PackageDigitalSignature digitalSignature) {

	}

	public void signPart(PackagePart part,
			PackageDigitalSignature digitalSignature) {

	}

	/**
	 * [M6.2] If there are no Digital Signature XML Signature parts in the
	 * package, the Digital Signature Origin part is optional.
	 * 
	 * @param part
	 *            The part
	 */
	public void unsignPart(PackagePart part) {

	}

	/**
	 * Ensure that the origin digital part exist, if not it's created.
	 * 
	 * [M6.1] When creating the first Digital Signature XML Signature part, the
	 * package implementer shall create the Digital Signature Origin part, if it
	 * does not exist, in order to specify a relationship to that Digital
	 * Signature XML Signature part.
	 * 
	 * @param digitalSignaturePart
	 *            If no origin digital signature part is already set, this
	 *            digital signature part will be specify as the origin digital
	 *            signature part.
	 */
	private void ensureOriginDigitalPart(
			DigitalSignatureOriginPart originSignaturePart) {
		if (originSignaturePart == null)
			throw new IllegalArgumentException(
					"originSignaturePart can't be null");

		if (this.originPart == null) {
			this.originPart = originPart;
			// TODO Cr��er la partie
		}
	}

	private void ensureSignatures() {
		if (this.signatures == null) {
			this.signatures = new ArrayList<PackageDigitalSignature>();

		}
	}

	private boolean originParExists() {
		if (!this.originPartSearchDone) {
			try
			{
			for (PackageRelationship rel : this._container
					.getRelationshipsByType(PackageRelationshipTypes.DIGITAL_SIGNATURE_ORIGIN)) {
				if (rel.getTargetMode() != TargetMode.INTERNAL)
					throw new InvalidFormatException("");
				// TODO Complete
			}
			} catch (OpenXML4JException e)
			{
				
			}
		}
		return this.originPartSearchDone;
	}
}