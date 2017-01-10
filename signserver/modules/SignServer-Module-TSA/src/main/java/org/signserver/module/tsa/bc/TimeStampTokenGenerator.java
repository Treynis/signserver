// DSS-639: copied from BouncyCastle 1.47, modified to include the IssuerSerial structure
// in the signingCertificate CMS signed attribute in the response.
// Also modified to allow always including the ordering field in TSTInfo.
// DSS-1310: Backporting optional inclusion of IssuerSerial from trunk.
package org.signserver.module.tsa.bc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.cert.CRLException;
import java.security.cert.CertStore;
import java.security.cert.CertStoreException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bouncycastle.asn1.ASN1Boolean;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1GeneralizedTime;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.ess.ESSCertID;
import org.bouncycastle.asn1.ess.SigningCertificate;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.tsp.Accuracy;
import org.bouncycastle.asn1.tsp.MessageImprint;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.IssuerSerial;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CRLHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cms.CMSAttributeTableGenerationException;
import org.bouncycastle.cms.CMSAttributeTableGenerator;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSSignedGenerator;
import org.bouncycastle.cms.DefaultSignedAttributeTableGenerator;
import org.bouncycastle.cms.SignerInfoGenerator;
import org.bouncycastle.cms.SimpleAttributeTableGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.interfaces.GOST3410PrivateKey;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.tsp.TSPException;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.util.CollectionStore;
import org.bouncycastle.util.Store;

public class TimeStampTokenGenerator
{
    int accuracySeconds = -1;

    int accuracyMillis = -1;

    int accuracyMicros = -1;

    boolean ordering = false;
    boolean includeOrdering = false;

    GeneralName tsa = null;
    
    private ASN1ObjectIdentifier  tsaPolicyOID;

    PrivateKey      key;
    X509Certificate cert;
    String          digestOID;
    AttributeTable  signedAttr;
    AttributeTable  unsignedAttr;

    private List certs = new ArrayList();
    private List crls = new ArrayList();
    private List attrCerts = new ArrayList();
    private SignerInfoGenerator signerInfoGen;

    /**
     * Basic Constructor - set up a calculator based on signerInfoGen with a ESSCertID calculated from
     * the signer's associated certificate using the sha1DigestCalculator.
     *
     * @param sha1DigestCalculator calculator for SHA-1 of certificate.
     * @param signerInfoGen the generator for the signer we are using.
     * @param tsaPolicy tasPolicy to send.
     * @throws IllegalArgumentException if calculator is not SHA-1 or there is no associated certificate for the signer,
     * @throws TSPException if the signer certificate cannot be processed.
     */
    public TimeStampTokenGenerator(
        DigestCalculator sha1DigestCalculator,
        final SignerInfoGenerator         signerInfoGen,
        ASN1ObjectIdentifier              tsaPolicy,
        boolean                           isIssuerSerialIncluded)
        throws IllegalArgumentException, TSPException
    {
        this.signerInfoGen = signerInfoGen;
        this.tsaPolicyOID = tsaPolicy;

        if (!sha1DigestCalculator.getAlgorithmIdentifier().getAlgorithm().equals(OIWObjectIdentifiers.idSHA1))
        {
            throw new IllegalArgumentException("Digest calculator must be for SHA-1");
        }

        if (!signerInfoGen.hasAssociatedCertificate())
        {
            throw new IllegalArgumentException("SignerInfoGenerator must have an associated certificate");
        }

        TSPUtil.validateCertificate(signerInfoGen.getAssociatedCertificate());

        try
        {
            OutputStream dOut = sha1DigestCalculator.getOutputStream();
            final X509CertificateHolder ch = signerInfoGen.getAssociatedCertificate();
            
            dOut.write(ch.getEncoded());

            dOut.close();

            final DERInteger serial = new DERInteger(ch.getSerialNumber());
            final X500Name issuer = ch.getIssuer();                   
            final GeneralName name = new GeneralName(issuer);
            final GeneralNames names = new GeneralNames(name);
            final IssuerSerial is = isIssuerSerialIncluded ? new IssuerSerial(names, ASN1Integer.getInstance(serial)) : null;
            
            final ESSCertID essCertid = new ESSCertID(sha1DigestCalculator.getDigest(), is);
            this.signerInfoGen = new SignerInfoGenerator(signerInfoGen, new CMSAttributeTableGenerator()
            {
                public AttributeTable getAttributes(Map parameters)
                    throws CMSAttributeTableGenerationException
                {
                    AttributeTable table = signerInfoGen.getSignedAttributeTableGenerator().getAttributes(parameters);

                    return table.add(PKCSObjectIdentifiers.id_aa_signingCertificate, new SigningCertificate(essCertid));
                }
            }, signerInfoGen.getUnsignedAttributeTableGenerator());

        }
        catch (IOException e)
        {
            throw new TSPException("Exception processing certificate.", e);
        }
    }

    /**
     * basic creation - only the default attributes will be included here.
     * @deprecated use SignerInfoGenerator constructor that takes a digest calculator
     */
    public TimeStampTokenGenerator(
        final SignerInfoGenerator     signerInfoGen,
        ASN1ObjectIdentifier          tsaPolicy)
        throws IllegalArgumentException, TSPException
    {
        this(new DigestCalculator()
        {
            private ByteArrayOutputStream bOut = new ByteArrayOutputStream();

            public AlgorithmIdentifier getAlgorithmIdentifier()
            {
                return new AlgorithmIdentifier(OIWObjectIdentifiers.idSHA1, DERNull.INSTANCE);
            }

            public OutputStream getOutputStream()
            {
                return bOut;
            }

            public byte[] getDigest()
            {
                try
                {
                    return MessageDigest.getInstance("SHA-1").digest(bOut.toByteArray());
                }
                catch (NoSuchAlgorithmException e)
                {
                    throw new IllegalStateException("cannot find sha-1: "+ e.getMessage());
                }
            }
        }, signerInfoGen, tsaPolicy, true);
    }

    /**
     * basic creation - only the default attributes will be included here.
     * @deprecated use SignerInfoGenerator constructor that takes a digest calculator.
     */
    public TimeStampTokenGenerator(
        PrivateKey      key,
        X509Certificate cert,
        String          digestOID,
        String          tsaPolicyOID)
        throws IllegalArgumentException, TSPException
    {
        this(key, cert, digestOID, tsaPolicyOID, null, null);
    }

    /**
     * basic creation - only the default attributes will be included here.
     * @deprecated use SignerInfoGenerator constructor that takes a digest calculator.
     */
    public TimeStampTokenGenerator(
        PrivateKey      key,
        X509Certificate cert,
        ASN1ObjectIdentifier          digestOID,
        String          tsaPolicyOID)
        throws IllegalArgumentException, TSPException
    {
        this(key, cert, digestOID.getId(), tsaPolicyOID, null, null);
    }

    /**
     * create with a signer with extra signed/unsigned attributes.
     * @deprecated use SignerInfoGenerator constructor that takes a digest calculator.
     */
    public TimeStampTokenGenerator(
        PrivateKey      key,
        X509Certificate cert,
        String          digestOID,
        String          tsaPolicyOID,
        AttributeTable  signedAttr,
        AttributeTable  unsignedAttr)
        throws IllegalArgumentException, TSPException
    {   
        this.key = key;
        this.cert = cert;
        this.digestOID = digestOID;
        this.tsaPolicyOID = new ASN1ObjectIdentifier(tsaPolicyOID);
        this.unsignedAttr = unsignedAttr;

        //
        // add the essCertid
        //
        final Hashtable signedAttrs;
        
        if (signedAttr != null)
        {
            signedAttrs = signedAttr.toHashtable();
        }
        else
        {
            signedAttrs = new Hashtable();
        }


        TSPUtil.validateCertificate(cert);

        try
        {
            ESSCertID essCertid = new ESSCertID(MessageDigest.getInstance("SHA-1").digest(cert.getEncoded()));
            signedAttrs.put(PKCSObjectIdentifiers.id_aa_signingCertificate,
                    new Attribute(
                            PKCSObjectIdentifiers.id_aa_signingCertificate,
                            new DERSet(new SigningCertificate(essCertid))));
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new TSPException("Can't find a SHA-1 implementation.", e);
        }
        catch (CertificateEncodingException e)
        {
            throw new TSPException("Exception processing certificate.", e);
        }
        
        this.signedAttr = new AttributeTable(signedAttrs);
    }

    /**
     * @deprecated use addCertificates and addCRLs
     * @param certificates
     * @throws CertStoreException
     * @throws TSPException
     */
    public void setCertificatesAndCRLs(CertStore certificates)
            throws CertStoreException, TSPException
    {
        Collection c1 = certificates.getCertificates(null);

        for (Iterator it = c1.iterator(); it.hasNext();)
        {
            try
            {
                certs.add(new JcaX509CertificateHolder((X509Certificate)it.next()));
            }
            catch (CertificateEncodingException e)
            {
                throw new TSPException("cannot encode certificate: " + e.getMessage(), e);
            }
        }

        c1 = certificates.getCRLs(null);

        for (Iterator it = c1.iterator(); it.hasNext();)
        {
            try
            {
                crls.add(new JcaX509CRLHolder((X509CRL)it.next()));
            }
            catch (CRLException e)
            {
                throw new TSPException("cannot encode CRL: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Add the store of X509 Certificates to the generator.
     *
     * @param certStore  a Store containing X509CertificateHolder objects
     */
    public void addCertificates(
        Store certStore)
    {
        certs.addAll(certStore.getMatches(null));
    }

    /**
     *
     * @param crlStore a Store containing X509CRLHolder objects.
     */
    public void addCRLs(
        Store crlStore)
    {
        crls.addAll(crlStore.getMatches(null));
    }

    /**
     *
     * @param attrStore a Store containing X509AttributeCertificate objects.
     */
    public void addAttributeCertificates(
        Store attrStore)
    {
        attrCerts.addAll(attrStore.getMatches(null));
    }

    public void setAccuracySeconds(int accuracySeconds)
    {
        this.accuracySeconds = accuracySeconds;
    }

    public void setAccuracyMillis(int accuracyMillis)
    {
        this.accuracyMillis = accuracyMillis;
    }

    public void setAccuracyMicros(int accuracyMicros)
    {
        this.accuracyMicros = accuracyMicros;
    }

    public void setOrdering(boolean ordering)
    {
        this.ordering = ordering;
    }
    
    public void setIncludeOrdering(boolean includeOrdering) {
        this.includeOrdering = includeOrdering;
    }

    public void setTSA(GeneralName tsa)
    {
        this.tsa = tsa;
    }
    
    //------------------------------------------------------------------------------

    public TimeStampToken generate(
        TimeStampRequest    request,
        BigInteger          serialNumber,
        Date                genTime,
        String              provider)
        throws NoSuchAlgorithmException, NoSuchProviderException, TSPException
    {
        ASN1Sequence empty = new DERSequence();
        return generate(request, serialNumber, genTime, provider,
                        Extensions.getInstance(empty));
    }
    
    public TimeStampToken generate(
        TimeStampRequest    request,
        BigInteger          serialNumber,
        Date                genTime,
        String              provider,
        Extensions          additionalExtensions)
        throws NoSuchAlgorithmException, NoSuchProviderException, TSPException
    {
        if (signerInfoGen == null)
        {
            try
            {
                JcaSignerInfoGeneratorBuilder sigBuilder = new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().setProvider(provider).build());

                sigBuilder.setSignedAttributeGenerator(new DefaultSignedAttributeTableGenerator(signedAttr));

                if (unsignedAttr != null)
                {
                    sigBuilder.setUnsignedAttributeGenerator(new SimpleAttributeTableGenerator(unsignedAttr));
                }

                signerInfoGen = sigBuilder.build(new JcaContentSignerBuilder(getSigAlgorithm(key, digestOID)).setProvider(provider).build(key), cert);
            }
            catch (OperatorCreationException e)
            {
                throw new TSPException("Error generating signing operator", e);
            }
            catch (CertificateEncodingException e)
            {
                throw new TSPException("Error encoding certificate", e);
            }
        }

        return generate(request, serialNumber, genTime, additionalExtensions);
    }

    public TimeStampToken generate(
        TimeStampRequest    request,
        BigInteger          serialNumber,
        Date                genTime)
    throws TSPException
    {
        ASN1Sequence empty = new DERSequence();
        return generate(request, serialNumber, genTime, Extensions.getInstance(empty));
    }
    
    public TimeStampToken generate(
        TimeStampRequest    request,
        BigInteger          serialNumber,
        Date                genTime,
        Extensions          additionalExtensions)
        throws TSPException
    {
        if (signerInfoGen == null)
        {
            throw new IllegalStateException("can only use this method with SignerInfoGenerator constructor");
        }

        ASN1ObjectIdentifier digestAlgOID = request.getMessageImprintAlgOID();

        AlgorithmIdentifier algID = new AlgorithmIdentifier(digestAlgOID, new DERNull());
        MessageImprint      messageImprint = new MessageImprint(algID, request.getMessageImprintDigest());

        Accuracy accuracy = null;
        if (accuracySeconds > 0 || accuracyMillis > 0 || accuracyMicros > 0)
        {
            ASN1Integer seconds = null;
            if (accuracySeconds > 0)
            {
                seconds = new ASN1Integer(accuracySeconds);
            }

            ASN1Integer millis = null;
            if (accuracyMillis > 0)
            {
                millis = new ASN1Integer(accuracyMillis);
            }

            ASN1Integer micros = null;
            if (accuracyMicros > 0)
            {
                micros = new ASN1Integer(accuracyMicros);
            }

            accuracy = new Accuracy(seconds, millis, micros);
        }

        ASN1Boolean derOrdering = null;
        if (ordering || includeOrdering)
        {
            derOrdering = new ASN1Boolean(ordering);
        }

        ASN1Integer  nonce = null;
        if (request.getNonce() != null)
        {
            nonce = new ASN1Integer(request.getNonce());
        }

        ASN1ObjectIdentifier tsaPolicy = tsaPolicyOID;
        if (request.getReqPolicy() != null)
        {
            tsaPolicy = request.getReqPolicy();
        }

        Extensions exts = request.getExtensions();

        if (additionalExtensions != null &&
            additionalExtensions.getExtensionOIDs().length > 0) {
            List<ASN1Encodable> allExtensions =
                    new LinkedList<ASN1Encodable>();
            
            if (exts != null) {
                ASN1Sequence seq = (ASN1Sequence) exts.toASN1Primitive();
                ASN1Encodable[] seqArr = seq.toArray();
                List<ASN1Encodable> seqList = Arrays.asList(seqArr);
                
                allExtensions.addAll(seqList);
            }
            
            ASN1Sequence additionalSeq =
                    (ASN1Sequence) additionalExtensions.toASN1Primitive();
                ASN1Encodable[] additionalSeqArr = additionalSeq.toArray();
                List<ASN1Encodable> additionalSeqList =
                        Arrays.asList(additionalSeqArr);
            allExtensions.addAll(additionalSeqList);
            
            ASN1Sequence allSeq = new DERSequence(allExtensions.toArray(new ASN1Encodable[0]));
            
            exts = Extensions.getInstance(allSeq);
        }

        TSTInfo tstInfo = new TSTInfo(tsaPolicy,
                messageImprint, new ASN1Integer(serialNumber),
                new ASN1GeneralizedTime(genTime), accuracy, derOrdering,
                nonce, tsa, exts);

        try
        {
            CMSSignedDataGenerator  signedDataGenerator = new CMSSignedDataGenerator();

            if (request.getCertReq())
            {
                // TODO: do we need to check certs non-empty?
                signedDataGenerator.addCertificates(new CollectionStore(certs));
                signedDataGenerator.addCRLs(new CollectionStore(crls));
                signedDataGenerator.addAttributeCertificates(new CollectionStore(attrCerts));
            }
            else
            {
                signedDataGenerator.addCRLs(new CollectionStore(crls));
            }

            signedDataGenerator.addSignerInfoGenerator(signerInfoGen);

            byte[] derEncodedTSTInfo = tstInfo.getEncoded(ASN1Encoding.DER);

            CMSSignedData signedData = signedDataGenerator.generate(new CMSProcessableByteArray(PKCSObjectIdentifiers.id_ct_TSTInfo, derEncodedTSTInfo), true);

            return new TimeStampToken(signedData);
        }
        catch (CMSException cmsEx)
        {
            throw new TSPException("Error generating time-stamp token", cmsEx);
        }
        catch (IOException e)
        {
            throw new TSPException("Exception encoding info", e);
        }
    }

    private String getSigAlgorithm(
        PrivateKey key,
        String     digestOID)
    {
        String enc = null;

        if (key instanceof RSAPrivateKey || "RSA".equalsIgnoreCase(key.getAlgorithm()))
        {
            enc = "RSA";
        }
        else if (key instanceof DSAPrivateKey || "DSA".equalsIgnoreCase(key.getAlgorithm()))
        {
            enc = "DSA";
        }
        else if ("ECDSA".equalsIgnoreCase(key.getAlgorithm()) || "EC".equalsIgnoreCase(key.getAlgorithm()))
        {
            enc = "ECDSA";
        }
        else if (key instanceof GOST3410PrivateKey || "GOST3410".equalsIgnoreCase(key.getAlgorithm()))
        {
            enc = "GOST3410";
        }
        else if ("ECGOST3410".equalsIgnoreCase(key.getAlgorithm()))
        {
            enc = CMSSignedGenerator.ENCRYPTION_ECGOST3410;
        }

        return TSPUtil.getDigestAlgName(digestOID) + "with" + enc;
    }
}

