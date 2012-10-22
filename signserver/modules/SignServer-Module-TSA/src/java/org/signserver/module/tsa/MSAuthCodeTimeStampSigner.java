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
package org.signserver.module.tsa;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;

import javax.persistence.EntityManager;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DERUTCTime;
import org.bouncycastle.asn1.cmp.PKIStatus;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.cms.Time;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.Attribute;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessable;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSSignedGenerator;
import org.bouncycastle.cms.DefaultSignedAttributeTableGenerator;
import org.bouncycastle.cms.SignerInfoGenerator;
import org.bouncycastle.cms.SignerInfoGeneratorBuilder;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcDigestCalculatorProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.tsp.*;
import org.ejbca.util.Base64;
import org.signserver.common.*;
import org.signserver.server.ITimeSource;
import org.signserver.server.WorkerContext;
import org.signserver.server.archive.Archivable;
import org.signserver.server.archive.DefaultArchivable;
import org.signserver.server.cryptotokens.ICryptoToken;
import org.signserver.server.log.IWorkerLogger;
import org.signserver.server.signers.BaseSigner;

/**
 * A Signer signing Time-stamp request compatible with Microsoft Authenticode
 *
 * Implements a ISigner and have the following properties:
 *
 * <table border="1">
 *  <tr>
 *      <td>TIMESOURCE</td>
 *      <td>
 *          property containing the classpath to the ITimeSource implementation
 *          that should be used. (default LocalComputerTimeSource)
 *      </td>
 *  </tr>
 *  <tr>
 *      <td>ACCEPTEDALGORITHMS</td>
 *      <td>
 *          A ';' separated string containing accepted algorithms, can be null
 *          if it shouldn't be used. (OPTIONAL)
 *      </td>
 *  </tr>
 *  <tr>
 *      <td>ACCEPTEDPOLICIES</td>
 *      <td>
 *          A ';' separated string containing accepted policies, can be null if
 *          it shouldn't be used. (OPTIONAL)
 *      </td>
 * </tr>
 *  <tr>
 *      <td>ACCEPTEDEXTENSIONS</td>
 *      <td>
 *          A ';' separated string containing accepted extensions, can be null
 *          if it shouldn't be used. (OPTIONAL)
 *      </td>
 * </tr>
 *  <tr>
 *      <td>DIGESTOID</td>
 *      <td>
 *          The Digenst OID to be used in the timestamp
 *      </td>
 * </tr>
 *  <tr>
 *      <td>DEFAULTTSAPOLICYOID</td>
 *      <td>
 *          The default policy ID of the time stamp authority
 *      </td>
 * </tr>
 *  <tr>
 *      <td>ACCURACYMICROS</td>
 *      <td>
 *          Accuraty in micro seconds, Only decimal number format, only one of
 *          the accuracy properties should be set (OPTIONAL)
 *      </td>
 * </tr>
 *  <tr>
 *      <td>ACCURACYMILLIS</td>
 *      <td>
 *          Accuraty in milli seconds, Only decimal number format, only one of
 *          the accuracy properties should be set (OPTIONAL)
 *      </td>
 * </tr>
 *  <tr>
 *      <td>ACCURACYSECONDS</td>
 *      <td>
 *          Accuraty in seconds. Only decimal number format, only one of the
 *          accuracy properties should be set (OPTIONAL)
 *      </td>
 * </tr>
 *  <tr>
 *      <td>ORDERING</td>
 *      <td>
 *          The ordering (OPTIONAL), default false.
 *      </td>
 * </tr>
 *  <tr>
 *      <td>TSA</td>
 *      <td>
 *          General name of the Time Stamp Authority.
 *      </td>
 *  </tr>
 * <tr>
 *      <td>REQUIREVALIDCHAIN</td>
 *      <td>
 *          Set to true to perform an extra check that the SIGNERCERTCHAIN only 
 *          contains certificates in the chain of the signer certificate.
 *          (OPTIONAL), default false.
 *      </td>
 * </tr>
 *
 * </table>
 * 
 * Specifying a signer certificate (normally the SIGNERCERT property) is required 
 * as information from that certificate will be used to indicate which signer
 * signed the time-stamp token.
 * 
 * The SIGNERCERTCHAIN property contains all certificates included in the token 
 * if the client requests the certificates. The RFC specified that the signer 
 * certificate MUST be included in the list returned.
 * 
 *
 * @author Marcus Lundblad
 * @version $Id$
 */
public class MSAuthCodeTimeStampSigner extends BaseSigner {

    /** Log4j instance for actual implementation class. */
    private static final Logger LOG = Logger.getLogger(MSAuthCodeTimeStampSigner.class);

    /** Random generator algorithm. */
    private static String algorithm = "SHA1PRNG";

    /** Random generator. */
    private transient SecureRandom random;

    private static final BigInteger LOWEST =
            new BigInteger("0080000000000000", 16);

    private static final BigInteger HIGHEST =
            new BigInteger("7FFFFFFFFFFFFFFF", 16);

    //Private Property constants
    public static final String TIMESOURCE = "TIMESOURCE";
    public static final String ACCEPTEDALGORITHMS = "ACCEPTEDALGORITHMS";
    public static final String ACCEPTEDPOLICIES = "ACCEPTEDPOLICIES";
    public static final String ACCEPTEDEXTENSIONS = "ACCEPTEDEXTENSIONS";
    //public static final String DEFAULTDIGESTOID    = "DEFAULTDIGESTOID";
    public static final String DEFAULTTSAPOLICYOID = "DEFAULTTSAPOLICYOID";
    public static final String ACCURACYMICROS = "ACCURACYMICROS";
    public static final String ACCURACYMILLIS = "ACCURACYMILLIS";
    public static final String ACCURACYSECONDS = "ACCURACYSECONDS";
    public static final String ORDERING = "ORDERING";
    public static final String TSA = "TSA";
    public static final String REQUIREVALIDCHAIN = "REQUIREVALIDCHAIN";
    
    private static final String dataOID = "1.2.840.113549.1.7.1";
    private static final String msOID = "1.3.6.1.4.1.311.3.2.1";

    private static final String DEFAULT_WORKERLOGGER =
            DefaultTimeStampLogger.class.getName();

    private static final String DEFAULT_TIMESOURCE =
            "org.signserver.server.LocalComputerTimeSource";
    
    /** MIME type for the response data. **/
    private static final String RESPONSE_CONTENT_TYPE = "application/octet-stream";

    
    private static final String[] ACCEPTEDALGORITHMSNAMES = {
        "GOST3411",
        "MD5",
        "SHA1",
        "SHA224",
        "SHA256",
        "SHA384",
        "SHA512",
        "RIPEMD128",
        "RIPEMD160",
        "RIPEMD256"
    };
    
    private static final ASN1ObjectIdentifier[] ACCEPTEDALGORITHMSOIDS = {
        TSPAlgorithms.GOST3411,
        TSPAlgorithms.MD5,
        TSPAlgorithms.SHA1,
        TSPAlgorithms.SHA224,
        TSPAlgorithms.SHA256,
        TSPAlgorithms.SHA384,
        TSPAlgorithms.SHA512,
        TSPAlgorithms.RIPEMD128,
        TSPAlgorithms.RIPEMD160,
        TSPAlgorithms.RIPEMD256
    };

    private static final HashMap<String, ASN1ObjectIdentifier> ACCEPTEDALGORITHMSMAP =
            new HashMap<String, ASN1ObjectIdentifier>();
    private static final HashMap<ASN1ObjectIdentifier, String> ACCEPTEDALGORITHMSREVERSEMAP =
    		new HashMap<ASN1ObjectIdentifier, String>();

    static {
        for (int i = 0; i < ACCEPTEDALGORITHMSNAMES.length; i++) {
            ACCEPTEDALGORITHMSMAP.put(ACCEPTEDALGORITHMSNAMES[i],
                    ACCEPTEDALGORITHMSOIDS[i]);
            ACCEPTEDALGORITHMSREVERSEMAP.put(ACCEPTEDALGORITHMSOIDS[i],
            		ACCEPTEDALGORITHMSNAMES[i]);
        }
    }

    private static final String DEFAULT_ORDERING = "FALSE";
    
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

    private ITimeSource timeSource = null;
    private Set<ASN1ObjectIdentifier> acceptedAlgorithms = null;
    private Set<String> acceptedPolicies = null;
    private Set<String> acceptedExtensions = null;

    //private String defaultDigestOID = null;
    private ASN1ObjectIdentifier defaultTSAPolicyOID = null;
    
    private boolean validChain = true;
    
    @Override
    public void init(final int signerId, final WorkerConfig config,
            final WorkerContext workerContext,
            final EntityManager workerEntityManager) {
        super.init(signerId, config, workerContext, workerEntityManager);

        // Overrides the default worker logger to be this worker
        //  implementation's default instead of the WorkerSessionBean's
        if (config.getProperty("WORKERLOGGER") == null) {
            config.setProperty("WORKERLOGGER", DEFAULT_WORKERLOGGER);
        }

        // Check that the timestamp server is properly configured
        try {
            timeSource = getTimeSource();
            if (timeSource == null) {
                final String error = "Error: Timestamp signer :" + signerId +
                    " has a malconfigured timesource.";
                LOG.error(error);
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("TimeStampSigner[" + signerId + "]: "
                            + "Using TimeSource: "
                            + timeSource.getClass().getName());
                }
            }
        } catch (SignServerException e) {
            LOG.error("Could not create time source: " + e.getMessage());
        }
            
        final String policyId = config.getProperties().getProperty(DEFAULTTSAPOLICYOID);
        
        try {
        	if (policyId != null) {
        		defaultTSAPolicyOID = new ASN1ObjectIdentifier(policyId);
        	} else {
        		LOG.error("Error: No default TSA Policy OID have been configured");
        	}
        } catch (IllegalArgumentException iae) {
        	LOG.error("Error: TSA Policy OID " + policyId + " is invalid");
        }
       
        if (LOG.isDebugEnabled()) {
            LOG.debug("bctsp version: " + TimeStampResponseGenerator.class
                .getPackage().getImplementationVersion() + ", "
                + TimeStampRequest.class.getPackage()
                    .getImplementationVersion());
        }
        
        // Validate certificates in signer certificate chain
        final String requireValidChain = config.getProperty(REQUIREVALIDCHAIN, Boolean.FALSE.toString());
        if (Boolean.parseBoolean(requireValidChain)) {
            validChain = validateChain();
        }
    }

    /**
     * The main method performing the actual timestamp operation.
     * Expects the signRequest to be a GenericSignRequest contining a
     * TimeStampRequest
     *
     * @param signRequest
     * @param requestContext
     * @return the sign response
     * @see org.signserver.server.IProcessable#processData(org.signserver.common.ProcessRequest, org.signserver.common.RequestContext)
     */
    public ProcessResponse processData(final ProcessRequest signRequest,
            final RequestContext requestContext) throws
                IllegalRequestException,
                CryptoTokenOfflineException,
                SignServerException {

    	// Log values
		final Map<String, String> logMap =
            (Map<String, String>) requestContext.get(RequestContext.LOGMAP);

    	
    	try {
            final ISignRequest sReq = (ISignRequest) signRequest;

            // Check that the request contains a valid TimeStampRequest object.
            if (!(signRequest instanceof GenericSignRequest)) {
                    final IllegalRequestException exception =
                                    new IllegalRequestException(
                                                    "Recieved request wasn't an expected GenericSignRequest. ");
                    LOG.error("Received request wasn't an expected GenericSignRequest");
                    throw exception;
            }

            if (!((sReq.getRequestData() instanceof TimeStampRequest)
            || (sReq.getRequestData() instanceof byte[]))) {
                    final IllegalRequestException exception =
                                    new IllegalRequestException(
                                                    "Recieved request data wasn't an expected TimeStampRequest. ");
                    LOG.error("Received request data wasn't an expected TimeStampRequest");
                    throw exception;
            }

            if (!validChain) {
                    LOG.error("Certificate chain not correctly configured");
                    throw new CryptoTokenOfflineException("Certificate chain not correctly configured");
            }

            byte[] buf = (byte[]) sReq.getRequestData();
            ASN1Primitive asn1obj = ASN1Primitive.fromByteArray(Base64.decode(buf));
            ASN1Sequence asn1seq = ASN1Sequence.getInstance(asn1obj);

            if (asn1seq.size() != 2) {
                    LOG.error("Wrong structure, should be an ASN1Sequence with 2 elements");
                    throw new IllegalRequestException("Wrong structure, should be an ASN1Sequence with 2 elements");
            }

            ASN1ObjectIdentifier oid = ASN1ObjectIdentifier.getInstance(asn1seq.getObjectAt(0));
            ASN1Sequence asn1seq1 = ASN1Sequence.getInstance(asn1seq.getObjectAt(1));

            if (!oid.getId().equals(msOID)) {
                    LOG.error("Invalid OID in request: " + oid.getId());
                    throw new IllegalRequestException("Invalid OID in request: " + oid.getId());
            }

            if (asn1seq1.size() != 2) {
                    LOG.error("Wrong structure, should be an ASN1Sequence with 2 elements as the value of element 0 in the outer ASN1Sequence");
                    throw new IllegalRequestException("Wrong structure, should be an ASN1Sequence with 2 elements as the value of element 0 in the outer ASN1Sequence");
            } 

            oid = ASN1ObjectIdentifier.getInstance(asn1seq1.getObjectAt(0));

            if (!oid.getId().equals(dataOID)) {
                    throw new IllegalRequestException("Wrong contentType OID: " + oid.getId());
            }

            ASN1TaggedObject tag = ASN1TaggedObject.getInstance(asn1seq1.getObjectAt(1));

            if (tag.getTagNo() != 0) {
                    throw new IllegalRequestException("Wrong tag no (should be 0): " + tag.getTagNo());
            } 

            ASN1OctetString octets = ASN1OctetString.getInstance(tag.getObject());
            byte[] content = octets.getOctets();

            if (LOG.isDebugEnabled()) {
                    LOG.debug("Number of octets in tag: " + content.length);
            }

            //response.setContentType("application/octet-stream");

            // get signing cert certificate chain and private key
            Collection<Certificate> certList = this.getSigningCertificateChain();
            if (certList == null) {
                throw new SignServerException(
                        "Null certificate chain. This signer needs a certificate.");
            }

            Certificate[] certs = (Certificate[]) certList.toArray(new Certificate[0]);
            PrivateKey pk = this.getCryptoToken().getPrivateKey(
                    ICryptoToken.PURPOSE_SIGN);

            // Sign
            CMSSignedDataGenerator cmssdg = new CMSSignedDataGenerator();
            X509Certificate x509cert = (X509Certificate) certs[0]; 
            List<X509Certificate> certL = new ArrayList<X509Certificate>();

            for (final Certificate cert : certs) {
                    certL.add((X509Certificate) cert);
            }
            
            final Date date = getTimeSource().getGenTime();
            
            if (date == null) {
                throw new ServiceUnavailableException("Time source is not available");
            }
            
            ASN1EncodableVector signedAttributes = new ASN1EncodableVector();
            signedAttributes.add(new Attribute(CMSAttributes.signingTime, new DERSet(new Time(date))));

            AttributeTable signedAttributesTable = new AttributeTable(signedAttributes);
            signedAttributesTable.toASN1EncodableVector();
            DefaultSignedAttributeTableGenerator signedAttributeGenerator = new DefaultSignedAttributeTableGenerator(signedAttributesTable);

            
            SignerInfoGeneratorBuilder signerInfoBuilder =
                    new SignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().setProvider("BC").build());
            signerInfoBuilder.setSignedAttributeGenerator(signedAttributeGenerator);

            JcaContentSignerBuilder contentSigner = new JcaContentSignerBuilder("SHA1withRSA");
            contentSigner.setProvider("BC");

            CertStore cs = CertStore.getInstance("Collection",new CollectionCertStoreParameters(certList), "BC");
            cmssdg.addSignerInfoGenerator(signerInfoBuilder.build(contentSigner.build(pk),
                    new X509CertificateHolder(x509cert.getEncoded())));

            //cmssdg.addSigner(pk, x509cert, CMSSignedGenerator.DIGEST_SHA1); 
            cmssdg.addCertificatesAndCRLs(cs);
            
            CMSProcessable cmspba = new CMSProcessableByteArray(content);
            CMSSignedData cmssd = cmssdg.generate(dataOID, cmspba, true, "BC");

            byte[] der = ASN1Primitive.fromByteArray(cmssd.getEncoded()).getEncoded(); 

  
            // Log values
            logMap.put(ITimeStampLogger.LOG_TSA_TIME, date == null ? null
                : String.valueOf(date.getTime()));
            
            final String archiveId = createArchiveId(buf, (String) requestContext.get(RequestContext.TRANSACTION_ID));

            GenericSignResponse signResponse = null;
            byte[] signedbytes = Base64.encode(der);
            
            logMap.put(ITimeStampLogger.LOG_TSA_TIMESTAMPRESPONSE_ENCODED,
                    new String(signedbytes));
        	
            
            final Collection<? extends Archivable> archivables = Arrays.asList(new DefaultArchivable(Archivable.TYPE_RESPONSE, RESPONSE_CONTENT_TYPE, signedbytes, archiveId));

            
            if (signRequest instanceof GenericServletRequest) {
                signResponse = new GenericServletResponse(sReq.getRequestID(),
                        		signedbytes,
                                    getSigningCertificate(),
                                    archiveId,
                                    archivables,
                                    RESPONSE_CONTENT_TYPE);
            } else {
                signResponse = new GenericSignResponse(sReq.getRequestID(),
                        signedbytes,
                        getSigningCertificate(),
                        archiveId,
                        archivables);
            }
        
        	return signResponse;

  
        } catch (InvalidAlgorithmParameterException e) {
            final IllegalRequestException exception =
                    new IllegalRequestException(
                    "InvalidAlgorithmParameterException: " + e.getMessage(), e);
            LOG.error("InvalidAlgorithmParameterException: ", e);
            logMap.put(ITimeStampLogger.LOG_TSA_EXCEPTION,
                    exception.getMessage());
            throw exception;
        } catch (NoSuchAlgorithmException e) {
            final IllegalRequestException exception =
                    new IllegalRequestException(
                        "NoSuchAlgorithmException: " + e.getMessage(), e);
            LOG.error("NoSuchAlgorithmException: ", e);
            logMap.put(ITimeStampLogger.LOG_TSA_EXCEPTION,
                    exception.getMessage());
            throw exception;
        } catch (NoSuchProviderException e) {
            final IllegalRequestException exception =
                    new IllegalRequestException(
                    "NoSuchProviderException: " + e.getMessage(), e);
            LOG.error("NoSuchProviderException: ", e);
            logMap.put(ITimeStampLogger.LOG_TSA_EXCEPTION,
                    exception.getMessage());
            throw exception;
        } catch (CertStoreException e) {
            final IllegalRequestException exception =
                    new IllegalRequestException("CertStoreException: "
                    + e.getMessage(), e);
            LOG.error("CertStoreException: ", e);
            logMap.put(ITimeStampLogger.LOG_TSA_EXCEPTION,
                    exception.getMessage());
            throw exception;
        } catch (IOException e) {
            final IllegalRequestException exception =
                    new IllegalRequestException(
                    "IOException: " + e.getMessage(), e);
            LOG.error("IOException: ", e);
            logMap.put(ITimeStampLogger.LOG_TSA_EXCEPTION,
                    exception.getMessage());
            throw exception;
        } catch (CMSException e) {
        	final SignServerException exception =
        			new SignServerException(e.getMessage(), e);
        	LOG.error("CMSException: ", e);
        	logMap.put(ITimeStampLogger.LOG_TSA_EXCEPTION,
        			exception.getMessage());
        	throw exception;
        } catch (OperatorCreationException e) {
            final SignServerException exception =
                new SignServerException(e.getMessage(), e);
            LOG.error("OperatorCreationException: ", e);
            logMap.put(ITimeStampLogger.LOG_TSA_EXCEPTION,
        	exception.getMessage());
            throw exception;
        } catch (CertificateEncodingException e) {
            final SignServerException exception =
                new SignServerException(e.getMessage(), e);
            LOG.error("CertificateEncodingException: ", e);
            logMap.put(ITimeStampLogger.LOG_TSA_EXCEPTION,
        	exception.getMessage());
            throw exception;
        }
    }

    /**
     * @return a time source interface expected to provide accurate time
     */
    private ITimeSource getTimeSource() throws SignServerException {
        if (timeSource == null) {
            try {
                String classpath =
                        this.config.getProperties().getProperty(TIMESOURCE);
                if (classpath == null) {
                    classpath = DEFAULT_TIMESOURCE;
                }

                final Class<?> implClass = Class.forName(classpath);
                final Object obj = implClass.newInstance();
                timeSource = (ITimeSource) obj;
                timeSource.init(config.getProperties());

            } catch (ClassNotFoundException e) {
                throw new SignServerException("Class not found", e);
            } catch (IllegalAccessException iae) {
                throw new SignServerException("Illegal access", iae);
            } catch (InstantiationException ie) {
                throw new SignServerException("Instantiation error", ie);
            }
        }

        return timeSource;
    }

    @SuppressWarnings("unchecked")
    private Set<ASN1ObjectIdentifier> getAcceptedAlgorithms() {
        if (acceptedAlgorithms == null) {
            final String nonParsedAcceptedAlgorihms =
                    this.config.getProperties().getProperty(ACCEPTEDALGORITHMS);
            if (nonParsedAcceptedAlgorihms == null) {
                acceptedAlgorithms = TSPAlgorithms.ALLOWED;
            } else {
                final String[] subStrings =
                        nonParsedAcceptedAlgorihms.split(";");
                if (subStrings.length > 0) {
                    acceptedAlgorithms = new HashSet();
                    for (int i = 0; i < subStrings.length; i++) {
                        final ASN1ObjectIdentifier acceptAlg = ACCEPTEDALGORITHMSMAP.get(subStrings[i]);
                        if (acceptAlg != null) {
                            acceptedAlgorithms.add(acceptAlg);
                        } else {
                            LOG.error("Error, signer " + workerId
                                    + " configured with incompatible acceptable algorithm : "
                                    + subStrings[i]);
                        }
                    }
                }
            }
        }

        return acceptedAlgorithms;
    }

    private Set<String> getAcceptedPolicies() {
        if (acceptedPolicies == null) {
            final String nonParsedAcceptedPolicies =
                    this.config.getProperties().getProperty(ACCEPTEDPOLICIES);
            acceptedPolicies = makeSetOfProperty(nonParsedAcceptedPolicies);
        }

        return acceptedPolicies;

    }

    private Set<String> getAcceptedExtensions() {
        if (acceptedExtensions == null) {
            final String nonParsedAcceptedExtensions =
                    this.config.getProperties().getProperty(ACCEPTEDEXTENSIONS);
            acceptedExtensions = makeSetOfProperty(nonParsedAcceptedExtensions);
        }

        return acceptedExtensions;
    }

    /**
     * Help method taking a string and creating a java.util.Set of the
     * strings using ';' as a delimiter.
     * If null is used as and argument then will null be returned by the method.
     * @param nonParsedPropery Semicolon separated strings
     * @return Set of Strings
     */
    private Set<String> makeSetOfProperty(final String nonParsedPropery) {
        Set<String> retval = null;
        if (nonParsedPropery != null) {
            final String[] subStrings = nonParsedPropery.split(";");
            if (subStrings.length > 0) {
                retval = new HashSet<String>();
                retval.addAll(Arrays.asList(subStrings));
            }
        }
        return retval;
    }

    private TimeStampTokenGenerator getTimeStampTokenGenerator(
            final TimeStampRequest timeStampRequest,
            final Map<String, String> logMap) throws
                IllegalRequestException,
                CryptoTokenOfflineException,
                InvalidAlgorithmParameterException,
                NoSuchAlgorithmException,
                NoSuchProviderException,
                CertStoreException,
                OperatorCreationException,
                SignServerException {

        TimeStampTokenGenerator timeStampTokenGen = null;
        try {
            final ASN1ObjectIdentifier digestOID = timeStampRequest.getMessageImprintAlgOID();
            
            /*if (digestOID == null) {
                digestOID = defaultDigestOID;
            }*/

            ASN1ObjectIdentifier tSAPolicyOID = timeStampRequest.getReqPolicy();
            if (tSAPolicyOID == null) {
                tSAPolicyOID = defaultTSAPolicyOID;
            }
            logMap.put(ITimeStampLogger.LOG_TSA_POLICYID, tSAPolicyOID.getId());

            final X509Certificate signingCert
                    = (X509Certificate) getSigningCertificate();
            if (signingCert == null) {
                throw new CryptoTokenOfflineException(
                        "No certificate for this signer");
            }
            
            DigestCalculatorProvider calcProv = new BcDigestCalculatorProvider();    
            DigestCalculator calc = calcProv.get(new AlgorithmIdentifier(TSPAlgorithms.SHA1));
            
            
            Certificate cert = this.getSigningCertificate();
            
            PrivateKey privKey = this.getCryptoToken().getPrivateKey(ICryptoToken.PURPOSE_SIGN);
            ContentSigner cs =
            		new JcaContentSignerBuilder("SHA1WITHRSA").setProvider("BC").build(privKey);
            JcaSignerInfoGeneratorBuilder sigb = new JcaSignerInfoGeneratorBuilder(calcProv);
            X509CertificateHolder certHolder = new X509CertificateHolder(cert.getEncoded());
            SignerInfoGenerator sig = sigb.build(cs, certHolder);
            
            timeStampTokenGen = new TimeStampTokenGenerator(calc, sig, tSAPolicyOID);

            if (config.getProperties().getProperty(ACCURACYMICROS) != null) {
                timeStampTokenGen.setAccuracyMicros(Integer.parseInt(
                        config.getProperties().getProperty(ACCURACYMICROS)));
            }

            if (config.getProperties().getProperty(ACCURACYMILLIS) != null) {
                timeStampTokenGen.setAccuracyMillis(Integer.parseInt(
                        config.getProperties().getProperty(ACCURACYMILLIS)));
            }

            if (config.getProperties().getProperty(ACCURACYSECONDS) != null) {
                timeStampTokenGen.setAccuracySeconds(Integer.parseInt(
                        config.getProperties().getProperty(ACCURACYSECONDS)));
            }

            if (config.getProperties().getProperty(ORDERING) != null) {
                timeStampTokenGen.setOrdering(
                        config.getProperties().getProperty(ORDERING,
                            DEFAULT_ORDERING).equalsIgnoreCase("TRUE"));
            }

            if (config.getProperties().getProperty(TSA) != null) {
                final X500Name x500Name = new X500Name(config.getProperties()
                            .getProperty(TSA));
                timeStampTokenGen.setTSA(new GeneralName(x500Name));
            }
           
            // TODO: will probably need to fix this when moving to BC 2.0...
            timeStampTokenGen.setCertificatesAndCRLs(getCertStoreWithChain(signingCert));

        } catch (IllegalArgumentException e) {
            LOG.error("IllegalArgumentException: ", e);
            throw new IllegalRequestException(e.getMessage());
        } catch (TSPException e) {
            LOG.error("TSPException: ", e);
            throw new IllegalRequestException(e.getMessage());
        } catch (CertificateEncodingException e) {
        	LOG.error("CertificateEncodingException: ", e);
        	throw new IllegalRequestException(e.getMessage());
        } catch (IOException e) {
        	LOG.error("IOException: ", e);
        	throw new IllegalRequestException(e.getMessage());
        }

        return timeStampTokenGen;
    }
    
    private CertStore getCertStoreWithChain(Certificate signingCert) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException, CryptoTokenOfflineException, CertStoreException {
        Collection<Certificate> signingCertificateChain = getSigningCertificateChain();
        
        if (signingCertificateChain == null) {
            throw new CryptoTokenOfflineException("Certificate chain not available");
        } else {
            final CertStore certStore = CertStore.getInstance("Collection",
                    new CollectionCertStoreParameters(
                        signingCertificateChain), "BC");

            if (!containsCertificate(certStore, signingCert)) {
                throw new CryptoTokenOfflineException("Signer certificate not included in certificate chain");
            }
            return certStore;
        }
    }
    
    /**
     * @return True if the CertStore contained the Certificate
     */
    private boolean containsCertificate(final CertStore store, final Certificate subject) throws CertStoreException {
        final Collection<? extends Certificate> matchedCerts = store.getCertificates(new CertSelector() {
            @Override
            public boolean match(Certificate cert) {
                return subject.equals(cert);
            }
            @Override
            public Object clone() {
                return this;
            }
        });
        return matchedCerts.size() > 0;
    }

    private TimeStampResponseGenerator getTimeStampResponseGenerator(
            TimeStampTokenGenerator timeStampTokenGen) {
        
        return new TimeStampResponseGenerator(timeStampTokenGen,
                this.getAcceptedAlgorithms(),
                this.getAcceptedPolicies(),
                this.getAcceptedExtensions());
    }

    /**
     * Help method that generates a serial number using SecureRandom
     */
    private BigInteger getSerialNumber() {
        BigInteger serialNumber = null;
        try {
            serialNumber = getSerno();
        } catch (Exception e) {
            LOG.error("Error initiating Serial Number generator, SEVERE ERROR.",
                    e);
        }
        return serialNumber;
    }

    /**
     * Generates a number of serial number bytes. The number returned should
     * be a positive number.
     *
     * @return a BigInteger with a new random serial number.
     */
    public BigInteger getSerno() {
        if (random == null) {
            try {
                random = SecureRandom.getInstance(algorithm);
            } catch (NoSuchAlgorithmException e) {
                LOG.error(e);
            }
        }

        final byte[] sernobytes = new byte[8];
        boolean ok = false;
        BigInteger serno = null;
        while (!ok) {
            random.nextBytes(sernobytes);
            serno = new BigInteger(sernobytes).abs();

            // Must be within the range 0080000000000000 - 7FFFFFFFFFFFFFFF
            if ((serno.compareTo(LOWEST) >= 0)
                    && (serno.compareTo(HIGHEST) <= 0)) {
                ok = true;
            }
        }
        return serno;
    }
    
    private static class SHA1DigestCalculator implements DigestCalculator {
    	private ByteArrayOutputStream bOut = new ByteArrayOutputStream();
    	private MessageDigest digest;

    	public SHA1DigestCalculator() {
    		try {
    			this.digest = MessageDigest.getInstance("SHA1");
    		} catch (NoSuchAlgorithmException e) {
    			
    		}
    	}

    	public AlgorithmIdentifier getAlgorithmIdentifier() {
    		return new AlgorithmIdentifier(OIWObjectIdentifiers.idSHA1);
    	}

    	public OutputStream getOutputStream() {
    		return bOut;
    	}

    	public byte[] getDigest() {
    		byte[] bytes = digest.digest(bOut.toByteArray());

    		bOut.reset();

    		return bytes;
    	}
    }

    /**
     * @return True if each certificate in the certificate chain can be verified 
     * by the next certificate (if any). This does not check that the last 
     * certificate is a trusted certificate as the root certificate is normally 
     * not included.
     */
    private boolean validateChain() {
        boolean result = true;
        try {
            Collection<Certificate> signingCertificateChain = getSigningCertificateChain();
            if (signingCertificateChain instanceof List) {
                List<Certificate> chain = (List<Certificate>) signingCertificateChain;
                for (int i = 0; i < chain.size(); i++) {
                    Certificate subject = chain.get(i);
                    
                    // If we have the issuer we can validate the certificate
                    if (chain.size() > i + 1) {
                        Certificate issuer = chain.get(i + 1);
                        try {
                            subject.verify(issuer.getPublicKey(), "BC");
                        } catch (CertificateException ex) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Certificate could not be verified: " + ex.getMessage() + ": " + subject);
                            }
                            result = false;
                        } catch (NoSuchAlgorithmException ex) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Certificate could not be verified: " + ex.getMessage() + ": " + subject);
                            }
                            result = false;
                        } catch (InvalidKeyException ex) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Certificate could not be verified: " + ex.getMessage() + ": " + subject);
                            }
                            result = false;
                        } catch (NoSuchProviderException ex) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Certificate could not be verified: " + ex.getMessage() + ": " + subject);
                            }
                            result = false;
                        } catch (SignatureException ex) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Certificate could not be verified: " + ex.getMessage() + ": " + subject);
                            }
                            result = false;
                        }
                    }
                }
            } else {
                // This would be a bug
                LOG.error("Certificate chain was not an list!");
                result = false;
            }
        } catch (CryptoTokenOfflineException ex) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Unable to get signer certificate or chain: " + ex.getMessage());
            }
            result = false;
        }
        return result;
    }

    @Override
    protected List<String> getFatalErrors() {
        final List<String> result = new LinkedList<String>();
        result.addAll(super.getFatalErrors());
        
        try {
            // TODO: This test might be moved so that it is available to all signers
            // Check that certificiate chain contains the signer certificate
            final Certificate certificate = getSigningCertificate();
            try {
                getCertStoreWithChain(certificate);
            } catch (NoSuchAlgorithmException ex) {
                result.add("Unable to get certificate chain");
                LOG.error("Signer " + workerId + ": Unable to get certificate chain: " + ex.getMessage());
            } catch (NoSuchProviderException ex) {
                result.add("Unable to get certificate chain");
                LOG.error("Signer " + workerId + ": Unable to get certificate chain: " + ex.getMessage());
            } catch (CertStoreException ex) {
                result.add("Unable to get certificate chain");
                LOG.error("Signer " + workerId + ": Unable to get certificate chain: " + ex.getMessage());
            } catch (InvalidAlgorithmParameterException ex) {
                result.add("Unable to get certificate chain");
                LOG.error("Signer " + workerId + ": Unable to get certificate chain: " + ex.getMessage());
            } catch (CryptoTokenOfflineException ex) {
                result.add(ex.getMessage());
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Signer " + workerId + ": Could not get signer certificate in chain: " + ex.getMessage());
                }
            }

            // Check signer certificate chain if required
            if (!validChain) {
                result.add("Not strictly valid chain and " + REQUIREVALIDCHAIN + " specified");
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Signer " + workerId + ": " + REQUIREVALIDCHAIN + " specified but the chain was not found valid");
                }
            }

            // Check if certificat has the required EKU
            try {
                if (certificate instanceof X509Certificate) {
                    final X509Certificate cert = (X509Certificate) certificate;
                    if (cert.getExtendedKeyUsage() == null 
                            || !cert.getExtendedKeyUsage().contains(KeyPurposeId.id_kp_timeStamping.getId())) {
                        result.add("Missing extended key usage timeStamping");
                    }
                    if (cert.getCriticalExtensionOIDs() == null 
                            || !cert.getCriticalExtensionOIDs().contains(org.bouncycastle.asn1.x509.X509Extension.extendedKeyUsage.getId())) {
                        result.add("The extended key usage extension must be present and marked as critical");
                    }
                } else {
                    result.add("Unsupported certificate type");
                }
            } catch (CertificateParsingException ex) {
                result.add("Unable to parse certificate");
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Signer " + workerId + ": Unable to parse certificate: " + ex.getMessage());
                }
            }
        } catch (CryptoTokenOfflineException ex) {
            result.add("No signer certificate available");
            if (LOG.isDebugEnabled()) {
                LOG.debug("Signer " + workerId + ": Could not get signer certificate: " + ex.getMessage());
            }
        } 
        
        // check time source
        if (timeSource.getGenTime() == null) {
        	result.add("Time source not available");
        	if (LOG.isDebugEnabled()) {
        		LOG.debug("Signer " + workerId + ": time source not available");
        	}
        }

        return result;
    }
    
}
