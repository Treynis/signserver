package org.signserver.mailsigner.cli;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;

import org.signserver.common.ISignerCertReqData;
import org.signserver.common.ISignerCertReqInfo;
import org.signserver.common.InvalidWorkerIdException;
import org.signserver.common.CryptoTokenAuthenticationFailureException;
import org.signserver.common.CryptoTokenOfflineException;
import org.signserver.common.WorkerConfig;
import org.signserver.common.WorkerStatus;

public interface IMailSignerRMI extends Remote{
	
	/**
	 * Returns the current status of a mail signer. 
	 *
	 * Should be used with the cmd-line status command.
	 * @param signerId of the signer
	 * @return a MailSignerStatus object
	 *  
	 */
	public WorkerStatus getStatus(int workerId) throws InvalidWorkerIdException, RemoteException;
	
	/**
	 * Returns the Id of a  given a name of a MailSigner 
	 *
	 * @param signerName of the mail signer, cannot be null
	 * @return The Id of a named signer or 0 if no such name exists
	 *  
	 */
	public int getWorkerId(String signerName) throws RemoteException;
	
	/**
	 * Method used when a configuration have been updated. And should be
	 * called from the command line.
	 *	  
	 *
	 * @param workerId of the mail signer that should be reloaded, or 0 to reload
	 *  all available mail signers 
	 */
	public void reloadConfiguration(int workerId) throws RemoteException;
	
	/**
	 * Method used to activate the sign-token of a mail signer.
	 * Should be called from the command line.
	 *    
	 * 
	 * @param signerId of the mail signer
	 * @param authenticationCode (PIN) used to activate the token.
	 * 
	 * @throws CryptoTokenOfflineException 
	 * @throws CryptoTokenAuthenticationFailureException 
	 *
	 */
	public void activateSigner(int signerId, String authenticationCode)
		throws CryptoTokenAuthenticationFailureException,
		CryptoTokenOfflineException, InvalidWorkerIdException, RemoteException;
	
	/**
	 * Method used to deactivate the sign-token of a mail signer.
	 * Should be called from the command line.
	 *    
	 * 
	 * @param signerId of the signer
	 * @param authenticationCode (PIN) used to activate the token.
	 * @return true if deactivation was successful.
	 * @throws CryptoTokenOfflineException 
	 * @throws InvalidWorkerIdException 
	 *
	 */
	public boolean deactivateSigner(int signerId)
		throws CryptoTokenOfflineException, InvalidWorkerIdException, RemoteException;
	
	/**
	 * Method used to upload a certificate to a signers configuration
	 * 
	 * @param signerId id of the signer
	 * @param signerCert the certificate used to sign signature requests
	 *  
	 */
	public void uploadSignerCertificate(int signerId, X509Certificate signerCert) throws RemoteException;
	
	/**
	 * Method used to upload a complete certificate chain to a configuration
	 * 
	 * @param signerId id of the signer
	 * @param signerCerts the certificate chain used to sign signature requests
	 */
	public void uploadSignerCertificateChain(int signerId, Collection<Certificate> signerCerts) throws RemoteException;
	
	/**
	 * Method used to remove a key from a mail signer.
	 * 
	 * @param signerId id of the signer
	 * @param purpose on of ICryptoToken.PURPOSE_ constants
	 * @return true if removal was successful.
	 * 
	 */
	public boolean destroyKey(int signerId, int purpose) throws	InvalidWorkerIdException, RemoteException;
	
	/**
	 * Method used to let a mail signer generate a certificate request
	 * using the signers own genCertificateRequest method
	 * 
	 * @param signerId id of the signer
	 * @param certReqInfo information used by the signer to create the request
	 * 
	 */
	public ISignerCertReqData genCertificateRequest(int signerId, ISignerCertReqInfo certReqInfo) throws		
		CryptoTokenOfflineException, InvalidWorkerIdException, RemoteException;
	
	/**
	 * Returns the current configuration of a mail signer.
	 * 
	 * Observe that this config might not be active until a reload command have been executed.
	 * 
	 * 
	 * @param signerId
	 * @return the current (not always active) configuration
	 * 
	 *  
	 */
	public WorkerConfig getCurrentSignerConfig(int signerId) throws RemoteException;
	
	/**
	 * Sets a parameter in a worker configuration
	 * 
	 * Observe that the worker isn't activated with this config until reload is performed.
	 * 
	 * @param workerId
	 * @param key
	 * @param value
	 * 
	 */
	public void setWorkerProperty(int workerId, String key, String value) throws RemoteException;
	
	/**
	 * Removes a given workers property
	 * 
	 * @param workerId
	 * @param key
	 * @return true if the property did exist and was removed otherwise false
	 * 
	 */	
	public boolean removeWorkerProperty(int workerId, String key) throws RemoteException;
	
	/**
	 * Method setting a global configuration property. For node. prefix will the node id be appended.
	 * @param scope one of the GlobalConfiguration.SCOPE_ constants
	 * @param key of the property should not have any scope prefix, never null
	 * @param value the value, never null.
	 */
	public void setGlobalProperty( java.lang.String scope,java.lang.String key,java.lang.String value )
	throws java.rmi.RemoteException;

	   /**
	    * Method used to remove a property from the global configuration.
	    * @param scope  one of the GlobalConfiguration.SCOPE_ constants
	    * @param key of the property should not have any scope prefix, never null
	    * @return true if removal was successful, otherwise false.
	    */
	   public boolean removeGlobalProperty( java.lang.String scope,java.lang.String key )
	      throws java.rmi.RemoteException;

	   /**
	    * Method that returns all the global properties with Global Scope and Node scopes properties for this node.
	    * @return A GlobalConfiguration Object, never null
	    */
	   public org.signserver.common.GlobalConfiguration getGlobalConfiguration(  )
	      throws java.rmi.RemoteException;
	   
		/**
		 * Methods that generates a free worker id that can be used for new signers
		 */
		public int genFreeWorkerId()throws java.rmi.RemoteException;

		/**
		 * Method returning a list of available workers.
		 * @param workerType constant defined in GlobalConfiguration.WORKERTYPE_
		 * @return the list of available worker id's
		 * @throws java.rmi.RemoteException
		 */
		public List<Integer> getWorkers(int workerType) throws java.rmi.RemoteException;
}
