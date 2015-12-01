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
package xades4j.providers.impl;

import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import xades4j.providers.KeyInfoCertificatesProvider;
import xades4j.providers.SigningCertChainException;
import xades4j.verification.UnexpectedJCAException;

/**
 * An implementation of {@code KeyInfoCertificatesProvider} that only provides
 * the first certificate (the signing certificate).
 * @author Markus
 */
public class DefaultKeyInfoCertificatesProvider implements KeyInfoCertificatesProvider
{

    @Override
    public List<X509Certificate> getCertificates(List<X509Certificate> signingCertificateChain) throws SigningCertChainException, UnexpectedJCAException
    {
        return Collections.singletonList(signingCertificateChain.get(0));
    }

}
