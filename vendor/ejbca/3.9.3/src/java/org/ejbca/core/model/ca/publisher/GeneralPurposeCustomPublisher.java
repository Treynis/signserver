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
 
package org.ejbca.core.model.ca.publisher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.ejbca.core.ejb.ca.store.CertificateDataBean;
import org.ejbca.core.model.InternalResources;
import org.ejbca.core.model.log.Admin;
import org.ejbca.core.model.ra.ExtendedInformation;
import org.ejbca.util.CertTools;

/**
 * This class is used for publishing to user defined script or command.
 *
 * @version $Id: GeneralPurposeCustomPublisher.java 7453 2009-05-09 13:58:06Z anatom $
 */
public class GeneralPurposeCustomPublisher implements ICustomPublisher{
    private static Logger log = Logger.getLogger(GeneralPurposeCustomPublisher.class);
    private static final InternalResources intres = InternalResources.getInstance();

	public static final String crlExternalCommandPropertyName			= "crl.application";
	public static final String certExternalCommandPropertyName			= "cert.application";
	public static final String revokeExternalCommandPropertyName		= "revoke.application";
	public static final String crlFailOnErrorCodePropertyName			= "crl.failOnErrorCode";
	public static final String certFailOnErrorCodePropertyName			= "cert.failOnErrorCode";
	public static final String revokeFailOnErrorCodePropertyName		= "revoke.failOnErrorCode";
	public static final String crlFailOnStandardErrorPropertyName		= "crl.failOnStandardError";
	public static final String certFailOnStandardErrorPropertyName		= "cert.failOnStandardError";
	public static final String revokeFailOnStandardErrorPropertyName	= "revoke.failOnStandardError";
	
	private String crlExternalCommandFileName		= null;
	private String certExternalCommandFileName		= null;
	private String revokeExternalCommandFileName	= null;
	private boolean crlFailOnErrorCode				= true;
	private boolean certFailOnErrorCode				= true;
	private boolean revokeFailOnErrorCode			= true;
	private boolean crlFailOnStandardError			= true;
	private boolean certFailOnStandardError			= true;
	private boolean revokeFailOnStandardError		= true;

    /**
     * Creates a new instance of DummyCustomPublisher
     */
    public GeneralPurposeCustomPublisher() {}

	/**
	 * Load used properties.
	 * @param properties The properties to load.
	 * 
	 * @see org.ejbca.core.model.ca.publisher.ICustomPublisher#init(java.util.Properties)
	 */
	public void init(Properties properties) {
		log.trace(">init");		
		// Extract system properties
		crlFailOnErrorCode = properties.getProperty(crlFailOnErrorCodePropertyName, "true").equalsIgnoreCase("true");
		crlFailOnStandardError = properties.getProperty(crlFailOnStandardErrorPropertyName, "true").equalsIgnoreCase("true");
		crlExternalCommandFileName = properties.getProperty(crlExternalCommandPropertyName);
		certFailOnErrorCode = properties.getProperty(certFailOnErrorCodePropertyName, "true").equalsIgnoreCase("true");
		certFailOnStandardError = properties.getProperty(certFailOnStandardErrorPropertyName, "true").equalsIgnoreCase("true");
		certExternalCommandFileName = properties.getProperty(certExternalCommandPropertyName);
		revokeFailOnErrorCode = properties.getProperty(revokeFailOnErrorCodePropertyName, "true").equalsIgnoreCase("true");
		revokeFailOnStandardError = properties.getProperty(revokeFailOnStandardErrorPropertyName, "true").equalsIgnoreCase("true");
		revokeExternalCommandFileName = properties.getProperty(revokeExternalCommandPropertyName);
	} // init

	/**
	 * Writes crtificate to temporary file and executes an external command with the full pathname
	 * of the temporary file as argument. The temporary file is the encoded form of the certificate
	 * e.g. X.509 certificates would be encoded as ASN.1 DER. All parameters but incert are ignored.
	 * @param incert The certificate
	 * @param username The username
	 * @param type The certificate type
	 * 
	 * @see org.ejbca.core.model.ca.publisher.ICustomPublisher#storeCertificate(org.ejbca.core.model.log.Admin, java.security.cert.Certificate, java.lang.String, java.lang.String, int, int)
	 */
	public boolean storeCertificate(Admin admin, Certificate incert, String username, String password, String cafp, int status, int type, long revocationDate, int revocationReason, String tag, int certificateProfileId, long lastUpdate, ExtendedInformation extendedinformation) throws PublisherException {
        if (log.isTraceEnabled()) {
        	log.trace(">storeCertificate, Storing Certificate for user: " + username);	
        }
        
        if ( (status == CertificateDataBean.CERT_REVOKED) || (status == CertificateDataBean.CERT_TEMP_REVOKED) ) {
        	// Call separate script for revocation
        	revokeCertificate(admin, incert, revocationReason);
        } else if (status == CertificateDataBean.CERT_ACTIVE) {
            // Don't publish non-active certificates
            // Make sure that an external command was specified
    		if ( certExternalCommandFileName == null ) {
    			String msg = intres.getLocalizedMessage("publisher.errormissingproperty", certExternalCommandPropertyName);
            	log.error(msg);
    			throw new PublisherException(msg);
    		}
    		// Run internal method to create tempfile and run the command
    		List arguments = new ArrayList();	//<String>
    		arguments.add(String.valueOf(type));
    		try {
    			arguments.add(CertTools.getSubjectDN(incert));
    			arguments.add(CertTools.getIssuerDN(incert));
    			arguments.add(CertTools.getSerialNumberAsString(incert));
    			runWithTempFile(certExternalCommandFileName, incert.getEncoded(), certFailOnErrorCode, certFailOnStandardError, arguments);
    		} catch (CertificateEncodingException e) {
    			String msg = intres.getLocalizedMessage("publisher.errorcertconversion");
            	log.error(msg);
    			throw new PublisherException(msg);
    		}        	
        }
		return true;
	} // storeCertificate

	/**
	 * Writes the CRL to a temporary file and executes an external command with the temporary file as argument.
	 * By default, a PublisherException is thrown if the external command returns with an errorlevel or outputs
	 * to stderr. 
	 * 
	 * @see org.ejbca.core.model.ca.publisher.ICustomPublisher#storeCRL(org.ejbca.core.model.log.Admin, byte[], java.lang.String, int)
	 */
	public boolean storeCRL(Admin admin, byte[] incrl, String cafp, int number) throws PublisherException {
        log.trace(">storeCRL, Storing CRL");
        // Verify initialization 
		if ( crlExternalCommandFileName == null ) {
			String msg = intres.getLocalizedMessage("publisher.errormissingproperty", crlExternalCommandPropertyName);
        	log.error(msg);
			throw new PublisherException(msg);
		}
		// Run internal method to create tempfile and run the command
		runWithTempFile(crlExternalCommandFileName, incrl, crlFailOnErrorCode, crlFailOnStandardError);
		return true;
	}

	/**
	 * Writes certificate to temporary file and executes an external command with the full pathname
	 * of the temporary file as argument. The temporary file is the encoded form of the certificate
	 * e.g. X.509 certificates would be encoded as ASN.1 DER. All parameters but cert are ignored.
	 * @param cert The certificate
	 * 
	 */
	public void revokeCertificate(Admin admin, Certificate cert, int reason) throws PublisherException {
        log.trace(">revokeCertificate, Rekoving Certificate");
        // Verify initialization 
		if ( revokeExternalCommandFileName == null ) {
			String msg = intres.getLocalizedMessage("publisher.errormissingproperty", revokeExternalCommandPropertyName);
        	log.error(msg);
			throw new PublisherException(msg);
		}
		// Run internal method to create tempfile and run the command
		List arguments = new ArrayList();	//<String>
		arguments.add(String.valueOf(reason));
		try {
			arguments.add(CertTools.getSubjectDN(cert));
			arguments.add(CertTools.getIssuerDN(cert));
			arguments.add(CertTools.getSerialNumberAsString(cert));
			runWithTempFile(revokeExternalCommandFileName, cert.getEncoded(), revokeFailOnErrorCode, revokeFailOnStandardError, arguments);
		} catch (CertificateEncodingException e) {
			String msg = intres.getLocalizedMessage("publisher.errorcertconversion");
        	log.error(msg);
			throw new PublisherException(msg);
		}
	} // revokeCertificate

	/**
	 * Check if the specified external excutable file(s) exist.
	 * @param admin Ignored 
	 * 
	 * @see org.ejbca.core.model.ca.publisher.ICustomPublisher#testConnection(org.ejbca.core.model.log.Admin)
	 */
	public void testConnection(Admin admin) throws PublisherConnectionException {
        log.trace("testConnection, Testing connection");
        // Test if specified commands exist
        if ( crlExternalCommandFileName != null ) {
        	if ( !(new File(crlExternalCommandFileName)).exists() ) {
    			String msg = intres.getLocalizedMessage("publisher.commandnotfound", crlExternalCommandFileName);
            	log.error(msg);
    			throw new PublisherConnectionException(msg);
        	}
        }
        if ( certExternalCommandFileName != null ) {
        	if ( !(new File(certExternalCommandFileName)).exists() ) {
    			String msg = intres.getLocalizedMessage("publisher.commandnotfound", certExternalCommandFileName);
            	log.error(msg);
    			throw new PublisherConnectionException(msg);
        	}
        }
        if ( revokeExternalCommandFileName != null ) {
        	if ( !(new File(revokeExternalCommandFileName)).exists() ) {
    			String msg = intres.getLocalizedMessage("publisher.commandnotfound", revokeExternalCommandFileName);
            	log.error(msg);
    			throw new PublisherConnectionException(msg);
        	}
        }
	} // testConnection

	/**
	 * Does nothing.
	 */
	protected void finalize() throws Throwable {
        log.trace(">finalize, doing nothing");
		super.finalize(); 
	} // finalize
	

	/**
	 * Writes a byte-array to a temporary file and executes the given command with the file as argument. The
	 * function will, depending on its parameters, fail if output to standard error from the command was
	 * detected or the command returns with an non-zero exit code. 
	 * 
	 * @param externalCommand The command to run.
	 * @param bytes The buffer with content to write to the file.
	 * @param failOnCode Determines if the method should fail on a non-zero exit code.
	 * @param failOnOutput Determines if the method should fail on output to standard error.
	 * @throws PublisherException
	 */
	private void runWithTempFile(String externalCommand, byte[] bytes, boolean failOnCode, boolean failOnOutput) throws PublisherException {
		List additionalArguments = new ArrayList();	// <String>
		runWithTempFile(externalCommand, bytes, failOnCode, failOnOutput, additionalArguments);
	}

	/**
	 * Writes a byte-array to a temporary file and executes the given command with the file as argument. The
	 * function will, depending on its parameters, fail if output to standard error from the command was
	 * detected or the command returns with an non-zero exit code. 
	 * 
	 * @param externalCommand The command to run.
	 * @param bytes The buffer with content to write to the file.
	 * @param failOnCode Determines if the method should fail on a non-zero exit code.
	 * @param failOnOutput Determines if the method should fail on output to standard error.
	 * @param additionalArgument Added to the command after the tempfiles name
	 * @throws PublisherException
	 */
	private void runWithTempFile(String externalCommand, byte[] bytes, boolean failOnCode, boolean failOnOutput, String additionalArgument) throws PublisherException {
		List additionalArguments = new ArrayList();	// <String>
		additionalArguments.add(additionalArgument);
		runWithTempFile(externalCommand, bytes, failOnCode, failOnOutput, additionalArguments);
	}

	/**
	 * Writes a byte-array to a temporary file and executes the given command with the file as argument. The
	 * function will, depending on its parameters, fail if output to standard error from the command was
	 * detected or the command returns with an non-zero exit code. 
	 * 
	 * @param externalCommand The command to run.
	 * @param bytes The buffer with content to write to the file.
	 * @param failOnCode Determines if the method should fail on a non-zero exit code.
	 * @param failOnOutput Determines if the method should fail on output to standard error.
	 * @param additionalArguments Added to the command after the tempfiles name
	 * @throws PublisherException
	 */
	private void runWithTempFile(String externalCommand, byte[] bytes, boolean failOnCode, boolean failOnOutput, List additionalArguments) throws PublisherException {
		// Create temporary file
		File tempFile 			= null;
		FileOutputStream fos	= null;
		try {
			tempFile = File.createTempFile("GeneralPurposeCustomPublisher", ".tmp");
			fos = new FileOutputStream(tempFile);
			fos.write(bytes);
			//fos.close();
		} catch (FileNotFoundException e) {
			String msg = intres.getLocalizedMessage("publisher.errortempfile");
        	log.error(msg, e);
        	throw new PublisherException(msg);
		} catch (IOException e) {
			try {
				fos.close();
			} catch (IOException e1) {
			}
			tempFile.delete();
			String msg = intres.getLocalizedMessage("publisher.errortempfile");
        	log.error(msg, e);
        	throw new PublisherException(msg);
		}
		// Exec file from properties with the file as an argument
		String tempFileName = null;
		try {
			tempFileName = tempFile.getCanonicalPath();
			String[] cmdcommand = (externalCommand).split("\\s");
			additionalArguments.add(0, tempFileName);
			String[] cmdargs = (String[]) additionalArguments.toArray(new String[0]);
			String[] cmdarray = new String[cmdcommand.length+cmdargs.length];
			System.arraycopy(cmdcommand, 0, cmdarray, 0, cmdcommand.length);
			System.arraycopy(cmdargs, 0, cmdarray, cmdcommand.length, cmdargs.length);
			Process externalProcess = Runtime.getRuntime().exec( cmdarray, null, null);
			BufferedReader stdError = new BufferedReader( new InputStreamReader( externalProcess.getErrorStream() ) );
			BufferedReader stdInput = new BufferedReader( new InputStreamReader( externalProcess.getInputStream() ) );
			while ( stdInput.readLine() != null ) { }	// Required under win32 to avoid lock
			String stdErrorOutput = null;
			// Check errorcode and the external applications output to stderr 
			if ( ((externalProcess.waitFor() != 0) && failOnCode) || (stdError.ready() && failOnOutput )) {
				tempFile.delete();
				String errTemp = null;
				while ( stdError.ready() && (errTemp = stdError.readLine()) != null ) {
					if (stdErrorOutput == null) { 
						stdErrorOutput = errTemp;
					} else {
						stdErrorOutput += "\n" + errTemp;
					}
				}
				String msg = intres.getLocalizedMessage("publisher.errorexternalapp", externalCommand);
				if ( stdErrorOutput != null ) {
						msg += " - " + stdErrorOutput + " - "+ tempFileName;
				}
	        	log.error(msg);
				throw new PublisherException(msg);
			}
		} catch (IOException e) {
			String msg = intres.getLocalizedMessage("publisher.errorexternalapp", externalCommand);
        	log.error(msg, e);
			throw new PublisherException(msg);
		} catch (InterruptedException e) {
			String msg = intres.getLocalizedMessage("publisher.errorexternalapp", externalCommand);
        	log.error(msg, e);
			throw new PublisherException(msg);
		} finally {
			try {
				fos.close();
			} catch (IOException e1) {
			}
	        // Remove temporary file or schedule for delete if delete fails.
			if ( !tempFile.delete() ) {
				tempFile.deleteOnExit();
	        	log.info( intres.getLocalizedMessage("publisher.errordeletetempfile", tempFileName) );
			}
		}
	} // runWithTempFile
} // GeneralPurposeCustomPublisher
