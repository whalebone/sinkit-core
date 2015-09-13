package biz.karms.sinkit.ejb.impl;

import biz.karms.sinkit.ejb.ArchiveService;
import biz.karms.sinkit.ejb.CacheBuilder;
import biz.karms.sinkit.ejb.CacheService;
import biz.karms.sinkit.ioc.IoCRecord;

import javax.ejb.*;
import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Created by tkozel on 26.7.15.
 * <p>
 * Maintains state
 */
@Singleton
@AccessTimeout(value = 5, unit = TimeUnit.MINUTES)
public class CacheBuilderEJB implements CacheBuilder {

    private AtomicBoolean cacheRebuilding = new AtomicBoolean(false);

    @Inject
    private Logger log;

    @EJB
    private ArchiveService archiveService;

    @EJB
    private CacheService cacheService;

    @Lock(LockType.READ)
    @Override
    public boolean isCacheRebuildRunning() {
        return cacheRebuilding.get();
    }

    @Asynchronous
    @Lock(LockType.READ)
    @Override
    public Future<Integer> runCacheRebuilding() throws ConcurrentAccessException {

        if (!cacheRebuilding.compareAndSet(false, true)) {
            log.info("Cache rebuilding still in process -> skipping");
            throw new ConcurrentAccessException("Cache rebuilding already in progress...");
        }

        try {
            return new AsyncResult<>(this.rebuildCache());
        } finally {
            cacheRebuilding.set(false);
        }
    }

    private int rebuildCache() {

        log.info("Rebuilding Cache started");

        // TODO: Is it O.K. that this could _take time_ ?
        cacheService.dropTheWholeCache();

        int recordsCount = 0;
        int from = 0;
        int size = 1000;

        List<IoCRecord> iocs;

        try {
            do {
                iocs = archiveService.getActiveIoCs(from, size);
                for (IoCRecord ioc : iocs) {
                    cacheService.addToCache(ioc);
                }
                recordsCount += iocs.size();
                from += size;
                log.info("Cache rebuilding: processing batch of " + iocs.size() + " iocs");

            } while (iocs.size() >= size);

        } catch (Exception ex) {
            log.severe("Cache rebuilding failed: " + ex.getMessage());
            ex.printStackTrace();
        }

        log.info("Cache rebuilding: rebuilding finished -  " + recordsCount + " iocs processed.");

        return recordsCount;
    }
}
