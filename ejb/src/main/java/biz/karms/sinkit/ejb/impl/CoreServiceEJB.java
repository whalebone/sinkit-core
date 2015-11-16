package biz.karms.sinkit.ejb.impl;

import biz.karms.sinkit.ejb.ArchiveService;
import biz.karms.sinkit.ejb.CacheBuilder;
import biz.karms.sinkit.ejb.CacheService;
import biz.karms.sinkit.ejb.CoreService;
import biz.karms.sinkit.ejb.util.IoCValidator;
import biz.karms.sinkit.exception.ArchiveException;
import biz.karms.sinkit.exception.IoCValidationException;
import biz.karms.sinkit.ioc.IoCRecord;
import biz.karms.sinkit.ioc.IoCSeen;
import biz.karms.sinkit.ioc.IoCSourceId;
import biz.karms.sinkit.ioc.IoCSourceIdType;
import biz.karms.sinkit.ioc.util.IoCSourceIdBuilder;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by tkozel on 25.6.15.
 */
@Stateless
public class CoreServiceEJB implements CoreService {

    //public static final int IOC_ACTIVE_HOURS = Integer.parseInt(System.getenv("SINKIT_IOC_ACTIVE_HOURS"));
    private static final String IOC_ACTIVE_HOURS_ENV = "SINKIT_IOC_ACTIVE_HOURS";
    private int iocActiveHours;

    @Inject
    private Logger log;

    @EJB
    private ArchiveService archiveService;

    @EJB
    private CacheService cacheService;

    @EJB
    private CacheBuilder cacheBuilder;

    @PostConstruct
    public void setup() {
        if (log == null || archiveService == null || cacheService == null || cacheBuilder == null) {
            throw new IllegalArgumentException("Logger, ArchiveServiceEJB, CacheServiceEJB, CacheBuilderEJB must be injected.");
        }

        try {
            iocActiveHours = Integer.parseInt(System.getenv(IOC_ACTIVE_HOURS_ENV));
        } catch (RuntimeException re) {
            throw new IllegalArgumentException("System env " + IOC_ACTIVE_HOURS_ENV + " is invalid: " + System.getenv(IOC_ACTIVE_HOURS_ENV));
        }

        if (iocActiveHours < 0) {
            throw new IllegalArgumentException("System env " + IOC_ACTIVE_HOURS_ENV + " is mandatory and must be integer bigger than zero.");
        }
    }

    public int getIocActiveHours() {
        return iocActiveHours;
    }

    @Override
    public IoCRecord processIoCRecord(IoCRecord ioc) throws ArchiveException, IoCValidationException {
        // validate ioc
        IoCValidator.validateIoCRecord(ioc, iocActiveHours);

        // try to construct source ID
        IoCSourceId sid = IoCSourceIdBuilder.build(ioc);
        ioc.getSource().setId(sid);

        ioc.setActive(true);

        Date seenFirst;
        Date seenLast;
        if (ioc.getTime().getSource() == null) {
            seenFirst = ioc.getTime().getObservation();
            seenLast = ioc.getTime().getObservation();
        } else {
            seenFirst = ioc.getTime().getSource();
            seenLast =  ioc.getTime().getSource();
        }
        IoCSeen seen = new IoCSeen();
        seen.setLast(seenLast);
        seen.setFirst(seenFirst);
        ioc.setSeen(seen);
        ioc.getTime().setReceivedByCore(Calendar.getInstance().getTime());

        // ioc is inserted as is if does not exist or last.seen is updated
        archiveService.archiveReceivedIoCRecord(ioc);

        if (ioc.getSource().getId().getType() == IoCSourceIdType.FQDN || ioc.getSource().getId().getType() == IoCSourceIdType.IP) {
            cacheService.addToCache(ioc);
        }

        return ioc;
    }

    @Override
    public int deactivateIocs() throws ArchiveException {
        log.info("Deactivation job started");
        int deactivated = 0;
        List<IoCRecord> iocs;

        // due to archiveService.findIoCsForDeactivation has limit for single search (i.e. max 1000 records)
        // this has to be done in multiple runs until search returns 0 results
        do {
            iocs = archiveService.findIoCsForDeactivation(iocActiveHours);
            if (!iocs.isEmpty()) {
                for (IoCRecord ioc : iocs) {
                    cacheService.removeFromCache(ioc);
                    archiveService.deactivateRecord(ioc);
                }
                deactivated += iocs.size();
            }
        } while (iocs.size() > 0);

        if (deactivated == 0) {
            log.info("No IoCs for deactivation found. Ending job...");
        } else {
            log.info("IoCs deactivated: " + deactivated);
        }
        return deactivated;
    }

    @Override
    public boolean runCacheRebuilding() {

        if (cacheBuilder.isCacheRebuildRunning()) {
            log.info("Cache rebuilding still in process -> skipping");
            return false;
        }

        cacheBuilder.runCacheRebuilding();
        return true;
    }

    @Override
    public void enrich() {
        throw new UnsupportedOperationException("VirusTotal enricher is handled by Clustered HA Singleton Timer Service. This API call is currently disabled.");
    }
}
