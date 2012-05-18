package org.ejbca.core.protocol.xkms.client;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import javax.xml.bind.JAXBElement;

import org.ejbca.core.model.ca.crl.RevokedCertInfo;
import org.ejbca.core.protocol.xkms.common.XKMSConstants;
import org.ejbca.util.CertTools;
import org.ejbca.util.keystore.P12toPEM;
import org.w3._2000._09.xmldsig_.X509DataType;
import org.w3._2002._03.xkms_.KeyBindingType;
import org.w3._2002._03.xkms_.StatusType;
import org.w3._2002._03.xkms_.UnverifiedKeyBindingType;
import org.w3._2002._03.xkms_.UseKeyWithType;

/**
 * Base class inherited by all XKMS cli commands.
 * Checks the property file and creates a webservice connection.
 *  
 * @author Philip Vendil
 * $Id: XKMSCLIBaseCommand.java 5718 2008-06-10 13:03:58Z anatom $
 */

public abstract class XKMSCLIBaseCommand {
	
	protected String[] args = null;
	private XKMSInvoker xkms = null;
	private Properties props = null;
	private String password = null;
	
	protected X509Certificate clientCert = null;
	protected Key privateKey = null;
	private Collection catrustlist = null;
	
	
	protected static final String[] REASON_TEXTS ={"NOT REVOKED","UNSPECIFIED","KEYCOMPROMISE","CACOMPROMISE",
		"AFFILIATIONCHANGED","SUPERSEDED","CESSATIONOFOPERATION",
		"CERTIFICATEHOLD","REMOVEFROMCRL","PRIVILEGESWITHDRAWN",
	"AACOMPROMISE"};
	
    protected static final String RESPONDWITH_X509CERT           = "X509CERT";
    protected static final String RESPONDWITH_X509CHAIN          = "X509CHAIN";
    protected static final String RESPONDWITH_X509CHAINANDCRL    = "X509CHAINANDCRL";
    
    protected static final String ENCODING_PEM        = "pem";
    protected static final String ENCODING_DER        = "der";
    protected static final String ENCODING_P12        = "p12";
    protected static final String ENCODING_JKS        = "jks";
    
    protected static final String KEYUSAGE_ALL                  = "ALL";
    protected static final String KEYUSAGE_SIGNATURE            = "SIGNATURE";
    protected static final String KEYUSAGE_ENCRYPTION           = "ENCRYPTION";
    protected static final String KEYUSAGE_EXCHANGE             = "EXCHANGE";
    
    protected static final String QUERYTYPE_CERT               = "CERT";			
    protected static final String QUERYTYPE_SMIME              = "SMIME";	
    protected static final String QUERYTYPE_TLS                = "TLS";
    protected static final String QUERYTYPE_TLSHTTP            = "TLSHTTP";
    protected static final String QUERYTYPE_TLSSMTP            = "TLSSMTP";
    protected static final String QUERYTYPE_IPSEC              = "IPSEC";
    protected static final String QUERYTYPE_PKIX               = "PKIX";
	
	public static final int NOT_REVOKED = RevokedCertInfo.NOT_REVOKED;
	public static final int REVOKATION_REASON_UNSPECIFIED = RevokedCertInfo.REVOKATION_REASON_UNSPECIFIED;
	public static final int REVOKATION_REASON_KEYCOMPROMISE = RevokedCertInfo.REVOKATION_REASON_KEYCOMPROMISE;
	public static final int REVOKATION_REASON_CACOMPROMISE = RevokedCertInfo.REVOKATION_REASON_CACOMPROMISE;
	public static final int REVOKATION_REASON_AFFILIATIONCHANGED = RevokedCertInfo.REVOKATION_REASON_AFFILIATIONCHANGED;
	public static final int REVOKATION_REASON_SUPERSEDED = RevokedCertInfo.REVOKATION_REASON_SUPERSEDED;
	public static final int REVOKATION_REASON_CESSATIONOFOPERATION = RevokedCertInfo.REVOKATION_REASON_CESSATIONOFOPERATION;
	public static final int REVOKATION_REASON_CERTIFICATEHOLD = RevokedCertInfo.REVOKATION_REASON_CERTIFICATEHOLD;
	public static final int REVOKATION_REASON_REMOVEFROMCRL = RevokedCertInfo.REVOKATION_REASON_REMOVEFROMCRL;
	public static final int REVOKATION_REASON_PRIVILEGESWITHDRAWN = RevokedCertInfo.REVOKATION_REASON_PRIVILEGESWITHDRAWN;
	public static final int REVOKATION_REASON_AACOMPROMISE = RevokedCertInfo.REVOKATION_REASON_AACOMPROMISE;
	
	protected static final int[] REASON_VALUES = {NOT_REVOKED,REVOKATION_REASON_UNSPECIFIED, 
		 REVOKATION_REASON_KEYCOMPROMISE, REVOKATION_REASON_CACOMPROMISE,
		 REVOKATION_REASON_AFFILIATIONCHANGED, REVOKATION_REASON_SUPERSEDED,
		 REVOKATION_REASON_CESSATIONOFOPERATION, REVOKATION_REASON_CERTIFICATEHOLD,
		 REVOKATION_REASON_REMOVEFROMCRL, REVOKATION_REASON_PRIVILEGESWITHDRAWN,
		 REVOKATION_REASON_AACOMPROMISE};
	
	XKMSCLIBaseCommand(String[] args){
		CertTools.installBCProvider();
		this.args = args;
		
	}
	
	/**
	 * Method creating a connection to the webservice
	 * using the information stored in the property files.
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	protected XKMSInvoker getXKMSInvoker() throws  FileNotFoundException, IOException{       
		if(xkms == null){
			
			  if(getKeyStorePath()!=null){
				  try{
				  KeyStore clientKeyStore = KeyStore.getInstance("JKS");				  
			      clientKeyStore.load(new FileInputStream(getKeyStorePath()), getKeyStorePassword().toCharArray());
			      if(getKeyStoreAlias() == null){
			    	  throw new IOException("Error no alias specified in the property file");
			      }
			      String alias = getKeyStoreAlias();       
			      clientCert = (java.security.cert.X509Certificate)clientKeyStore.getCertificate(alias);            
			      privateKey = clientKeyStore.getKey(alias,"foo123".toCharArray());
			      Certificate[] trustedcerts = clientKeyStore.getCertificateChain(alias);
			      catrustlist = new ArrayList();
			      for(int i=0;i<trustedcerts.length;i++ ){
			    	if(((X509Certificate)trustedcerts[i]).getBasicConstraints() != -1){
			    		catrustlist.add(trustedcerts[i]);
			    	}
			      }
				  }catch(Exception e){
					  throw new IOException("Error reading client keystore " + e.getMessage());
				  }			      
			  }
									   		
			xkms = new XKMSInvoker(getWebServiceURL(),catrustlist);

		}
                
        return xkms;
        
	}

	private String getKeyStorePassword() throws FileNotFoundException, IOException {
		if(password == null){
			if(getProperties().getProperty("xkmscli.keystore.password") == null){
			   BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			   System.out.print("Enter keystore password :");
			   password = reader.readLine();
			}else{
				password = getProperties().getProperty("xkmscli.keystore.password");
			}
		}
		return password;
	}

	private String getKeyStorePath() throws FileNotFoundException, IOException {
		return getProperties().getProperty("xkmscli.keystore.path");
	}

	private String getKeyStoreAlias() throws FileNotFoundException, IOException {
		return getProperties().getProperty("xkmscli.keystore.alias");
	}
	
	private String getWebServiceURL() throws FileNotFoundException, IOException {	
		return getProperties().getProperty("xkmscli.url", "http://localhost:8080/ejbca/xkms/xkms");
	}

	private Properties getProperties() throws FileNotFoundException, IOException  {
		if(props == null){
		  props  = new Properties();
		  try {
			props.load(new FileInputStream("xkmscli.properties"));
		  } catch (FileNotFoundException e) {
			// Try in parent directory
			props.load(new FileInputStream("../xkmscli.properties"));
		  }
		}
		return props;
	}
	
	protected PrintStream getPrintStream(){
		return System.out;
	}
	
	protected int getRevokeReason(String reason) throws Exception{
		for(int i=0;i<REASON_TEXTS.length;i++){
		   if(REASON_TEXTS[i].equalsIgnoreCase(reason)){
			   return REASON_VALUES[i];
		   }
		}		
		getPrintStream().println("Error : Unsupported reason " + reason);
		usage();
		System.exit(-1);
		return 0;
	}
	
	protected String genId() throws NoSuchAlgorithmException {
        BigInteger serno = null;		
        Random random = SecureRandom.getInstance("SHA1PRNG");

        long seed = Math.abs((new Date().getTime()) + this.hashCode());
        random.setSeed(seed);
		try {
	        byte[] sernobytes = new byte[8];

	        random.nextBytes(sernobytes);
	        serno = (new java.math.BigInteger(sernobytes)).abs();
	       
		} catch (Exception e) {
			getPrintStream().println("Error generating response ID " );
		}
		return "_" + serno.toString();
	}
	
	/**
     * Returns a collection of resonswith tags.
     * 
     * @param arg
     * @return a collection of Strings containging respond with constatns
     */
    protected Collection getResponseWith(String arg) {
    	ArrayList retval = new ArrayList();
		
    	if(arg.equalsIgnoreCase(RESPONDWITH_X509CERT)){
    		retval.add(XKMSConstants.RESPONDWITH_X509CERT);
    		return retval;
    	}

    	if(arg.equalsIgnoreCase(RESPONDWITH_X509CHAIN)){
    		retval.add(XKMSConstants.RESPONDWITH_X509CHAIN);
    		return retval;
    	}
    	
    	if(arg.equalsIgnoreCase(RESPONDWITH_X509CHAINANDCRL)){
    		retval.add(XKMSConstants.RESPONDWITH_X509CHAIN);
    		retval.add(XKMSConstants.RESPONDWITH_X509CRL);
    		return retval;
    	}
    	
		getPrintStream().println("Illegal response with " + arg);
        usage();
    	System.exit(-1);
		return null;
	}
	
	/**
     * Method that loads a certificate from file 
     * @param filename
     * @return
     */
    protected byte[] loadCert(String arg) {
		try {
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(arg));
			byte[] retval = new byte[bis.available()];
			bis.read(retval);
			return retval;
			
		} catch (FileNotFoundException e) {
			getPrintStream().println("Couldn't find file with name " + arg);
	        usage();
	    	System.exit(-1);
		} catch (IOException e) {
			getPrintStream().println("Couldn't read file with name " + arg);
	        usage();
	    	System.exit(-1);
		}
		return null;
	}
	
	protected String getRevokeReason(int reason) {
		for(int i=0;i<REASON_VALUES.length;i++){
			   if(REASON_VALUES[i]==reason){
				   return REASON_TEXTS[i];
			   }
			}		
		getPrintStream().println("Error : Unsupported reason " + reason);
		usage();
		System.exit(-1);
		return null;		
	}
	
	protected void displayKeyUsage(UnverifiedKeyBindingType next) {
		Iterator<String> iter = next.getKeyUsage().iterator();
		getPrintStream().println("  Certificate have the following key usage:");
		if(next.getKeyUsage().size() == 0){
			getPrintStream().println("    " + KEYUSAGE_ALL );
		}
		while(iter.hasNext()){
			String keyUsage = iter.next();
			if(keyUsage.equals(XKMSConstants.KEYUSAGE_SIGNATURE)){
				getPrintStream().println("    " + KEYUSAGE_SIGNATURE );				
			}
			if(keyUsage.equals(XKMSConstants.KEYUSAGE_ENCRYPTION)){
				getPrintStream().println("    " + KEYUSAGE_ENCRYPTION);				
			}
			if(keyUsage.equals(XKMSConstants.KEYUSAGE_EXCHANGE)){
				getPrintStream().println("    " + KEYUSAGE_EXCHANGE);				
			}
		}				
		
	}
	


	protected void displayUseKeyWith(UnverifiedKeyBindingType next) {
		Iterator<UseKeyWithType> iter = next.getUseKeyWith().iterator();
		if(next.getKeyUsage().size() != 0){
			getPrintStream().println("  Certificate can be used with applications:");
			while(iter.hasNext()){
				UseKeyWithType useKeyWith = iter.next();
				if(useKeyWith.getApplication().equals(XKMSConstants.USEKEYWITH_IPSEC)){
					getPrintStream().println("    " + QUERYTYPE_IPSEC + " = " + useKeyWith.getIdentifier());				
				}
				if(useKeyWith.getApplication().equals(XKMSConstants.USEKEYWITH_PKIX)){
					getPrintStream().println("    " + QUERYTYPE_PKIX + " = " + useKeyWith.getIdentifier());				
				}
				if(useKeyWith.getApplication().equals(XKMSConstants.USEKEYWITH_SMIME)){
					getPrintStream().println("    " + QUERYTYPE_SMIME + " = " + useKeyWith.getIdentifier());				
				}
				if(useKeyWith.getApplication().equals(XKMSConstants.USEKEYWITH_TLS)){
					getPrintStream().println("    " + QUERYTYPE_TLS + " = " + useKeyWith.getIdentifier());				
				}
				if(useKeyWith.getApplication().equals(XKMSConstants.USEKEYWITH_TLSHTTP)){
					getPrintStream().println("    " + QUERYTYPE_TLSHTTP + " = " + useKeyWith.getIdentifier());				
				}
				if(useKeyWith.getApplication().equals(XKMSConstants.USEKEYWITH_TLSSMTP)){
					getPrintStream().println("    " + QUERYTYPE_TLSSMTP + " = " + useKeyWith.getIdentifier());				
				}
			}
		}
	}
	
	   /**
     * Stores keystore.
     *
     * @param ks         KeyStore
     * @param username   username, the owner of the keystore
     * @param kspassword the password used to protect the peystore
     * @param createJKS  if a jks should be created
     * @param createPEM  if pem files should be created
     * @throws IOException if directory to store keystore cannot be created
     */
    protected void storeKeyStore(KeyStore ks, String username, String kspassword, boolean createJKS,
                               boolean createPEM, String mainStoreDir)
            throws IOException, KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException,
            NoSuchProviderException, CertificateException {       

        // Where to store it?
        if (mainStoreDir == null) {
            throw new IOException("Can't find directory to store keystore in.");
        }

        String keyStoreFilename = mainStoreDir  + username;

        if (createJKS) {
            keyStoreFilename += ".jks";
        } else {
            keyStoreFilename += ".p12";
        }

        // If we should also create PEM-files, do that
        if (createPEM) {
            String PEMfilename = mainStoreDir + "pem";
            P12toPEM p12topem = new P12toPEM(ks, kspassword, true);
            p12topem.setExportPath(PEMfilename);
            p12topem.createPEM();
            getPrintStream().println("Keystore written successfully to the directory " + PEMfilename);
        } else {
            FileOutputStream os = new FileOutputStream(keyStoreFilename);
            ks.store(os, kspassword.toCharArray());
            getPrintStream().println("Keystore written successfully to " + keyStoreFilename);
        }
        
        

    } // storeKeyStore
	
	protected void displayStatus(KeyBindingType type) {
		StatusType status = type.getStatus();
		getPrintStream().println("  The certificate had the following status");
		getPrintStream().println("  Valid:");
		displayStatusReasons(status.getValidReason());
		getPrintStream().println("  Indeterminable:");
		displayStatusReasons(status.getIndeterminateReason());
		getPrintStream().println("  Invalid:");
		displayStatusReasons(status.getInvalidReason());
		
	}

	private void displayStatusReasons(List<String> reasons) {
		if(reasons.size() == 0){
			getPrintStream().println("      NONE");
		}else{
			Iterator<String> iter = reasons.iterator();
			while(iter.hasNext()){
				String next = iter.next();
				if(next.equals(XKMSConstants.STATUSREASON_ISSUERTRUST)){
					getPrintStream().println("      ISSUERTRUST");
				}
				if(next.equals(XKMSConstants.STATUSREASON_REVOCATIONSTATUS)){
					getPrintStream().println("      REVOCATIONSTATUS");
				}
				if(next.equals(XKMSConstants.STATUSREASON_SIGNATURE)){
					getPrintStream().println("      SIGNATURE");
				}
				if(next.equals(XKMSConstants.STATUSREASON_VALIDITYINTERVAL)){
					getPrintStream().println("      VALIDITYINTERVAL");
				}
			}
		}
	}

	protected List getCertsFromKeyBinding(KeyBindingType keyBinding) throws CertificateException {
		ArrayList retval = new ArrayList();
		
		JAXBElement<X509DataType> jAXBX509Data = (JAXBElement<X509DataType>) keyBinding.getKeyInfo().getContent().get(0);		
		Iterator iter2 = jAXBX509Data.getValue().getX509IssuerSerialOrX509SKIOrX509SubjectName().iterator();
		while(iter2.hasNext()){
			JAXBElement next = (JAXBElement) iter2.next();					
			if(next.getName().getLocalPart().equals("X509Certificate")){
			  byte[] encoded = (byte[]) next.getValue();
			  Certificate nextCert = CertTools.getCertfromByteArray(encoded);
			  retval.add(nextCert);
			}
		}	
		
		return retval;
	}


	protected abstract void usage();

}
