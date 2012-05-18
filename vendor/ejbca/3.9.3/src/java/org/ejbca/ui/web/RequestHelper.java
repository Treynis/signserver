/*************************************************************************
 *                                                                       *
 *  EJBCA: The OpenSource Certificate Authority                          *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
 
package org.ejbca.ui.web;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.jce.netscape.NetscapeCertRequest;
import org.ejbca.core.ejb.ServiceLocator;
import org.ejbca.core.ejb.ServiceLocatorException;
import org.ejbca.core.ejb.ca.sign.ISignSessionLocal;
import org.ejbca.core.model.ca.SignRequestSignatureException;
import org.ejbca.core.model.log.Admin;
import org.ejbca.core.protocol.CVCRequestMessage;
import org.ejbca.core.protocol.IResponseMessage;
import org.ejbca.core.protocol.PKCS10RequestMessage;
import org.ejbca.core.protocol.X509ResponseMessage;
import org.ejbca.ui.web.pub.ServletDebug;
import org.ejbca.ui.web.pub.ServletUtils;
import org.ejbca.util.Base64;
import org.ejbca.util.CertTools;
import org.ejbca.util.RequestMessageUtils;

/**
 * Helper class for handling certificate request from browsers or general PKCS#10
 * 
 * @version $Id: RequestHelper.java 7736 2009-06-18 11:55:30Z anatom $
 */
public class RequestHelper {
    private static Logger log = Logger.getLogger(RequestHelper.class);
    private Admin administrator;
    private ServletDebug debug;
    private static final Pattern CLASSID = Pattern.compile("\\$CLASSID");

	public static final  String BEGIN_CERTIFICATE_REQUEST_WITH_NL = "-----BEGIN CERTIFICATE REQUEST-----\n";
	public static final  String END_CERTIFICATE_REQUEST_WITH_NL    = "\n-----END CERTIFICATE REQUEST-----\n";

	public static final  String BEGIN_CERTIFICATE_WITH_NL = "-----BEGIN CERTIFICATE-----\n";
	public static final  String END_CERTIFICATE_WITH_NL    = "\n-----END CERTIFICATE-----\n";
    public static final  String BEGIN_CRL_WITH_NL = "-----BEGIN X509 CRL-----\n";
    public static final  String END_CRL_WITH_NL    = "\n-----END X509 CRL-----\n";

	public static final  String BEGIN_PKCS7  = "-----BEGIN PKCS7-----\n";
	public static final  String END_PKCS7     = "\n-----END PKCS7-----\n";	
	public static final  String BEGIN_PKCS7_WITH_NL = "-----BEGIN PKCS7-----\n";
	public static final  String END_PKCS7_WITH_NL    = "\n-----END PKCS7-----\n";
	
	public static final int ENCODED_CERTIFICATE = 1;
	public static final int ENCODED_PKCS7          = 2;
	public static final int BINARY_CERTIFICATE = 3;
	
    /**
     * Creates a new RequestHelper object.
     *
     * @param administrator Admin doing the request
     * @param debug object to send debug to or null to disable
     */
    public RequestHelper(Admin administrator, ServletDebug debug) {
        this.administrator = administrator;
        this.debug = debug;
    }

    /**
     * Handles Firefox certificate request (KEYGEN), these are constructed as: <code>
     * SignedPublicKeyAndChallenge ::= SEQUENCE { publicKeyAndChallenge    PublicKeyAndChallenge,
     * signatureAlgorithm   AlgorithmIdentifier, signature        BIT STRING }</code> PublicKey's
     * encoded-format has to be RSA X.509.
     *
     * @param signsession EJB session to signature bean.
     * @param reqBytes buffer holding te request from NS.
     * @param username username in EJBCA for authoriation.
     * @param password users password for authorization.
     *
     * @return byte[] containing DER-encoded certificate.
     */
    public byte[] nsCertRequest(ISignSessionLocal signsession, byte[] reqBytes, String username,
        String password) throws Exception {
        byte[] buffer = Base64.decode(reqBytes);

        if (buffer == null) {
            return null;
        }

        ASN1InputStream in = new ASN1InputStream(new ByteArrayInputStream(buffer));
        ASN1Sequence spkac = (ASN1Sequence) in.readObject();
        in.close();

        NetscapeCertRequest nscr = new NetscapeCertRequest(spkac);

        // Verify POPO, we don't care about the challenge, it's not important.
        nscr.setChallenge("challenge");

        if (nscr.verify("challenge") == false) {
            throw new SignRequestSignatureException(
                "Invalid signature in NetscapeCertRequest, popo-verification failed.");
        }

        log.debug("POPO verification successful");

        X509Certificate cert = (X509Certificate) signsession.createCertificate(administrator,
                username, password, nscr.getPublicKey());

        // Don't include certificate chain in the PKCS7 to Firefox
        byte[] pkcs7 = signsession.createPKCS7(administrator, cert, false);
        log.debug("Created certificate (PKCS7) for " + username);
        if (debug != null) {
            debug.print("<h4>Generated certificate:</h4>");
            debug.printInsertLineBreaks(cert.toString().getBytes());
        }

        return pkcs7;
    } //nsCertRequest

    /**
     * Handles PKCS10 certificate request, these are constructed as: <code> CertificationRequest
     * ::= SEQUENCE { certificationRequestInfo  CertificationRequestInfo, signatureAlgorithm
     * AlgorithmIdentifier{{ SignatureAlgorithms }}, signature                       BIT STRING }
     * CertificationRequestInfo ::= SEQUENCE { version             INTEGER { v1(0) } (v1,...),
     * subject             Name, subjectPKInfo   SubjectPublicKeyInfo{{ PKInfoAlgorithms }},
     * attributes          [0] Attributes{{ CRIAttributes }}} SubjectPublicKeyInfo { ALGORITHM :
     * IOSet} ::= SEQUENCE { algorithm           AlgorithmIdentifier {{IOSet}}, subjectPublicKey
     * BIT STRING }</code> PublicKey's encoded-format has to be RSA X.509.
     *
     * @param signsession signsession to get certificate from
     * @param b64Encoded base64 encoded pkcs10 request message
     * @param username username of requesting user
     * @param password password of requesting user
     * @param resulttype should indicate if a PKCS7 or just the certificate is wanted.
     * @param doSplitLines
     * @return Base64 encoded byte[] 
     * @throws Exception
     */
    public byte[] pkcs10CertRequest(ISignSessionLocal signsession, byte[] b64Encoded,
        String username, String password, int resulttype, boolean doSplitLines) throws Exception {
        byte[] result = null;	
        Certificate cert=null;
		PKCS10RequestMessage req = RequestMessageUtils.genPKCS10RequestMessage(b64Encoded);
		req.setUsername(username);
        req.setPassword(password);
        IResponseMessage resp = signsession.createCertificate(administrator,req,Class.forName(X509ResponseMessage.class.getName()));
        cert = CertTools.getCertfromByteArray(resp.getResponseMessage());
        if(resulttype == ENCODED_CERTIFICATE) {
          result = cert.getEncoded();
        } else {  
          result = signsession.createPKCS7(administrator, cert, true);
        }
        log.debug("Created certificate (PKCS7) for " + username);
        if (debug != null) {
            debug.print("<h4>Generated certificate:</h4>");
            debug.printInsertLineBreaks(cert.toString().getBytes());
        }
        return Base64.encode(result, doSplitLines);
    } //pkcs10CertReq

    /** Handles CVC certificate requests. These are the special certificates for EAC ePassport PKI.
     * 
     * @param signsession signsession to get certificate from
     * @param b64Encoded base64 encoded cvc request message
     * @param username username of requesting user
     * @param password password of requesting user
     * @return Base64 encoded byte[] 
     * @throws Exception
     */
    public byte[] cvcCertRequest(ISignSessionLocal signsession, byte[] b64Encoded, String username, String password) throws Exception {            
			CVCRequestMessage req = RequestMessageUtils.genCVCRequestMessage(b64Encoded);
    		req.setUsername(username);
            req.setPassword(password);
            // Yes it says X509ResponseMessage, but for CVC it means it just contains the binary certificate blob
            IResponseMessage resp = signsession.createCertificate(administrator,req,Class.forName(X509ResponseMessage.class.getName()));
            Certificate cert = CertTools.getCertfromByteArray(resp.getResponseMessage());
            byte[] result = cert.getEncoded();
            log.debug("Created CV certificate for " + username);
            if (debug != null) {
                debug.print("<h4>Generated certificate:</h4>");
                debug.printInsertLineBreaks(cert.toString().getBytes());            	
            }
            return Base64.encode(result);
        } //cvcCertRequest

    /**
     * 
     * @param signsession
     * @param b64Encoded
     * @param username
     * @param password
     * @param resulttype
     * @return
     * @throws Exception
     */
    public byte[] pkcs10CertRequest(ISignSessionLocal signsession, byte[] b64Encoded,
                                    String username, String password, int resulttype) throws Exception {
        return pkcs10CertRequest(signsession, b64Encoded, username, password, resulttype, true);
    }    

    /**
     * Formats certificate in form to be received by IE
     *
     * @param bA input
     * @param out Output
     */
    public static void ieCertFormat(byte[] bA, PrintStream out)
        throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bA)));
        int rowNr = 0;

        while (true) {
            String line = br.readLine();

            if (line == null) {
                break;
            }

            if (line.indexOf("END CERT") < 0) {
                if (line.indexOf(" CERT") < 0) {
                    if (++rowNr > 1) {
                        out.println(" & _ ");
                    } else {
                        out.print("    cert = ");
                    }

                    out.print('\"' + line + '\"');
                }
            } else {
                break;
            }
        }

        out.println();
    } // ieCertFormat

    /**
     * @param certificate b64 encoded cert to be installed in netid
     * @param response output stream to send to
     * @param sc serveltcontext
     * @param responseTemplate path to template page for response
     * @throws Exception
     */
    public static void sendNewCertToIidClient(byte[] certificate, HttpServletRequest request, OutputStream out, ServletContext sc,
                                                String responseTemplate, String classid) throws Exception {
    	log.trace(">sendNewCertToIidClient");
        if ( certificate.length <= 0 ) {
            log.error("0 length certificate can not be sent to  client!");
            return;
        }
        StringWriter sw = new StringWriter();
        {
            InputStream is = sc.getResourceAsStream(responseTemplate);
            if (is == null) {
            	// Some app servers (oracle) require a / first...
            	log.debug("Trying to read responseTemplate with / first");
                is = sc.getResourceAsStream("/"+responseTemplate);
            }
            if (is == null) {
            	throw new IOException("Template '(/)"+responseTemplate+"' can not be found or read.");
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String baseURL = request.getRequestURL().toString().substring(0, request.getRequestURL().toString().lastIndexOf(
            		request.getRequestURI().toString()) ) + request.getContextPath() + "/";
            // If we would like to parse the jsp stuff instead so we could use "include" etc, we could use the below code
            // unfortunately if we are using https this will not work correctly, because we can not make a https connection here.
            /*
            String responseURL = baseURL + responseTemplate;
            BufferedReader br = new BufferedReader(new InputStreamReader( (new URL(responseURL)).openStream() ));
            */
            PrintWriter pw = new PrintWriter(sw);
            while (true) {
                String line = br.readLine();
                if (line == null) {
                    break;
                }
                line = line.replaceAll("\\x2E\\x2E/", baseURL);		// This line should be removed when headers are properly configured with absolute paths
                line = line.replaceAll("TAG_cert",new String(certificate));
                line = CLASSID.matcher(line).replaceFirst(classid);
                pw.println(line);
            }
            pw.close();
            sw.flush();
        }
        PrintWriter pw = new PrintWriter(out);
        if (log.isDebugEnabled()) {
            log.debug(sw);
        }
        pw.print(sw);
        pw.close();
        out.flush();
    	log.trace("<sendNewCertToIidClient");
    } // sendCertificates
    /**
     * Reads template and inserts cert to send back to IE for installation of cert
     *
     * @param b64cert cert to be installed in IE-client
     * @param out utput stream to send to
     * @param sc serveltcontext
     * @param responseTemplate path to responseTemplate
     * @param classid replace
     *
     * @throws Exception on error
     */
    public static void sendNewCertToIEClient(byte[] b64cert, OutputStream out, ServletContext sc,
        String responseTemplate, String classid) throws Exception {
        if (b64cert.length == 0) {
            log.error("0 length certificate can not be sent to IE client!");
            return;
        }

        PrintStream ps = new PrintStream(out);
        log.debug("Response template is: "+responseTemplate);
        InputStream is = sc.getResourceAsStream(responseTemplate);
        if (is == null) {
        	// Some app servers (oracle) require a / first...
        	log.debug("Trying to read responseTemplate with / first");
            is = sc.getResourceAsStream("/"+responseTemplate);
        }
        if (is == null) {
        	throw new IOException("Template '(/)"+responseTemplate+"' can not be found or read.");
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        while (true) {
            String line = br.readLine();

            if (line == null) {
                break;
            }

            if (line.indexOf("cert =") < 0) {
                ps.println(CLASSID.matcher(line).replaceFirst(classid));
            } else {
                RequestHelper.ieCertFormat(b64cert, ps);
            }
        }

        ps.close();
        log.debug("Sent reply to IE client");
        log.debug(new String(b64cert));
    } // sendNewCertToIEClient

    /**
     * Sends back cert to Firefox for installation of cert
     *
     * @param certs DER encoded certificates to be installed in browser
     * @param out output stream to send to
     *
     * @throws Exception on error
     */
    public static void sendNewCertToNSClient(byte[] certs, HttpServletResponse out)
        throws Exception {
    	log.trace(">nsCertRequest");
        if (certs.length == 0) {
            log.error("0 length certificate can not be sent to NS client!");
            return;
        }

        // Set content-type to what NS wants
        out.setContentType("application/x-x509-user-cert");
        out.setContentLength(certs.length);

        // Print the certificate
        out.getOutputStream().write(certs);
        log.debug("Sent reply to NS client");
        log.debug(new String(Base64.encode(certs)));
    	log.trace("<nsCertRequest");
    } // sendNewCertToNSClient

    /**
     * Sends back certificate as binary file (application/octet-stream)
     *
     * @param b64cert base64 encoded certificate to be returned
     * @param out output stream to send to
     * @param filename filename sent as 'Content-disposition' header 
     * @param beginKey, String contaitning key information, ie BEGIN_CERTIFICATE_WITH_NL or BEGIN_PKCS7_WITH_NL
     * @param beginKey, String contaitning key information, ie END_CERTIFICATE_WITH_NL or END_PKCS7_WITH_NL
     * @throws IOException 
     * @throws Exception on error
     */
    public static void sendNewB64File(byte[] b64cert, HttpServletResponse out, String filename, String beginKey, String endKey) 
    throws IOException {
        if (b64cert.length == 0) {
            log.error("0 length certificate can not be sent to client!");
            return;
        }

        // We must remove cache headers for IE
        ServletUtils.removeCacheHeaders(out);

        // Set content-type to general file
        out.setContentType("application/octet-stream");        
        out.setHeader("Content-disposition", "filename=\""+filename+"\"");

        out.setContentLength(b64cert.length + beginKey.length() + endKey.length());

        // Write the certificate
        ServletOutputStream os = out.getOutputStream();
        os.write(beginKey.getBytes());
        os.write(b64cert);
        os.write(endKey.getBytes());
        out.flushBuffer();
        log.debug("Sent reply to client");
        log.debug(new String(b64cert));        
    }
    /**
     * Sends back certificate as binary file (application/octet-stream)
     *
     * @param b64cert base64 encoded certificate to be returned
     * @param out output stream to send to
     * @param beginKey, String contaitning key information, ie BEGIN_CERTIFICATE_WITH_NL or BEGIN_PKCS7_WITH_NL
     * @param beginKey, String contaitning key information, ie END_CERTIFICATE_WITH_NL or END_PKCS7_WITH_NL
     * @throws Exception on error
     */
    public static void sendNewB64Cert(byte[] b64cert, HttpServletResponse out, String beginKey, String endKey)
        throws IOException {
        RequestHelper.sendNewB64File(b64cert, out, "cert.pem", beginKey, endKey);
    } // sendNewB64Cert

    /**
     * Sends back CA-certificate as binary file (application/x-x509-ca-cert)
     *
     * @param cert DER encoded certificate to be returned
     * @param out output stream to send to
     *
     * @throws Exception on error
     */
    public static void sendNewX509CaCert(byte[] cert, HttpServletResponse out)
        throws Exception {
        // Set content-type to CA-cert
        sendBinaryBytes(cert, out, "application/x-x509-ca-cert", null);
    } // sendNewX509CaCert

    /**
     * Sends back a number of bytes
     *
     * @param bytes DER encoded certificate to be returned
     * @param out output stream to send to
     * @param contentType mime type to send back bytes as
     * @param fileName to call the file in a Content-disposition, can be null to leave out this header
     *
     * @throws Exception on error
     */
    public static void sendBinaryBytes(byte[] bytes, HttpServletResponse out, String contentType, String filename)
        throws Exception {
        if ( (bytes == null) || (bytes.length == 0) ) {
            log.error("0 length can not be sent to client!");
            return;
        }

        if (filename != null) {
            // We must remove cache headers for IE
            ServletUtils.removeCacheHeaders(out);
            out.setHeader("Content-disposition", "filename=\""+filename+"\"");        	
        }

        // Set content-type to general file
        out.setContentType(contentType);
        out.setContentLength(bytes.length);

        // Write the certificate
        ServletOutputStream os = out.getOutputStream();
        os.write(bytes);
        out.flushBuffer();
        log.debug("Sent " + bytes.length + " bytes to client");
    } // sendBinaryBytes
    
    /** Returns the default content encoding used in JSPs. Reads the env-entry contentEncoding from web.xml.
     * 
     * @return The content encoding set in the webs env-entry java:comp/env/contentEncoding, or ISO-8859-1 (default), never returns null.
     */
    public static String getDefaultContentEncoding() {
        String ret = null;
        try {
            ret = ServiceLocator.getInstance().getString("java:comp/env/contentEncoding");            
        } catch (ServiceLocatorException e) {
            log.debug("Can not find any default content encoding, using hard default ISO-8859-1.");
            ret = "ISO-8859-1";            
        }
        if (ret == null) {
            log.debug("Can not find any default content encoding, using hard default ISO-8859-1.");
            ret = "ISO-8859-1";
        } 
        return ret;
    }
    
    /** Sets the default character encoding for decoding post and get parameters. 
     * First tries to get the character encoding from the request, if the browser is so kind to tell us which it is using, which it never does...
     * Otherwise, when the browser is silent, it sets the character encoding to the same encoding that we use to display the pages.
     * 
     * @param request HttpServletRequest   
     * @throws UnsupportedEncodingException 
     * 
     */
    public static void setDefaultCharacterEncoding(HttpServletRequest request) throws UnsupportedEncodingException {
        String encoding = request.getCharacterEncoding();
        if(StringUtils.isEmpty(encoding)) {
            encoding = RequestHelper.getDefaultContentEncoding();
            log.debug("Setting encoding to default value: "+encoding);
            request.setCharacterEncoding(encoding);
        } else {
            log.debug("Setting encoding to value from request: "+encoding);
            request.setCharacterEncoding(encoding);         
        }        
    }
        
}
