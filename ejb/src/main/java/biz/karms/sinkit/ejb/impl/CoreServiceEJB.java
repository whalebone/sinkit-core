package biz.karms.sinkit.ejb.impl;

import biz.karms.sinkit.ejb.ArchiveService;
import biz.karms.sinkit.ejb.BlacklistCacheService;
import biz.karms.sinkit.ejb.CacheBuilder;
import biz.karms.sinkit.ejb.CoreService;
import biz.karms.sinkit.ejb.WhitelistCacheService;
import biz.karms.sinkit.ejb.cache.pojo.WhitelistedRecord;
import biz.karms.sinkit.ejb.util.IoCValidator;
import biz.karms.sinkit.ejb.util.WhitelistUtils;
import biz.karms.sinkit.exception.ArchiveException;
import biz.karms.sinkit.exception.IoCValidationException;
import biz.karms.sinkit.ioc.IoCAccuCheckerReport;
import biz.karms.sinkit.ioc.IoCRecord;
import biz.karms.sinkit.ioc.IoCSeen;
import biz.karms.sinkit.ioc.IoCSourceId;
import biz.karms.sinkit.ioc.IoCSourceIdType;
import biz.karms.sinkit.ioc.util.IoCSourceIdBuilder;
import com.google.gson.Gson;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Tomas Kozel
 */
@Stateless
public class CoreServiceEJB implements CoreService {

    private static final String IOC_ACTIVE_HOURS_ENV = "SINKIT_IOC_ACTIVE_HOURS";
    private static final String WHITELIST_VALID_HOURS_ENV = "SINKIT_WHITELIST_VALID_HOURS";

    private int iocActiveHours;
    private long whitelistValidSeconds;

    @Inject
    private Logger log;

    @EJB
    private ArchiveService archiveService;

    @EJB
    private BlacklistCacheService blacklistCacheService;

    @EJB
    private WhitelistCacheService whitelistCacheService;

    @EJB
    private CacheBuilder cacheBuilder;

    @PostConstruct
    public void setup() {
        if (log == null || archiveService == null || blacklistCacheService == null || whitelistCacheService == null ||
                cacheBuilder == null) {
            throw new IllegalArgumentException("Logger, ArchiveServiceEJB, BlacklistCacheServiceEJB, " +
                    "WhitelistCacheServiceEJB, CacheBuilderEJB must be injected.");
        }

        try {
            iocActiveHours = Integer.parseInt(System.getenv(IOC_ACTIVE_HOURS_ENV));
        } catch (RuntimeException re) {
            throw new IllegalArgumentException("System env " + IOC_ACTIVE_HOURS_ENV + " is invalid: " + System.getenv(IOC_ACTIVE_HOURS_ENV));
        }

        if (iocActiveHours < 0) {
            throw new IllegalArgumentException("System env " + IOC_ACTIVE_HOURS_ENV + " is mandatory and must be integer bigger than zero.");
        }

        if (StringUtils.isBlank(System.getenv(WHITELIST_VALID_HOURS_ENV))) {
            throw new IllegalStateException("System property '" + WHITELIST_VALID_HOURS_ENV + "' is not set");
        }

        try {
            whitelistValidSeconds = Integer.parseInt(System.getenv(WHITELIST_VALID_HOURS_ENV)) * 3600;
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("System property '" + WHITELIST_VALID_HOURS_ENV + "' is not valid integer", ex);
        }
    }

    public int getIocActiveHours() {
        return iocActiveHours;
    }

    @Override
    public IoCRecord processIoCRecord(final IoCRecord ioc) throws ArchiveException, IoCValidationException {
        log.log(Level.FINE, "PROCESSING IOC: " + new Gson().toJson(ioc));

        // validate ioc
        IoCValidator.validateIoCRecord(ioc, iocActiveHours);

        // try to construct source ID
        ioc.getSource().setId(IoCSourceIdBuilder.build(ioc));

        ioc.setActive(true);
        Date seenFirst;
        Date seenLast;
        if (ioc.getTime().getSource() == null) {
            seenFirst = ioc.getTime().getObservation();
            seenLast = ioc.getTime().getObservation();
        } else {
            seenFirst = ioc.getTime().getSource();
            seenLast = ioc.getTime().getSource();
        }
        IoCSeen seen = new IoCSeen();
        seen.setLast(seenLast);
        seen.setFirst(seenFirst);
        ioc.setSeen(seen);
        Date receivedByCore = Calendar.getInstance().getTime();
        ioc.getTime().setReceivedByCore(receivedByCore);

        WhitelistedRecord white = null;
        if (ioc.getSource().getId().getType() == IoCSourceIdType.FQDN) {
            final String[] fqdns = WhitelistUtils.explodeDomains(ioc.getSource().getId().getValue());
            int i = 0;
            while (i < fqdns.length && white == null) {
                white = whitelistCacheService.get(fqdns[i++]);
            }
        } else {
            white = whitelistCacheService.get(ioc.getSource().getId().getValue());
        }
        if (white == null) {
            // Accuracy endeavour: Not on whitelist. Explicitly set to null.
            ioc.setWhitelistName(null);
            // ioc is inserted as is if does not exist or last.seen is updated
            // ioc has to be inserted before blacklist record, because it needs the documentId to be computed
            archiveService.archiveReceivedIoCRecord(ioc);
            blacklistCacheService.addToCache(ioc);
        } else {
            ioc.setWhitelistName(white.getSourceName());
            ioc.getTime().setWhitelisted(receivedByCore);
            archiveService.archiveReceivedIoCRecord(ioc);
            // Accuracy endeavour: Whitelisted, but we store it anyway.
            blacklistCacheService.addToCache(ioc);
        }

        return ioc;
    }

    /**
     * updates all entries in cache and in elastic with report
     *
     * @param report
     * @return true if all archivations and cache updates succeed
     * @throws ArchiveException
     * @throws IoCValidationException
     */
    @Override
    public boolean updateWithAccuCheckerReport(IoCAccuCheckerReport report) throws ArchiveException, IoCValidationException {

        if (report.getSource() == null || report.getSource().getId() == null || report.getSource().getId().getValue() == null) {
            throw new IoCValidationException("Accuchecker report doesn't have required field source.id.value.");
        }
        if (report.getAccuracy() == null) {
            throw new IoCValidationException("Accuchecker report doesn't have required field accuracy.");
        }
        boolean response = true;
        String source_id_value = report.getSource().getId().getValue();
        List<IoCRecord> iocs = archiveService.getMatchingActiveEntries("source.id.value", source_id_value);
        for (IoCRecord ioc : iocs) {

            ioc.updateWithAccuCheckerReport(report);

            archiveService.archiveReceivedIoCRecord(ioc);
            boolean cache_response = blacklistCacheService.addToCache(ioc);
            if (!cache_response) {
                response = false;
            }
        }
        return response;
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
            if (!CollectionUtils.isEmpty(iocs)) {
                for (IoCRecord ioc : iocs) {
                    blacklistCacheService.removeFromCache(ioc);
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

    @Override
    public boolean processWhitelistIoCRecord(final IoCRecord whiteIoC) throws IoCValidationException, ArchiveException {
        IoCValidator.validateWhitelistIoCRecord(whiteIoC);
        final IoCSourceId sid = IoCSourceIdBuilder.build(whiteIoC);
        whiteIoC.getSource().setId(sid);
        // TODO: remove when ttl is received from IntelMQ
        whiteIoC.getSource().setTtl(whitelistValidSeconds);
        WhitelistedRecord white = whitelistCacheService.get(whiteIoC.getSource().getId().getValue());
        boolean putToCacheBefore = true;
        if (white != null) {
            final Calendar expiresAt = Calendar.getInstance();
            expiresAt.add(Calendar.SECOND, whiteIoC.getSource().getTtl().intValue());
            // if old whitelist record needs update
            if (expiresAt.after(white.getExpiresAt())) {
                // if old whitelist record was completely processed during last run just update it and quit the process
                if (white.isCompleted()) {
                    if (whitelistCacheService.put(whiteIoC, true) == null) {
                        log.log(Level.SEVERE, "Cannot update whitelist record. Aborting process...");
                        return false;
                    } else {
                        return true;
                    }
                } else { // else the whitelisted record will be updated outside these 'ifs'
                    putToCacheBefore = true;
                }
            } else { // else old whitelist record doesn't need update
                // if old whitelist record was completely processed during last run and doesn't need update
                // then nothing else has to be done
                if (white.isCompleted()) {
                    return true;
                } else {
                    putToCacheBefore = false;
                }
            }
        }
        if (putToCacheBefore) {
            white = whitelistCacheService.put(whiteIoC, false);
            if (white == null) {
                log.log(Level.SEVERE, "Cannot put whitelist record to whitelist cache. Aborting process...");
                return false;
            }
        }
        final List<IoCRecord> iocs = archiveService.findIoCsForWhitelisting(white.getRawId());
        for (IoCRecord iocRecord : iocs) {
            if (!blacklistCacheService.removeWholeObjectFromCache(iocRecord)) {
                return false;
            }
            archiveService.setRecordWhitelisted(iocRecord, white.getSourceName());
        }
        return whitelistCacheService.setCompleted(white) != null;
    }

    @Override
    public WhitelistedRecord getWhitelistedRecord(final String id) {
        if (id == null) {
            log.log(Level.SEVERE, "getWhitelistedRecord: cannot search whitelist, id is null");
            return null;
        }
        return whitelistCacheService.get(id);
    }

    @Override
    public boolean removeWhitelistedRecord(final String id) {
        if (id == null) {
            log.log(Level.SEVERE, "removeWhitelistedRecord: cannot remove whitelist entry, id is null");
            return false;
        }
        return whitelistCacheService.remove(id);
    }

    @Override
    public boolean isWhitelistEmpty() {
        return whitelistCacheService.isWhitelistEmpty();
    }

    @Override
    public void setWhitelistValidSeconds(final long whitelistValidSeconds) {
        this.whitelistValidSeconds = whitelistValidSeconds;
    }


}
