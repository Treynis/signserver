/*
 * XAdES4j - A Java library for generation and verification of XAdES signatures.
 * Copyright (C) 2010 Luis Goncalves.
 *
 * XAdES4j is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or any later version.
 *
 * XAdES4j is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License along
 * with XAdES4j. If not, see <http://www.gnu.org/licenses/>.
 */
package xades4j.providers;

import java.security.cert.X509Certificate;
import java.util.List;
import xades4j.verification.UnexpectedJCAException;

/**
 * Used in signature production to get the certificate(s) to include in the 
 * KeyInfo structure.
 * @see xades4j.production.XadesSigningProfile
 * @author Markus
 */
public interface KeyInfoCertificatesProvider
{
    /**
     * Gets the signing certificates to be used in the KeyInfo structure.
     * At least the signing certificate must be present. Other certificates may
     * be present, possibly up to the trust anchor.
     * @param signingCertificateChain the complete signing certificate chain
     * @return the signing certificate (chain)
     * @throws SigningCertChainException if the signing certificate (chain) couldn't be obtained
     * @throws UnexpectedJCAException when an unexpected platform error occurs
     */
    List<X509Certificate> getCertificates(List<X509Certificate> signingCertificateChain) throws SigningCertChainException, UnexpectedJCAException;

}
