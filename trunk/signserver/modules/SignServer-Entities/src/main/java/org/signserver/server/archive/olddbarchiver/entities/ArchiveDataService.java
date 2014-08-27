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
package org.signserver.server.archive.olddbarchiver.entities;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.apache.log4j.Logger;
import org.cesecore.util.query.QueryCriteria;
import org.cesecore.util.query.QueryGenerator;
import org.ejbca.util.CertTools;
import org.signserver.common.ArchiveData;
import org.signserver.common.ArchiveMetadata;

/**
 * Entity Service class that acts as migration layer for
 * the old Home Interface for the Archive Data Entity Bean.
 *
 * Contains about the same methods as the EJB 2 entity beans home interface.
 *
 * @version $Id$
 */
public class ArchiveDataService {

    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(ArchiveDataService.class);
    
    private EntityManager em;

    public ArchiveDataService(EntityManager em) {
        this.em = em;
    }

    /**
     * Entity Bean holding info about a archive data.
     */
    public String create(int type, int signerId, String archiveid, X509Certificate clientCert,
            String requestIP, ArchiveData archiveData) {
        String uniqueId = type + ";" + signerId + ";" + archiveid;
        if (LOG.isDebugEnabled()) {
            LOG.debug("Creating archive data, uniqueId=" + uniqueId);
        }
        ArchiveDataBean adb = new ArchiveDataBean();
        adb.setUniqueId(uniqueId);
        adb.setType(type);
        adb.setSignerid(signerId);
        adb.setTime(new Date().getTime());
        adb.setArchiveid(archiveid);
        if (clientCert != null) {
            adb.setRequestIssuerDN(CertTools.getIssuerDN(clientCert));
            adb.setRequestCertSerialnumber(clientCert.getSerialNumber().toString(16));
        }
        adb.setRequestIP(requestIP);
        adb.setArchiveDataObject(archiveData);
        adb.setDataEncoding(ArchiveDataBean.DATA_ENCODING_XML);

        em.persist(adb);
        return uniqueId;
    }
    
    public String create(int type, int signerId, String archiveid, X509Certificate clientCert,
            String requestIP, String archiveData) {
        String uniqueId = type + ";" + signerId + ";" + archiveid;
        if (LOG.isDebugEnabled()) {
            LOG.debug("Creating archive data, uniqueId=" + uniqueId);
        }
        ArchiveDataBean adb = new ArchiveDataBean();
        adb.setUniqueId(uniqueId);
        adb.setType(type);
        adb.setSignerid(signerId);
        adb.setTime(new Date().getTime());
        adb.setArchiveid(archiveid);
        if (clientCert != null) {
            adb.setRequestIssuerDN(CertTools.getIssuerDN(clientCert));
            adb.setRequestCertSerialnumber(clientCert.getSerialNumber().toString(16));
        }
        adb.setRequestIP(requestIP);
        adb.setArchiveData(archiveData);
        adb.setDataEncoding(ArchiveDataBean.DATA_ENCODING_BASE64);
 
        em.persist(adb);
        return uniqueId;
    }

    /**
     * Method finding a AchiveData given its unique Id.
     */
    public ArchiveDataBean findByArchiveId(int type, int signerid, String archiveid) {
        try {
            return (ArchiveDataBean) em.createNamedQuery("ArchiveDataBean.findByArchiveId").setParameter(1, type).setParameter(2, signerid).setParameter(3, archiveid).getSingleResult();
        } catch (NoResultException ignored) {} // NOPMD
        return null;
    }
    
    /**
     * Method finding all ArchiveDataBeans given the archiveId.
     */
    public List findAllByArchiveId(final int signerid, final String archiveid) {
        try {
            return em.createNamedQuery("ArchiveDataBean.findAllByArchiveId").setParameter(1, signerid).setParameter(2, archiveid).getResultList();
        } catch (NoResultException e) {
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    public Collection<ArchiveDataBean> findByTime(int type, int signerid, long starttime, long endtime) {
        try {
            return em.createNamedQuery("ArchiveDataBean.findByTime").setParameter(1, type).setParameter(2, signerid).setParameter(3, starttime).setParameter(4, endtime).getResultList();
        } catch (NoResultException ignored) {} // NOPMD
        return new ArrayList<ArchiveDataBean>();
    }

    @SuppressWarnings("unchecked")
    public Collection<ArchiveDataBean> findByRequestCertificate(int type, int signerid, String requestIssuerDN, String requestCertSerialnumber) {
        try {
            return em.createNamedQuery("ArchiveDataBean.findByRequestCertificate").setParameter(1, type).setParameter(2, signerid).setParameter(3, requestIssuerDN).setParameter(4, requestCertSerialnumber).getResultList();
        } catch (NoResultException ignored) {} // NOPMD
        return new ArrayList<ArchiveDataBean>();
    }
    
    @SuppressWarnings("unchecked")
    public Collection<ArchiveDataBean> findAllByRequestCertificate(final int signerid, final String requestIssuerDN, final String requestCertSerialnumber) {
        try {
            return em.createNamedQuery("ArchiveDataBean.findAllByRequestCertificate").setParameter(1, signerid).setParameter(2, requestIssuerDN).setParameter(3, requestCertSerialnumber).getResultList();
        } catch (NoResultException ignored) {} // NOPMD
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public Collection<ArchiveDataBean> findByRequestCertificateAndTime(int type, int signerid, String requestIssuerDN, String requestCertSerialnumber, long starttime, long endtime) {
        try {
            return em.createNamedQuery("ArchiveDataBean.findByRequestCertificateAndTime").setParameter(1, type).setParameter(2, signerid).setParameter(3, requestIssuerDN).setParameter(4, requestCertSerialnumber).setParameter(5, starttime).setParameter(6, endtime).getResultList();
        } catch (NoResultException ignored) {} // NOPMD
        return new ArrayList<ArchiveDataBean>();
    }

    @SuppressWarnings("unchecked")
    public Collection<ArchiveDataBean> findByRequestIP(int type, int signerid, String requestIP) {
        try {
            return em.createNamedQuery("ArchiveDataBean.findByRequestIP").setParameter(1, type).setParameter(2, signerid).setParameter(3, requestIP).getResultList();
        } catch (NoResultException ignored) {} // NOPMD
        return new ArrayList<ArchiveDataBean>();
    }
    
    @SuppressWarnings("unchecked")
    public Collection<ArchiveDataBean> findAllByRequestIP(final int signerId, final String requestIP) {
        try {
            return em.createNamedQuery("ArchiveDataBean.findAllByRequestIP").setParameter(1, signerId).setParameter(2, requestIP).getResultList();
        } catch (NoResultException ignored) {} // NOPMD
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public Collection<ArchiveDataBean> findByRequestIPAndTime(int type, int signerid, String requestIP, long starttime, long endtime) {
        try {
            return em.createNamedQuery("ArchiveDataBean.findByRequestCertificateAndTime").setParameter(1, type).setParameter(2, signerid).setParameter(3, requestIP).setParameter(4, starttime).setParameter(5, endtime).getResultList();
        } catch (NoResultException ignored) {} // NOPMD
        return new ArrayList<ArchiveDataBean>();
    }
    
    @SuppressWarnings("unchecked")
    public Collection<ArchiveMetadata> findAllWithUniqueIdInList(Collection<String> uniqueIds,
        boolean includeData) {
        try {
            final List<ArchiveMetadata> result = new ArrayList<ArchiveMetadata>();
            final List<ArchiveDataBean> archiveDatas =
                    em.createNamedQuery("ArchiveDataBean.findAllWithUniqueIds").
                    setParameter("ids", uniqueIds).getResultList();
            
            for (final ArchiveDataBean archiveData : archiveDatas) {
                result.add(new ArchiveMetadata(archiveData.getType(),
                                               archiveData.getSignerid(),
                                               archiveData.getUniqueId(),
                                               archiveData.getArchiveid(),
                                               new Date(archiveData.getTime()),
                                               archiveData.getRequestIssuerDN(),
                                               archiveData.getRequestCertSerialnumber(),
                                               archiveData.getRequestIP(),
                                               includeData ?
                                                archiveData.getArchiveDataVO().getArchivedBytes() :
                                                null));
            }
            
            return result;
        } catch (NoResultException ignored) {} // NOPMD
        return new ArrayList<ArchiveMetadata>();
    }

    @SuppressWarnings("unchecked")
    public Collection<ArchiveMetadata> findMatchingCriteria(int startIndex, int max,
            QueryCriteria criteria, boolean includeData) {
        
        try {
            final QueryGenerator generator = QueryGenerator.generator(ArchiveDataBean.class, criteria, "a");
            final String conditions = generator.generate();
            
            final Query query =
                    em.createQuery("SELECT a FROM ArchiveDataBean a" + conditions);
            
            for (final String key : generator.getParameterKeys()) {
                final Object param = generator.getParameterValue(key);
                query.setParameter(key, param);
            }
            
            if (startIndex > 0) {
                query.setFirstResult(startIndex);
            }
            
            if (max > 0) {
                query.setMaxResults(max);
            }
            
            final List<ArchiveDataBean> queryResults = query.getResultList();
            final Collection<ArchiveMetadata> result = new LinkedList<ArchiveMetadata>();
            
            for (final ArchiveDataBean bean : queryResults) {
                final ArchiveMetadata metadata =
                    new ArchiveMetadata(bean.getType(), bean.getSignerid(),
                                        bean.getUniqueId(), bean.getArchiveid(),
                                        new Date(bean.getTime()), bean.getRequestIssuerDN(),
                                        bean.getRequestCertSerialnumber(),
                                        bean.getRequestIP(),
                                        includeData ?
                                            bean.getArchiveDataVO().getArchivedBytes() :
                                            null);
                result.add(metadata);
            }
            
            return result;
            
        } catch (NoResultException ignored) { // NOPMD
            // ignored
        }
        
        return Collections.emptyList();
    }

}
