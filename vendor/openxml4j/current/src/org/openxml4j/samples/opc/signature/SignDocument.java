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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.openxml4j.opc.Package;
import org.openxml4j.opc.PackageAccess;
import org.openxml4j.opc.signature.PackageDigitalSignatureManager;
import org.openxml4j.samples.DemoCore;

/**
 * 
 * sample that adds digital signature to document. It uses p12 file to sign document.
 * 
 * @author aziz.goktepe (aka rayback_2)
 * 
 */
public class SignDocument {

	/**
	 * @param args
	 * 
	 */
	public static void main(String[] args) {

		try {

			DemoCore demoCore = new DemoCore();

			// we are signing document with certificate and key from sample p12
			// keystore. 
			String keyStorePath = demoCore.getTestRootPath() + "pdfsigner.p12";
			String keyPin = "foo123";
			String keystorePin = "foo123";
			String keyAlias = "pdfsigner";

			//specify which file to sign
			String filepath = demoCore.getTestRootPath() + "sample.docx";
			
			//specify where to save signed file to
			String signedFilePath = demoCore.getTestRootPath()
					+ "sample_signed.docx";

			//open package with READ_WRITE access (can't apply signature if read only opened)
			Package p = Package.open(filepath, PackageAccess.READ_WRITE);
			
			//create digital signature manager object
			PackageDigitalSignatureManager dsm = new PackageDigitalSignatureManager(
					p);

			//retrieve signing private key and certificate from p12 keystore
			PrivateKey signingPrivateKey = getPrivateKey(keyStorePath,
					keystorePin, keyPin, keyAlias);
			X509Certificate signingCertificate = getCertificate(keyStorePath,
					keystorePin, keyAlias);

			// sign document 
			dsm.SignDocument(signingPrivateKey, signingCertificate);

			// save output to file
			File destFileSon = new File(signedFilePath);
			dsm.getContainer().save(destFileSon);
			
			//print something
			System.out.println("Signing of document completed successfully");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * sample helper methods for getting signing key and certificate from
	 * keystore
	 */

	public static PrivateKey getPrivateKey(String store, String sPass,
			String kPass, String alias) throws CertificateException,
			IOException, UnrecoverableKeyException, KeyStoreException,
			NoSuchAlgorithmException, NoSuchProviderException {
		return getKeyPair(store, sPass, kPass, alias).getPrivate();
	}

	public static X509Certificate getCertificate(String store, String sPass,
			String alias) throws KeyStoreException, NoSuchAlgorithmException,
			CertificateException, IOException, NoSuchProviderException {
		KeyStore ks = loadKeyStore(store, sPass);
		Certificate cert = ks.getCertificate(alias);
		return (X509Certificate) cert;
	}

	public static KeyPair getKeyPair(String store, String sPass, String kPass,
			String alias) throws CertificateException, IOException,
			UnrecoverableKeyException, KeyStoreException,
			NoSuchAlgorithmException, NoSuchProviderException {
		KeyStore ks = loadKeyStore(store, sPass);
		Key key = null;
		PublicKey publicKey = null;
		PrivateKey privateKey = null;
		if (ks.containsAlias(alias)) {
			key = ks.getKey(alias, kPass.toCharArray());
			if (key instanceof PrivateKey) {
				Certificate cert = ks.getCertificate(alias);
				publicKey = cert.getPublicKey();
				privateKey = (PrivateKey) key;

				return new KeyPair(publicKey, privateKey);

			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	private static KeyStore loadKeyStore(String store, String sPass)
			throws KeyStoreException, NoSuchAlgorithmException,
			CertificateException, IOException, NoSuchProviderException {
		Security
				.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		KeyStore myKS = KeyStore.getInstance("PKCS12", "BC");
		FileInputStream fis = new FileInputStream(store);
		myKS.load(fis, sPass.toCharArray());
		fis.close();
		return myKS;
	}

}
