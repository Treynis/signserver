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
package org.signserver.server.archive.olddbarchiver;

import java.security.cert.X509Certificate;
import javax.persistence.EntityManager;
import org.apache.log4j.Logger;
import org.signserver.common.ArchiveDataVO;
import org.signserver.common.RequestContext;
import org.signserver.common.WorkerConfig;
import org.signserver.server.SignServerContext;
import org.signserver.server.archive.Archivable;
import org.signserver.server.archive.ArchiveException;
import org.signserver.server.archive.Archiver;

/**
 * Archiver only accepting responses and currently only supports Archivables of
 * class ArchiveDataArchivable. 
 * 
 * Developers:
 * This class could be improved to support any Archivable if the
 * OldDatabaseArchiver should be able to be used with workers not returning
 * ArchiveData object any more.
 *
 * @author Markus Kilås
 * @version $Id$
 */
public class OldDatabaseArchiver implements Archiver {

    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(OldDatabaseArchiver.class);

    private ArchiveDataService dataService;

    @Override
    public void init(int listIndex, WorkerConfig config, SignServerContext context) {
        final EntityManager em = context.getEntityManager();
        if (em == null) {
            LOG.error("OldDatabaseArchiver requires a database connection");
        } else {
            dataService = new ArchiveDataService(em);
        }
    }

    @Override
    public boolean archive(Archivable archivable, RequestContext requestContext)
            throws ArchiveException {
        final boolean archived;
        if (Archivable.TYPE_RESPONSE.equals(archivable.getType())
                && archivable instanceof ArchiveDataArchivable) {
            if (dataService == null) {
                throw new ArchiveException("Could not archive as archiver was not successfully initialized");
            }
            final ArchiveDataArchivable ada = (ArchiveDataArchivable) archivable;
            final Integer workerId = (Integer) requestContext.get(RequestContext.WORKER_ID);
            final X509Certificate certificate = (X509Certificate) requestContext.get(RequestContext.CLIENT_CERTIFICATE);
            final String remoteIp = (String) requestContext.get(RequestContext.REMOTE_IP);

            dataService.create(ArchiveDataVO.TYPE_RESPONSE,
                            workerId,
                            ada.getArchiveId(),
                            certificate,
                            remoteIp,
                            ada.getArchiveData());
            archived = true;
        } else {
            archived = false;
        }
        return archived;
    }
}
