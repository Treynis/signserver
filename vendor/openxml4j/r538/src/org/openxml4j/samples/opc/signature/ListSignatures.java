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

package org.openxml4j.samples.opc.signature;

import java.util.List;

import org.openxml4j.exceptions.OpenXML4JException;
import org.openxml4j.opc.Package;
import org.openxml4j.opc.PackageAccess;
import org.openxml4j.opc.signature.PackageDigitalSignature;
import org.openxml4j.opc.signature.PackageDigitalSignatureManager;
import org.openxml4j.opc.signature.PackageRelationshipSelector;
import org.openxml4j.opc.signature.PartIdentifier;
import org.openxml4j.opc.signature.RelationshipIdentifier;
import org.openxml4j.opc.signature.VerifyResult;
import org.openxml4j.samples.DemoCore;

/**
 * 
 * sample that lists digital signatures in a package
 * 
 * @author aziz.goktepe (aka rayback_2)
 * 
 */
public class ListSignatures {

	/**
	 * @param args
	 * @throws OpenXML4JException
	 */
	public static void main(String[] args) throws OpenXML4JException {
		try {
			DemoCore demoCore = new DemoCore();
			
			//specify file holding signatures
			String filepath = demoCore.getTestRootPath() + "sample_signed.docx";
//			filepath = demoCore.getTestRootPath() + "sample_signed_twice.docx"; // document having 2 signatures

			Package p = Package.open(filepath, PackageAccess.READ);
			PackageDigitalSignatureManager dsm = new PackageDigitalSignatureManager(
					p);

			//is package signed at all ?
			System.out.println("Is Package Signed : " + dsm.getIsSigned());

			if (dsm.getIsSigned()) {
				
				// get signature origin part uri
				System.out.println("Digital Signature Origin Part URI "
						+ dsm.getOriginPart().getPartName().getURI());

				// try to verify all signatures at once
				try {
					VerifyResult allSigVerifyResult = dsm.VerifySignatures();

					if (allSigVerifyResult != VerifyResult.Success) {
						System.out
								.println("At Least one signature in package has problems with verification !");
					} else {
						System.out
								.println("All signatures this package verified OK");
					}
				} catch (Exception e) {
					System.out
							.println("At Least one signature in package has problems with verification !");
				}

				System.out.println();
				
				// get signatures in package
				List<PackageDigitalSignature> sigs = dsm.getSignatures();
				System.out.println("Number of signatures found : " + sigs.size());
				System.out.println("Listing signatures in package");
				
				// try to verify and print detailed information about each
				// signature separately
				// note : even if we haven't called Verify() method, it would be called on some getters of
				// packagedigitalsignature implicitly. 
				// see comments for getters

				for (PackageDigitalSignature sig : sigs) {
					
					//verify signature with certificate (or public key) found inside package
					//we could also call overload of verify to specify with which certificate to verify signature
					sig.Verify();
					
					//print signature information
					System.out.println();
					System.out.println("Signature part : "
							+ sig.getSignaturePart().getPartName().getURI());
					System.out.println("Id of signature : "
							+ sig.getSignature().getId());
					System.out.println("Is signature valid : "
							+ sig.getIsSignatureValid());
					System.out.println("Signature value length : "
							+ sig.getSignatureValue().length);
					System.out.println("Signing public key algorithm : "
							+ sig.getSigningPublicKey().getAlgorithm());
					System.out.println("Signer DN : "
							+ sig.getSigner().getSubjectDN());

					System.out.println();
					
					// retrieve signed parts and print (which parts are signed by this signature)
					System.out
							.println("----- parts signed by this signature BEGINS-----");

					if (sig.getSignedParts() != null) {
						for (PartIdentifier pIdent : sig.getSignedParts()) {
							System.out.println(pIdent.getPartURI());
						}
					} else {
						// hmm might it be the case that relationships only are
						// signed but no part ?
						System.out.println("NONE");
					}

					System.out
							.println("----- parts signed by this signature ENDS-----");

					System.out.println();
					
					
					// retrieve signed relationships and print (which relationship parts and which relationship in each of those parts are signed)
					System.out
							.println("----- relationships signed by this signature BEGINS-----");
					if (sig.getSignedRelationshipSelectors() != null) {
						for (PackageRelationshipSelector relSel : sig
								.getSignedRelationshipSelectors()) {

							System.out
									.println(relSel.getRelationshipPartName());
							
							// see if all relationships in relationships part
							// are signed, or is it selected by id or sourcetype
							if (relSel.getIsAllRelationshipsIncluded()) {
								System.out
										.println("\t ****All relationships in this relationship part are signed (no relationship transform) ****");
							} else {
								
								// ok we got relationships selected by some
								// criterie (source id or type) print them
								System.out
										.println("\t ****Relationship identifiers for this relationship (that is relationships that are included in signature) ****");
								for (RelationshipIdentifier relIdent : relSel
										.getRelationshipIdentifiers()) {
									System.out.print("\t");
									System.out.println(relIdent);
								}
							}

						}
					} else {
						// hmm might it be the case that no relationship is
						// signed but only parts are
						System.out.println("NONE");
					}
					
					System.out
							.println("----- relationships signed by this signature ENDS-----");

					// get Signature Time and Signature time format
					System.out.println("Signature Time is : "
							+ sig.getSigningTimeStringValue());
					System.out.println("Signature Time Format is : "
							+ sig.getTimeFormat());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
