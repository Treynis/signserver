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
package org.signserver.mailsigner.core;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.AccessException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.mail.MessagingException;

import org.apache.log4j.Logger;
import org.apache.mailet.GenericMailet;
import org.apache.mailet.Mail;
import org.apache.mailet.MailetConfig;
import org.ejbca.util.CertTools;
import org.signserver.common.CryptoTokenAuthenticationFailureException;
import org.signserver.common.CryptoTokenOfflineException;
import org.signserver.common.GlobalConfiguration;
import org.signserver.common.ICertReqData;
import org.signserver.common.ISignerCertReqInfo;
import org.signserver.common.InvalidWorkerIdException;
import org.signserver.common.MailSignerConfig;
import org.signserver.common.MailSignerUser;
import org.signserver.common.WorkerConfig;
import org.signserver.common.WorkerStatus;
import org.signserver.mailsigner.BaseMailProcessor;
import org.signserver.mailsigner.IMailProcessor;
import org.signserver.mailsigner.cli.IMailSignerRMI;

/**
 * MailSignerContainerMailet is the base James Mailet that reads the 
 * mail signer configuration file and sets up the IMailSigners in the
 * system.
 * 
 * It also handles the calling of the mails to the configured
 * mail signers.
 * 
 * 
 * @author Philip Vendil
 * $Id: MailSignerContainerMailet.java,v 1.6 2008-01-19 03:42:11 herrvendil Exp $
 */
public class MailSignerContainerMailet extends GenericMailet implements IMailSignerRMI{

	public static final String TESTMODE_SETTING = "TESTMODE";
	
	private static transient Logger log = Logger.getLogger(MailSignerContainerMailet.class.getName());
	
    private ConcurrentHashMap<Integer, IMailProcessor> mailProcessors = new ConcurrentHashMap<Integer, IMailProcessor>();
	
    private MailSignerUserRepository userRepository = new MailSignerUserRepository();

    /*Boolean indicating this instance isn't run from a testscript */
	private boolean getMailContext;
    
    
    /**
     * Creates all the configured IMailProcessor plugins, initializes
     * them and send them their configuration.
     * 
     * Also sets up the RMI service for the CLI.
     */
	@Override
	public void init(MailetConfig mailetConfig) throws MessagingException {
		super.init(mailetConfig);
		getMailContext = true;
		try{
			     
			Registry registry = LocateRegistry.createRegistry(MailSignerConfig.getRMIRegistryPort());
			Remote stup = UnicastRemoteObject.exportObject(this,MailSignerConfig.getRMIServerPort());
			registry.rebind(MailSignerConfig.RMI_OBJECT_NAME, stup);
			log.info("MailSigner RMI interface bound successfully with registry on port: " + MailSignerConfig.getRMIRegistryPort() + " and server on port: " + MailSignerConfig.getRMIServerPort());			
		}catch(AccessException e){
			log.error("Failed binding MailSigner RMI interface.", e);
		}catch (RemoteException e) {
			log.error("Failed binding MailSigner RMI interface.", e);
		}
		
	}

	/**
	 * Method that sends the mail to all the configured mail signers.
	 * In id order (ascending) order.
	 */
	public void service(Mail mail) throws MessagingException{
		List<Integer> mailIds = NonEJBGlobalConfigurationSession.getInstance().getWorkers(GlobalConfiguration.WORKERTYPE_MAILSIGNERS);
		for (Integer id : mailIds) {
			try {
				IMailProcessor mailProcessor = getMailSigner(id);
				
				Properties activeProps = mailProcessor.getStatus().getActiveSignerConfig().getProperties();
				if(!activeProps.getProperty(BaseMailProcessor.DISABLED,"FALSE").equalsIgnoreCase("TRUE")){
				  getMailSigner(id).service(mail);
				}
			} catch (InvalidWorkerIdException e) {
				// Should never happen
				log.error(e);
			} catch (CryptoTokenOfflineException e){
				log.error("CryptoTokenOfflineException : " + e.getMessage(),e);
				throw new MessagingException(e.getMessage(),e);
			}
		}
		mailTest(mail);
	}

	/**
	 * Method used to test a mail signer, checks if setting TESTMODE is set
	 * to TRUE in global configuration. If so is the mail serialized and
	 * written to the temporary directory for later inspection.
	 * @param mail
	 */
	private void mailTest(Mail mail) {

		if(mail.getState() != Mail.ERROR){
			GlobalConfiguration gc = NonEJBGlobalConfigurationSession.getInstance().getGlobalConfiguration();
			if(gc.getProperty(GlobalConfiguration.SCOPE_GLOBAL, TESTMODE_SETTING)!= null &&
					gc.getProperty(GlobalConfiguration.SCOPE_GLOBAL, TESTMODE_SETTING).equalsIgnoreCase("TRUE")){
				String signserverhome = System.getenv("SIGNSERVER_HOME");
				if(signserverhome == null){
					log.error("Error performing test of mail signer, environment variable SIGNSERVER_HOME isn't set");
				}

				try {
					FileOutputStream fos = new FileOutputStream(signserverhome + "/tmp/testmail");				
					mail.getMessage().writeTo(fos);
					fos.close();
				} catch (FileNotFoundException e) {
					log.error("Error performing test of mail signer : " + e.getMessage());
				} catch (IOException e) {
					log.error("Error performing test of mail signer : " + e.getMessage());
				} catch (MessagingException e) {
					log.error("Error performing test of mail signer : " + e.getMessage());
				} 
				mail.setState(Mail.GHOST);
			}
		}
	}		


	/**
	 * @see org.signserver.mailsigner.cli.IMailSignerRMI#activateSigner(int, String)
	 */
	public void activateCryptoToken(int signerId, String authenticationCode)
			throws CryptoTokenAuthenticationFailureException,
			CryptoTokenOfflineException, InvalidWorkerIdException,
			RemoteException {
		getMailSigner(signerId).activateCryptoToken(authenticationCode);		
	}

	/**
	 * @see org.signserver.mailsigner.cli.IMailSignerRMI#deactivateSigner(int)
	 */
	public boolean deactivateCryptoToken(int signerId)
			throws CryptoTokenOfflineException, InvalidWorkerIdException,
			RemoteException {
		return getMailSigner(signerId).deactivateCryptoToken();
		
	}

	/**
	 * @see org.signserver.mailsigner.cli.IMailSignerRMI#destroyKey(int, int)
	 */
	public boolean destroyKey(int signerId, int purpose)
			throws InvalidWorkerIdException, RemoteException {		
		return getMailSigner(signerId).destroyKey(purpose);
	}

	/**
	 * @see org.signserver.mailsigner.cli.IMailSignerRMI#getCertificateRequest(int, ISignerCertReqInfo)
	 */
	public ICertReqData genCertificateRequest(int signerId,
			ISignerCertReqInfo certReqInfo) throws CryptoTokenOfflineException,
			InvalidWorkerIdException, RemoteException {
		
		return getMailSigner(signerId).genCertificateRequest(certReqInfo);
	}

	/**
	 * @see org.signserver.mailsigner.cli.IMailSignerRMI#getWorkerId(String)
	 */
	public int getWorkerId(String signerName) throws RemoteException {
		int retval = 0;
		
		List<Integer> signerIds = NonEJBGlobalConfigurationSession.getInstance().getWorkers(GlobalConfiguration.WORKERTYPE_MAILSIGNERS);
		for (Integer id : signerIds) {			
			try {
				String name = getMailSigner(id).getStatus().getActiveSignerConfig().getProperties().getProperty(MailSignerConfig.NAME);
				if(name != null && name.equalsIgnoreCase(signerName)){
					retval = id;
				}
			} catch (InvalidWorkerIdException e) {
				// Should never happen
				log.error(e);
			}

		}
		
		return retval;
	}

	/**
	 * @see org.signserver.mailsigner.cli.IMailSignerRMI#getStatus(int)
	 */
	public WorkerStatus getStatus(int workerId)
			throws InvalidWorkerIdException, RemoteException {
		
		return getMailSigner(workerId).getStatus();
	}

	/**
	 * @see org.signserver.mailsigner.cli.IMailSignerRMI#reloadConfiguration(int, Properties)
	 */
	public void reloadConfiguration(int workerId)
			throws RemoteException {
		if(workerId == 0){
		  NonEJBGlobalConfigurationSession.getInstance().reload();
		  mailProcessors.clear();
		}else{
		  mailProcessors.remove(workerId);
		}		
	}

	/**
	 * @see org.signserver.mailsigner.cli.IMailSignerRMI#uploadSignerCertificate(int, X509Certificate)
	 */
	public void uploadSignerCertificate(int signerId, X509Certificate signerCert)
			throws RemoteException {
		
		PropertyFileStore pfs = PropertyFileStore.getInstance();
		
		ArrayList<X509Certificate> list = new ArrayList<X509Certificate>();
		list.add(signerCert);
		try {
			String stringcert = new String(CertTools.getPEMFromCerts(list));
			pfs.setWorkerProperty(signerId, MailSignerConfig.SIGNERCERT, stringcert);				
		} catch (CertificateException e) {
			log.error(e);
		}
	}

	/**
	 * @see org.signserver.mailsigner.cli.IMailSignerRMI#uploadSignerCertificateChain(int, Collection)
	 */
	public void uploadSignerCertificateChain(int signerId,
			Collection<Certificate> signerCerts) throws RemoteException {
		PropertyFileStore pfs = PropertyFileStore.getInstance();

		try {
			String stringcert = new String(CertTools.getPEMFromCerts(signerCerts));
			pfs.setWorkerProperty(signerId, MailSignerConfig.SIGNERCERTCHAIN, stringcert);				
		} catch (CertificateException e) {
			log.error(e);
		}
		
	}
	
	/**
	 * @see org.signserver.mailsigner.cli.IMailSignerRMI#getCurrentSignerConfig(int)
	 */
	public WorkerConfig getCurrentSignerConfig(int signerId) {		
		return PropertyFileStore.getInstance().getWorkerProperties(signerId);
	}

	/**
	 * @see org.signserver.mailsigner.cli.IMailSignerRMI#removeWorkerProperty(int, String)
	 */
	public boolean removeWorkerProperty(int workerId, String key) {
		boolean exists = true;
		PropertyFileStore pfs = PropertyFileStore.getInstance();
		
		exists = pfs.getWorkerProperties(workerId).getProperties().containsKey(key);
		if(exists){
		  pfs.removeWorkerProperty(workerId, key);
		}
		return exists;
	}

	/**
	 * @see org.signserver.mailsigner.cli.IMailSignerRMI#setWorkerProperty(int, String, String)
	 */
	public void setWorkerProperty(int workerId, String key, String value) {
		 PropertyFileStore.getInstance().setWorkerProperty(workerId, key, value);		
	}
	
	  /**
	   * @see org.signserver.mailsigner.cli.IMailSignerRMI#getGlobalConfiguration()
	   */
		public GlobalConfiguration getGlobalConfiguration() {
			return NonEJBGlobalConfigurationSession.getInstance().getGlobalConfiguration();
		}

		  /**
		   * @see org.signserver.mailsigner.cli.IMailSignerRMI#removeGlobalProperty(String, String)
		   */
		public boolean removeGlobalProperty(String scope, String key) {
			return NonEJBGlobalConfigurationSession.getInstance().removeProperty(scope, key);			 
		}

		  /**
		   * @see org.signserver.mailsigner.cli.IMailSignerRMI#setGlobalProperty(String, String, String)
		   */
		public void setGlobalProperty(String scope, String key, String value) {
			NonEJBGlobalConfigurationSession.getInstance().setProperty(scope, key, value);			
		}
		
		public List<Integer> getWorkers(int workerType){
			return NonEJBGlobalConfigurationSession.getInstance().getWorkers(workerType);
		}
	
		/**
		 * Methods that generates a free workerid that can be used for new signers
		 */
		public int genFreeWorkerId(){
			Collection<?> ids =  NonEJBGlobalConfigurationSession.getInstance().getWorkers(GlobalConfiguration.WORKERTYPE_ALL);
			int max = 0;
			Iterator<?> iter = ids.iterator();
			while(iter.hasNext()){
				Integer id = (Integer) iter.next();
				if(id.intValue() > max){
					max = id.intValue();
				}
			}
			
			return max+1;
		}
		
		
	/**
	 * Method that finds and initializes a MailSigner
	 */
	private IMailProcessor getMailSigner(int signerId) throws InvalidWorkerIdException{
		IMailProcessor retval = mailProcessors.get(signerId);
				
		if(retval == null){
			List<Integer> mailIds = NonEJBGlobalConfigurationSession.getInstance().getWorkers(GlobalConfiguration.WORKERTYPE_MAILSIGNERS);
			for (Integer id : mailIds) {
				if(id.equals(signerId)){
					GlobalConfiguration gc = NonEJBGlobalConfigurationSession.getInstance().getGlobalConfiguration();
					String classPath = gc.getProperty(GlobalConfiguration.SCOPE_GLOBAL, GlobalConfiguration.WORKERPROPERTY_BASE + id + GlobalConfiguration.WORKERPROPERTY_CLASSPATH);
					
					try {
						IMailProcessor mailProcessor = (IMailProcessor) this.getClass().getClassLoader().loadClass(classPath).newInstance();
						mailProcessor.init(id, cloneWorkerProperties(PropertyFileStore.getInstance().getWorkerProperties(id)),  (getMailContext) ? getMailetContext() : null);
						mailProcessors.put(id, mailProcessor);
						retval = mailProcessor;
					} catch (Exception e) {
						log.error("Error creating an instance of mail signer with Id " + id,e);
					} 				
					break;
				}
			}			
		}
		
		if(retval == null){
			throw new InvalidWorkerIdException("Error, couldn't find signer id in global configuration.");
		}
		
		return retval;
	}

	private WorkerConfig cloneWorkerProperties(WorkerConfig workerProperties) {
		WorkerConfig retval = new WorkerConfig();
		
		Enumeration<Object> en = workerProperties.getProperties().keys();
		while(en.hasMoreElements()){
			String key = (String) en.nextElement();
			retval.getProperties().setProperty(key, workerProperties.getProperties().getProperty(key));
		}
		
		return retval;
	}

	/**
	 * @see org.signserver.mailsigner.cli.IMailSignerRMI#addAuthorizedUser(String, String)
	 */
	public void addAuthorizedUser(String username, String password) {
		userRepository.addUser(username, password);		
	}

	/**
	 * @see org.signserver.mailsigner.cli.IMailSignerRMI#getAuthorizedUsers()
	 */
	public List<MailSignerUser> getAuthorizedUsers() {		
		return userRepository.getUsersSorted();
	}

	/**
	 * @see org.signserver.mailsigner.cli.IMailSignerRMI#removeAuthorizedUser(String)
	 */
	public boolean removeAuthorizedUser(String username) {
		if(userRepository.containsCaseInsensitive(username)){
			userRepository.removeUser(username);
			return true;
		}
		return false;
	}

}
