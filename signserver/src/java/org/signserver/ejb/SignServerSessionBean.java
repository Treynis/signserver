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


package org.signserver.ejb;

import java.math.BigInteger;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.log4j.Logger;
import org.ejbca.util.CertTools;
import org.signserver.common.ArchiveDataVO;
import org.signserver.common.AuthorizedClient;
import org.signserver.common.GlobalConfiguration;
import org.signserver.common.ISignRequest;
import org.signserver.common.ISignResponse;
import org.signserver.common.ISignerCertReqData;
import org.signserver.common.ISignerCertReqInfo;
import org.signserver.common.IllegalSignRequestException;
import org.signserver.common.InvalidSignerIdException;
import org.signserver.common.SignTokenAuthenticationFailureException;
import org.signserver.common.SignTokenOfflineException;
import org.signserver.common.SignerConfig;
import org.signserver.common.WorkerConfig;
import org.signserver.common.WorkerStatus;
import org.signserver.ejb.interfaces.IGlobalConfigurationSession;
import org.signserver.ejb.interfaces.IServiceTimerSession;
import org.signserver.ejb.interfaces.ISignServerSession;
import org.signserver.server.IWorker;
import org.signserver.server.WorkerFactory;
import org.signserver.server.signers.BaseSigner;
import org.signserver.server.signers.ISigner;

/**
 * The main sign server session bean
 * 
 */
@Stateless
public class SignServerSessionBean implements ISignServerSession.ILocal, ISignServerSession.IRemote  {
    @PersistenceContext(unitName="SignServerJPA")
    EntityManager em;

	private static final long serialVersionUID = 1L;

	@EJB
	private IGlobalConfigurationSession.ILocal globalConfigurationSession;

	@EJB
	private IServiceTimerSession.ILocal serviceTimerSession; 
	
	/** Log4j instance for actual implementation class */
	public transient Logger log = Logger.getLogger(this.getClass());

    /** The local home interface of Worker Config entity bean. */
    private WorkerConfigDataService workerConfigService = null;
    
    /** The local home interface of archive entity bean. */
    private ArchiveDataService archiveDataService = null;

    @PostConstruct
	public void create() {
    	workerConfigService = new WorkerConfigDataService(em);
    	archiveDataService = new ArchiveDataService(em);
	}
	
	

	/* (non-Javadoc)
	 * @see org.signserver.ejb.ISignServerSession#signData(int, org.signserver.common.ISignRequest, java.security.cert.X509Certificate, java.lang.String)
	 */
	public ISignResponse signData(int signerId, ISignRequest request,
	                              X509Certificate clientCert, String requestIP) throws IllegalSignRequestException,
		SignTokenOfflineException {
		log.debug(">signData " + request.getRequestID());
		IWorker worker = WorkerFactory.getInstance().getWorker(signerId, workerConfigService, globalConfigurationSession);
		
        if(worker == null){
        	throw new IllegalSignRequestException("Non-existing signerId");
        }
        
        if(!(worker instanceof ISigner)){
        	throw new IllegalSignRequestException("Worker exists but isn't a signer.");
        }
		ISigner signer = (ISigner) worker;
        
        if(signer.getAuthenticationType() == ISigner.AUTHTYPE_CLIENTCERT){
        	if(clientCert == null){
        		throw new IllegalSignRequestException("Error, client authentication is required.");   
        	}else{
        		if(!authorizedToRequestSignature(clientCert, new SignerConfig (signer.getStatus().getActiveSignerConfig()).getAuthorizedClients())){        	                
        			
        			throw new IllegalSignRequestException("Error, client '" + clientCert.getSubjectDN().toString() + "' requesting signature from signer with id : " + 
        					signerId + " isn't an authorized client. ");   
        		}
        	}
        }
        
        if(signer.getStatus().getActiveSignerConfig().getProperties().getProperty(BaseSigner.DISABLED,"FALSE").equalsIgnoreCase("TRUE")){
        	throw new SignTokenOfflineException("Error Signer : " + signerId + " is disabled and cannot perform any signature operations");
        }
        
        ISignResponse res = signer.signData(request,  clientCert);

        if(signer.getStatus().getActiveSignerConfig().getProperties().getProperty(BaseSigner.ARCHIVE,"FALSE").equalsIgnoreCase("TRUE")){
        	if(res.getArchiveData() != null){    			
    			  archiveDataService.create(ArchiveDataVO.TYPE_RESPONSE,signerId, res.getArchiveId(), clientCert, requestIP, res.getArchiveData());        		        	
        	}else{
        		log.error("Error archiving response generated of signer " + signerId + ", archiving is not supported by signer.");
        	}
        }
        
        log.info("Signer " + signerId + " Processed request " + res.getRequestID() + " Successfully");
        
		log.debug("<signData " + request.getRequestID());
		return res;
	}



	private boolean authorizedToRequestSignature(X509Certificate clientCert, Collection<AuthorizedClient> authorizedClients) {

        boolean isAuthorized = false;
        final Iterator<AuthorizedClient> iter = authorizedClients.iterator();
        String clientDN = CertTools.stringToBCDNString(clientCert.getIssuerDN().toString()); 
        
        while( iter.hasNext() && !isAuthorized ){
            AuthorizedClient next = (AuthorizedClient) iter.next();
            try {
                // If both authorized clients Issuer DN And Cert Serial match, 
                // the client is authorized.
                isAuthorized = clientDN.equals(next.getIssuerDN()) &&
                               clientCert.getSerialNumber().equals(new BigInteger(next.getCertSN(),16));
            }catch( IllegalArgumentException e) {
                log.warn(e.getMessage() + " for athorized client");
            }
        }
		return isAuthorized;
	}

	/* (non-Javadoc)
	 * @see org.signserver.ejb.ISignServerSession#getStatus(int)
	 */
	public WorkerStatus getStatus(int workerId) throws InvalidSignerIdException{
		IWorker worker = WorkerFactory.getInstance().getWorker(workerId, workerConfigService, globalConfigurationSession);
		if(worker == null){
			throw new InvalidSignerIdException("Given SignerId " + workerId + " doesn't exist");
		}
		
		
		return worker.getStatus();
	}

	/* (non-Javadoc)
	 * @see org.signserver.ejb.ISignServerSession#getSignerId(java.lang.String)
	 */
	public int getSignerId(String signerName) {
		return WorkerFactory.getInstance().getSignerIdFromName(signerName.toUpperCase(), workerConfigService, globalConfigurationSession);		
	}
	 
	
	/* (non-Javadoc)
	 * @see org.signserver.ejb.ISignServerSession#reloadConfiguration(int)
	 */
	public void reloadConfiguration(int workerId) {
		if(workerId == 0){
		  globalConfigurationSession.reload();
		}else{
			WorkerFactory.getInstance().reloadWorker(workerId, workerConfigService, globalConfigurationSession);
		}
		
		if(workerId == 0 || globalConfigurationSession.getWorkers(GlobalConfiguration.WORKERTYPE_SERVICES).contains(new Integer(workerId))){
		  serviceTimerSession.unload(workerId);
		  serviceTimerSession.load(workerId);
		}
	}

	/* (non-Javadoc)
	 * @see org.signserver.ejb.ISignServerSession#activateSigner(int, java.lang.String)
	 */
	public void activateSigner(int signerId, String authenticationCode)
		throws SignTokenAuthenticationFailureException,
		SignTokenOfflineException, InvalidSignerIdException {
		IWorker worker = WorkerFactory.getInstance().getWorker(signerId, workerConfigService,globalConfigurationSession);
		if(worker == null){
			throw new InvalidSignerIdException("Given SignerId " + signerId + " doesn't exist");
		}
		
        if(!(worker instanceof ISigner)){
        	throw new InvalidSignerIdException("Worker exists but isn't a signer.");
        }
		ISigner signer = (ISigner) worker;
		
		signer.activateSigner(authenticationCode);
	}

	/* (non-Javadoc)
	 * @see org.signserver.ejb.ISignServerSession#deactivateSigner(int)
	 */
	public boolean deactivateSigner(int signerId)
		throws SignTokenOfflineException, InvalidSignerIdException {
		IWorker worker = WorkerFactory.getInstance().getWorker(signerId, workerConfigService,globalConfigurationSession);
		if(worker == null){
			throw new InvalidSignerIdException("Given SignerId " + signerId + " doesn't exist");
		}
		
        if(!(worker instanceof ISigner)){
        	throw new InvalidSignerIdException("Worker exists but isn't a signer.");
        }
		ISigner signer = (ISigner) worker;
		
		return signer.deactivateSigner();
	}

	/* (non-Javadoc)
	 * @see org.signserver.ejb.ISignServerSession#getCurrentSignerConfig(int)
	 */
	public WorkerConfig getCurrentSignerConfig(int signerId){
        return getSignerConfig(signerId); 				
	}
	
	/* (non-Javadoc)
	 * @see org.signserver.ejb.ISignServerSession#setWorkerProperty(int, java.lang.String, java.lang.String)
	 */
	public void setWorkerProperty(int workerId, String key, String value){
		WorkerConfig config = getSignerConfig(workerId);
		config.setProperty(key.toUpperCase(),value);
		workerConfigService.setWorkerConfig(workerId, config);		
	}
	
	/* (non-Javadoc)
	 * @see org.signserver.ejb.ISignServerSession#removeWorkerProperty(int, java.lang.String)
	 */	
	public boolean removeWorkerProperty(int workerId, String key){
		boolean result = false;
		WorkerConfig config = getSignerConfig(workerId);
				
		result = config.removeProperty(key.toUpperCase());
		if(config.getProperties().size() == 0){
			workerConfigService.removeWorkerConfig(workerId);
			log.debug("WorkerConfig is empty and therefore removed.");
		}else{
			workerConfigService.setWorkerConfig(workerId,config);
		}
		return result;
	}	
	
	/* (non-Javadoc)
	 * @see org.signserver.ejb.ISignServerSession#getAuthorizedClients(int)
	 */
	public Collection<AuthorizedClient> getAuthorizedClients(int signerId){
		return new SignerConfig( getSignerConfig(signerId)).getAuthorizedClients();
	}
	
	/* (non-Javadoc)
	 * @see org.signserver.ejb.ISignServerSession#addAuthorizedClient(int, org.signserver.common.AuthorizedClient)
	 */
	public void addAuthorizedClient(int signerId, AuthorizedClient authClient){
		WorkerConfig config = getSignerConfig(signerId);
		(new SignerConfig(config)).addAuthorizedClient(authClient);
		workerConfigService.setWorkerConfig(signerId, config);		
	}

	/* (non-Javadoc)
	 * @see org.signserver.ejb.ISignServerSession#removeAuthorizedClient(int, org.signserver.common.AuthorizedClient)
	 */
	public boolean removeAuthorizedClient(int signerId, AuthorizedClient authClient){
		boolean result = false;
		WorkerConfig config = getSignerConfig(signerId);
		
		
		result = (new SignerConfig(config)).removeAuthorizedClient(authClient);
		workerConfigService.setWorkerConfig(signerId, config);
		return result;
	}
	
	/* (non-Javadoc)
	 * @see org.signserver.ejb.ISignServerSession#getCertificateRequest(int, org.signserver.common.ISignerCertReqInfo)
	 */
	public ISignerCertReqData getCertificateRequest(int signerId, ISignerCertReqInfo certReqInfo) throws		
		SignTokenOfflineException, InvalidSignerIdException {
			IWorker worker = WorkerFactory.getInstance().getWorker(signerId, workerConfigService,globalConfigurationSession);
			if(worker == null){
				throw new InvalidSignerIdException("Given SignerId " + signerId + " doesn't exist");
			}
			
	        if(!(worker instanceof ISigner)){
	        	throw new InvalidSignerIdException("Worker exists but isn't a signer.");
	        }
			ISigner signer = (ISigner) worker;
			
			return signer.genCertificateRequest(certReqInfo);
	}
	
	/* (non-Javadoc)
	 * @see org.signserver.ejb.ISignServerSession#destroyKey(int, int)
	 */
	public boolean destroyKey(int signerId, int purpose) throws	InvalidSignerIdException {
			IWorker worker = WorkerFactory.getInstance().getWorker(signerId, workerConfigService,globalConfigurationSession);
			if(worker == null){
				throw new InvalidSignerIdException("Given SignerId " + signerId + " doesn't exist");
			}
			
	        if(!(worker instanceof ISigner)){
	        	throw new InvalidSignerIdException("Worker exists but isn't a signer.");
	        }
			ISigner signer = (ISigner) worker;
			
			return signer.destroyKey(purpose);
	}
	
	/* (non-Javadoc)
	 * @see org.signserver.ejb.ISignServerSession#uploadSignerCertificate(int, java.security.cert.X509Certificate, java.lang.String)
	 */
	public void uploadSignerCertificate(int signerId, X509Certificate signerCert, String scope){		
		WorkerConfig config = getSignerConfig(signerId);

		( new SignerConfig(config)).setSignerCertificate(signerCert,scope);
		workerConfigService.setWorkerConfig(signerId, config);
	}
	
	/* (non-Javadoc)
	 * @see org.signserver.ejb.ISignServerSession#uploadSignerCertificateChain(int, java.util.Collection, java.lang.String)
	 */
	public void uploadSignerCertificateChain(int signerId, Collection<Certificate> signerCerts, String scope){		
		
		WorkerConfig config = getSignerConfig(signerId);
		(new SignerConfig( config)).setSignerCertificateChain(signerCerts, scope);
		workerConfigService.setWorkerConfig(signerId, config);
	}
	
	/* (non-Javadoc)
	 * @see org.signserver.ejb.ISignServerSession#genFreeWorkerId()
	 */
	public int genFreeWorkerId(){
		Collection<Integer> ids =  globalConfigurationSession.getWorkers(GlobalConfiguration.WORKERTYPE_ALL);
		int max = 0;
		Iterator<Integer> iter = ids.iterator();
		while(iter.hasNext()){
			Integer id =  iter.next();
			if(id.intValue() > max){
				max = id.intValue();
			}
		}
		
		return max+1;
	}
	
	/* (non-Javadoc)
	 * @see org.signserver.ejb.ISignServerSession#findArchiveDataFromArchiveId(int, java.lang.String)
	 */
	public ArchiveDataVO findArchiveDataFromArchiveId(int signerId, String archiveId){
		ArchiveDataVO retval = null;
		
		ArchiveDataBean adb = archiveDataService.findByArchiveId(ArchiveDataVO.TYPE_RESPONSE,signerId,archiveId);
		if(adb != null){
			retval = adb.getArchiveDataVO();
		}
		
		return retval;
	}
	
	/* (non-Javadoc)
	 * @see org.signserver.ejb.ISignServerSession#findArchiveDatasFromRequestIP(int, java.lang.String)
	 */
	public List<ArchiveDataVO> findArchiveDatasFromRequestIP(int signerId, String requestIP){
		ArrayList<ArchiveDataVO> retval = new ArrayList<ArchiveDataVO>();

		Collection<ArchiveDataBean> result = archiveDataService.findByRequestIP(ArchiveDataVO.TYPE_RESPONSE,signerId,requestIP);
		Iterator<ArchiveDataBean> iter = result.iterator();
		while(iter.hasNext()){
			ArchiveDataBean next =  iter.next();
			retval.add(next.getArchiveDataVO());
		}

		return retval;
	}
	
	/* (non-Javadoc)
	 * @see org.signserver.ejb.ISignServerSession#findArchiveDatasFromRequestCertificate(int, java.math.BigInteger, java.lang.String)
	 */
	public List<ArchiveDataVO> findArchiveDatasFromRequestCertificate(int signerId, BigInteger requestCertSerialnumber, String requestCertIssuerDN){
		ArrayList<ArchiveDataVO> retval = new ArrayList<ArchiveDataVO>();

		Collection<ArchiveDataBean> result = archiveDataService.findByRequestCertificate(ArchiveDataVO.TYPE_RESPONSE,signerId,CertTools.stringToBCDNString(requestCertIssuerDN),requestCertSerialnumber.toString(16));
		Iterator<ArchiveDataBean> iter = result.iterator();
		while(iter.hasNext()){
			ArchiveDataBean next = iter.next();
			retval.add(next.getArchiveDataVO());
		}
		
		return retval;
	}	
	
	
	private WorkerConfig getSignerConfig(int signerId){
		WorkerConfig signerConfig = workerConfigService.getWorkerConfig(signerId);
	    if(signerConfig == null){
	    	workerConfigService.create(signerId, WorkerConfig.class.getName());
			signerConfig = workerConfigService.getWorkerConfig(signerId);
		}
		return signerConfig;	
	}
	


    
}
