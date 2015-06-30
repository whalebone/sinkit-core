package biz.karms.sinkit.ejb;

import biz.karms.sinkit.exception.ArchiveException;
import biz.karms.sinkit.ioc.IoCRecord;

import javax.ejb.*;
import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Created by tkozel on 29.6.15.
 */
@Singleton
public class IoCDeactivatorEJB {

    private AtomicBoolean busy = new AtomicBoolean(false);

    @Inject
    private ArchiveServiceEJB archiveService;

    @Inject
    private ServiceEJB cacheService;

    @Inject
    private Logger log;

    @Lock(LockType.READ)
    public void run() throws InterruptedException, ArchiveException {

        if (!busy.compareAndSet(false, true)) {
            log.info("Deactivation still in progress -> skipping next run");
            return;
        }

        try {
            this.deactivateIocs();
        } finally {
            busy.set(false);
        }
    }

    private int deactivateIocs() throws ArchiveException {

        log.info("Deactivation job started");

        int deactivated = 0;
        List<IoCRecord> iocs;

        // due to archiveService.findIoCsForDeactivation has limit for single search (i.e. max 1000 records)
        // this has to be done in multiple runs until search returns 0 results
        do {

            iocs = archiveService.findIoCsForDeactivation(CoreServiceEJB.IOC_ACTIVE_HOURS);

            if (!iocs.isEmpty()) {

                for (IoCRecord ioc : iocs) {
                    archiveService.deactivateRecord(ioc);
                    cacheService.removeFromCache(ioc);
                }
                deactivated += iocs.size();
            }
        } while (iocs.size() > 0);

        if (deactivated == 0)
            log.info("No IoCs for deactivation found. Ending job...");
        else
            log.info("IoCs deactivated: " + deactivated);

        return iocs.size();
    }

}
