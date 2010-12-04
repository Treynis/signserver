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

package org.signserver.protocol.ws.server;

import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.jws.WebService;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import org.signserver.common.CompileTimeSettings;
import org.signserver.common.CryptoTokenOfflineException;
import org.signserver.common.GlobalConfiguration;
import org.signserver.common.ProcessRequest;
import org.signserver.common.ProcessResponse;
import org.signserver.common.ISignResponse;
import org.signserver.common.IllegalRequestException;
import org.signserver.common.InvalidWorkerIdException;
import org.signserver.common.ProcessableConfig;
import org.signserver.common.RequestAndResponseManager;
import org.signserver.common.RequestContext;
import org.signserver.common.ServiceLocator;
import org.signserver.common.SignServerException;
import org.signserver.common.SignerStatus;
import org.signserver.common.WorkerStatus;
import org.signserver.ejb.interfaces.IGlobalConfigurationSession;
import org.signserver.ejb.interfaces.IWorkerSession;
import org.signserver.healthcheck.HealthCheckUtils;
import org.signserver.protocol.ws.Certificate;
import org.signserver.protocol.ws.ISignServerWS;
import org.signserver.protocol.ws.ProcessRequestWS;
import org.signserver.protocol.ws.ProcessResponseWS;
import org.signserver.protocol.ws.WorkerStatusWS;
import org.signserver.server.CertificateClientCredential;
import org.signserver.server.IClientCredential;
import org.signserver.server.UsernamePasswordClientCredential;
import org.signserver.web.SignServerHealthCheck;



/**
 * Implementor of the ISignServerWS interface.
 * 
 * @author Philip Vendil
 * $Id$
 */

@WebService(targetNamespace="gen.ws.protocol.signserver.org")
public class SignServerWS implements ISignServerWS {
	
	@Resource
	private WebServiceContext wsContext;	

	private static final Logger log = Logger.getLogger(SignServerWS.class);

        private static final String HTTP_AUTH_BASIC_AUTHORIZATION = "Authorization";
        

	public Collection<WorkerStatusWS> getStatus(String workerIdOrName)
			throws InvalidWorkerIdException {
		log.debug("WS getStatus called");
		ArrayList<WorkerStatusWS> retval = new ArrayList<WorkerStatusWS>();
		
		String errormessage = "";
		
		errormessage += HealthCheckUtils.checkDB(getCheckDBString());
		if(errormessage.equals("")){
		  errormessage += HealthCheckUtils.checkMemory(getMinimumFreeMemory());
		
		}
		
		if(errormessage.equals("")){
			// everything seems ok.
			errormessage = null;
		}
		
		int workerId = 0;
		try {
			if(!workerIdOrName.equalsIgnoreCase(ISignServerWS.ALLWORKERS)){
			  workerId = getWorkerId(workerIdOrName);
			}
		} catch (IllegalRequestException e) {
			throw new InvalidWorkerIdException("Worker id or name " + workerIdOrName + " couldn't be found.");
		}
		
		if(workerId != 0){
			// Specified WorkerId
			if(errormessage== null){
				  errormessage = checkSigner(workerId);
			}
			WorkerStatusWS resp = new WorkerStatusWS();
			resp.setWorkerName(workerIdOrName);
			if(errormessage == null){					
				resp.setOverallStatus(WorkerStatusWS.OVERALLSTATUS_ALLOK);
			}else{
				resp.setOverallStatus(WorkerStatusWS.OVERALLSTATUS_ERROR);
				resp.setErrormessage(errormessage);
			}
			retval.add(resp);
		}else{
			// All Workers
			List<Integer> signers = getGlobalConfigurationSession().getWorkers(GlobalConfiguration.WORKERTYPE_PROCESSABLE);
			for (Iterator<Integer> iterator = signers.iterator(); iterator.hasNext();) {
				int next =  iterator.next();
				if(errormessage== null){
				  errormessage = checkSigner(next);
				}
				
				WorkerStatusWS resp = new WorkerStatusWS();
				resp.setWorkerName("" +next);
				if(errormessage == null){					
					resp.setOverallStatus(WorkerStatusWS.OVERALLSTATUS_ALLOK);
				}else{
					resp.setOverallStatus(WorkerStatusWS.OVERALLSTATUS_ERROR);
					resp.setErrormessage(errormessage);
				}
				retval.add(resp);			    
			}							
		}
		return retval;
	}
	
	private String checkSigner(int workerId) throws InvalidWorkerIdException{
		String retval = null;
		WorkerStatus workerStatus = getWorkerSession().getStatus(workerId);
		String currentMessage = workerStatus.isOK();
		if(currentMessage != null){
			retval += currentMessage;
		}		
		return retval;
	}


   /**
    * @see  org.signserver.protocol.ws.ISignServerWS#process(String, Collection)
    */
	public Collection<ProcessResponseWS> process(String workerIdOrName,
			Collection<ProcessRequestWS> requests)
			throws InvalidWorkerIdException, IllegalRequestException,
			CryptoTokenOfflineException, SignServerException {
		ArrayList<ProcessResponseWS> retval = new ArrayList<ProcessResponseWS>();

                final HttpServletRequest servletRequest =
                        (HttpServletRequest) wsContext.getMessageContext()
                        .get(MessageContext.SERVLET_REQUEST);
                String requestIP = getRequestIP();
		X509Certificate clientCertificate = getClientCertificate();
                final RequestContext requestContext = new RequestContext(clientCertificate, requestIP);

                IClientCredential credential;

                if (clientCertificate instanceof X509Certificate) {
                    final X509Certificate cert = (X509Certificate) clientCertificate;
                    log.debug("Authentication: certificate");
                    credential = new CertificateClientCredential(
                            cert.getSerialNumber().toString(16),
                            cert.getIssuerDN().getName());
                } else {
                    // Check is client supplied basic-credentials
                    final String authorization = servletRequest.getHeader(
                            HTTP_AUTH_BASIC_AUTHORIZATION);
                    if (authorization != null) {
                        log.debug("Authentication: password");

                        final String decoded[] = new String(Base64.decode(
                                authorization.split("\\s")[1])).split(":", 2);

                        credential = new UsernamePasswordClientCredential(
                            decoded[0], decoded[1]);
                    } else {
                        log.debug("Authentication: none");
                        credential = null;
                    }
                }
                requestContext.put(RequestContext.CLIENT_CREDENTIAL, credential);
		
		int workerId = getWorkerId(workerIdOrName);
		
		ArrayList<Certificate> signerCertificateChain = getSignerCertificateChain(workerId);
		
		for (Iterator<ProcessRequestWS> iterator = requests.iterator(); iterator.hasNext();) {
			ProcessRequestWS next = iterator.next();
			ProcessRequest req;
			try {
				req = RequestAndResponseManager.parseProcessRequest(next.getRequestData());
			} catch (IOException e1) {
				log.error("Error parsing process request",e1);
				throw new IllegalRequestException(e1.getMessage());
			}
			ProcessResponse resp = getWorkerSession().process(workerId, req, requestContext);
			ProcessResponseWS wsresp = new ProcessResponseWS();
			try {
				wsresp.setResponseData(RequestAndResponseManager.serializeProcessResponse(resp));
			} catch (IOException e1) {
				log.error("Error parsing process response",e1);
				throw new SignServerException(e1.getMessage());
			}
			if(resp instanceof ISignResponse){
				wsresp.setRequestID(((ISignResponse)resp).getRequestID());
				try {
					wsresp.setWorkerCertificate(new Certificate(((ISignResponse)resp).getSignerCertificate()));
					wsresp.setWorkerCertificateChain(signerCertificateChain);					
				} catch (CertificateEncodingException e) {
					log.error(e);
				}
				
			}
			retval.add(wsresp);
		}
		return retval;
	}				


	private ArrayList<Certificate> getSignerCertificateChain(int workerId) throws InvalidWorkerIdException {
		ArrayList<Certificate> retval = null;
		try{
		  WorkerStatus ws = getWorkerSession().getStatus(workerId);
		  if(ws instanceof SignerStatus){
			ProcessableConfig sc = new ProcessableConfig(((SignerStatus) ws).getActiveSignerConfig());
			Collection<java.security.cert.Certificate> signerCertificateChain = sc.getSignerCertificateChain();
			
			if(signerCertificateChain != null){
				retval = new ArrayList<Certificate>();
				for (Iterator<java.security.cert.Certificate> iterator = signerCertificateChain.iterator(); iterator.hasNext();) {
					retval.add(new Certificate(iterator.next()));			
				}
			}
		  }
		}catch (CertificateEncodingException e) {
			log.error(e);
		}
		
		
		
	return null;
}


	private X509Certificate getClientCertificate(){
		MessageContext msgContext = wsContext.getMessageContext();
		HttpServletRequest request = (HttpServletRequest) msgContext.get(MessageContext.SERVLET_REQUEST);
		X509Certificate[] certificates = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");

		if(certificates != null){
			return certificates[0];
		}
		return null;
	}
	
	private String getRequestIP(){
		MessageContext msgContext = wsContext.getMessageContext();
		HttpServletRequest request = (HttpServletRequest) msgContext.get(MessageContext.SERVLET_REQUEST);
		
		return request.getRemoteAddr();
	}
	
    private int getWorkerId(String workerIdOrName) throws IllegalRequestException{
    	int retval = 0;
    	
    	if(workerIdOrName.substring(0, 1).matches("\\d")){
    		retval = Integer.parseInt(workerIdOrName);    		
    	}else{
    		retval = getWorkerSession().getWorkerId(workerIdOrName);
    		if(retval == 0){
    			throw new IllegalRequestException("Error: No worker with the given name could be found");
    		}
    	}
    	
    	return retval;
    }
    
    
    private int minimumFreeMemory = 1;
    private int getMinimumFreeMemory(){
        final String minMemory = CompileTimeSettings.getInstance().getProperty(
                CompileTimeSettings.HEALTHECK_MINIMUMFREEMEMORY);
      if (minMemory != null) {
    	  try{
    	    minimumFreeMemory = Integer.parseInt(minMemory.trim());
    	  }catch(NumberFormatException e){
    		  log.error("Error: SignServerWS badly configured, setting healthcheck.minimumfreememory should only contain integers");
    	  }
      }
      return minimumFreeMemory;
    }
    
    private String checkDBString = "Select count(*) from signerconfigdata";
    private String getCheckDBString(){
        final String dbString = CompileTimeSettings.getInstance().getProperty(
                CompileTimeSettings.HEALTHECK_CHECKDBSTRING);
      if (dbString != null) {
    	  checkDBString = dbString;
      }
      return checkDBString;
    }

    @EJB
	private IWorkerSession.ILocal workersession;
	
    private IWorkerSession.ILocal getWorkerSession(){
    	if(workersession == null){
    		try{
    		  workersession = ServiceLocator.getInstance().lookupLocal(
                          IWorkerSession.ILocal.class);
    		}catch(NamingException e){
    			log.error(e);
    		}
    	}
    	
    	return workersession;
    }

    @EJB
	private IGlobalConfigurationSession.ILocal globalconfigsession;
	
    private IGlobalConfigurationSession.ILocal getGlobalConfigurationSession(){
       if (globalconfigsession == null) {
            try {
                globalconfigsession = ServiceLocator.getInstance().lookupLocal(
                        IGlobalConfigurationSession.ILocal.class);
            } catch (NamingException e) {
                log.error(e);
            }
        }
        return globalconfigsession;
    }
}
