/*************************************************************************
 *                                                                       *
 *  SignServer: The OpenSource Automated Signing Server                  *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.signserver.validationservice.server;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.cert.CRLException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.CRLNumber;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.IssuingDistributionPoint;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.X509KeyUsage;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.x509.X509V2CRLGenerator;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.cesecore.util.Base64;
import org.cesecore.util.CertTools;
import org.ejbca.core.model.ca.caadmin.IllegalKeyStoreException;
import org.ejbca.core.model.ca.catoken.CATokenOfflineException;
import org.ejbca.core.model.ca.crl.RevokedCertInfo;
import org.signserver.common.SignServerUtil;

/**
 * Contains help methods for testing the validation service
 * 
 * 
 * @author Philip Vendil 30 nov 2007
 *
 * @version $Id$
 */
public class ValidationTestUtils {

    public static X509Certificate genCert(long validity, String sigAlg, String dn, PrivateKey privKey, PublicKey pubKey, Date startDate, Date endDate, boolean isCA)
            throws CertificateEncodingException, InvalidKeyException,
                   IllegalStateException, NoSuchAlgorithmException,
                   SignatureException, IOException, NoSuchProviderException,
                   OperatorCreationException, 
                   CertificateException {
        return genCert(validity, sigAlg, dn, privKey, pubKey, startDate, endDate, isCA, 0);
    }

    public static X509Certificate genCert(long validity, String sigAlg, String dn, PrivateKey privKey, PublicKey pubKey, Date startDate, Date endDate, boolean isCA, int keyUsage)
            throws CertificateEncodingException, InvalidKeyException,
                   IllegalStateException, NoSuchAlgorithmException,
                   SignatureException, IOException, NoSuchProviderException,
                   OperatorCreationException, 
                   CertificateException {
        return genCert(validity, sigAlg, dn, privKey, pubKey, startDate, endDate, isCA, keyUsage, null);
    }

    public static X509Certificate genCert(long validity, String sigAlg, String dn, PrivateKey privKey, PublicKey pubKey, Date startDate, Date endDate, boolean isCA, int keyUsage, CRLDistPoint crlDistPoint)
            throws CertificateEncodingException, InvalidKeyException,
                   IllegalStateException, NoSuchAlgorithmException,
                   SignatureException, IOException, NoSuchProviderException,
                   OperatorCreationException, 
                   CertificateException {
        final List<Extension> extensions = new LinkedList<Extension>();

        // CRL Distribution point
        if (crlDistPoint != null) {
            extensions.add(new Extension(Extension.cRLDistributionPoints, false, crlDistPoint.getEncoded()));
        }

        //return cert;
        return CertTools.genSelfCertForPurpose(dn, validity, null, privKey,
                pubKey, sigAlg, isCA, keyUsage, endDate, startDate, "BC", true, extensions);
    }

    public static String genPEMStringFromChain(List<X509Certificate> chain) throws CertificateEncodingException {
        String beginKey = "\n-----BEGIN CERTIFICATE-----\n";
        String endKey = "\n-----END CERTIFICATE-----\n";
        String retval = "";
        for (X509Certificate cert : chain) {
            retval += beginKey;
            retval += new String(Base64.encode(cert.getEncoded(), true));
            retval += endKey;
        }

        return retval;
    }

    public static X509CRL genCRL(X509Certificate cacert, PrivateKey privKey, DistributionPoint dp, Collection<RevokedCertInfo> certs, int crlPeriod, int crlnumber) throws CATokenOfflineException, IllegalKeyStoreException, IOException, SignatureException, NoSuchProviderException, InvalidKeyException, CRLException, NoSuchAlgorithmException {
        final String sigAlg = "SHA1WithRSA";

        boolean crlDistributionPointOnCrlCritical = true;
        boolean crlNumberCritical = false;

        Date thisUpdate = new Date();
        Date nextUpdate = new Date();

        // crlperiod is hours = crlperiod*60*60*1000 milliseconds
        nextUpdate.setTime(nextUpdate.getTime() + (crlPeriod * (long) (60 * 60 * 1000)));
        X509V2CRLGenerator crlgen = new X509V2CRLGenerator();
        crlgen.setThisUpdate(thisUpdate);
        crlgen.setNextUpdate(nextUpdate);
        crlgen.setSignatureAlgorithm(sigAlg);

        CRLNumber crlnum = new CRLNumber(BigInteger.valueOf(crlnumber));
        crlgen.addExtension(X509Extensions.CRLNumber.getId(), crlNumberCritical, crlnum);

        // Make DNs
        crlgen.setIssuerDN(cacert.getSubjectX500Principal());

        if (certs != null) {
            Iterator<RevokedCertInfo> it = certs.iterator();
            while (it.hasNext()) {
                RevokedCertInfo certinfo = it.next();
                crlgen.addCRLEntry(certinfo.getUserCertificate(), certinfo.getRevocationDate(), certinfo.getReason());
            }
        }

        // CRL Distribution point URI  	    
        IssuingDistributionPoint idp = new IssuingDistributionPoint(dp.getDistributionPoint(), false, false, null, false, false);

        // According to the RFC, IDP must be a critical extension.
        // Nonetheless, at the moment, Mozilla is not able to correctly
        // handle the IDP extension and discards the CRL if it is critical.
        crlgen.addExtension(X509Extensions.IssuingDistributionPoint.getId(), crlDistributionPointOnCrlCritical, idp);

        X509CRL crl;
        crl = crlgen.generate(privKey, "BC");
        // Verify before sending back
        crl.verify(cacert.getPublicKey());

        return crl;
    }

    public static CRLDistPoint generateDistPointWithUrl(URL cdpUrl) {
        GeneralName gn = new GeneralName(GeneralName.uniformResourceIdentifier, new DERIA5String(cdpUrl.toExternalForm()));
        GeneralNames gns = new GeneralNames(gn);
        DistributionPointName dpn = new DistributionPointName(0, gns);
        return new CRLDistPoint(new DistributionPoint[]{new DistributionPoint(dpn, null, null)});
    }

    public static CRLDistPoint generateDistPointWithIssuer(String issuer) {
        GeneralName gn = new GeneralName(new X509Name(issuer));
        GeneralNames gns = new GeneralNames(gn);
        DistributionPointName dpn = new DistributionPointName(0, gns);
        return new CRLDistPoint(new DistributionPoint[]{new DistributionPoint(dpn, null, null)});
    }
}
