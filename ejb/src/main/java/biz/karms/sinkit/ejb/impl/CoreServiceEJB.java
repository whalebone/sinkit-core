package biz.karms.sinkit.ejb.impl;

import biz.karms.sinkit.ejb.ArchiveService;
import biz.karms.sinkit.ejb.CacheBuilder;
import biz.karms.sinkit.ejb.CacheService;
import biz.karms.sinkit.ejb.CoreService;
import biz.karms.sinkit.ejb.util.IoCValidator;
import biz.karms.sinkit.eventlog.*;
import biz.karms.sinkit.exception.ArchiveException;
import biz.karms.sinkit.exception.IoCValidationException;
import biz.karms.sinkit.ioc.IoCRecord;
import biz.karms.sinkit.ioc.IoCSeen;
import biz.karms.sinkit.ioc.IoCSourceId;
import biz.karms.sinkit.ioc.IoCSourceIdType;
import biz.karms.sinkit.ioc.util.IoCSourceIdBuilder;

import javax.annotation.PostConstruct;
import javax.ejb.*;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Logger;

/**
 * Created by tkozel on 25.6.15.
 */
@Stateless
public class CoreServiceEJB implements CoreService {

    public static final int IOC_ACTIVE_HOURS = 72;

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
    }

    @Override
    public IoCRecord processIoCRecord(IoCRecord receivedIoc) throws ArchiveException, IoCValidationException {
        // validate ioc
        IoCValidator.validateIoCRecord(receivedIoc, IOC_ACTIVE_HOURS);

        // try to construct source ID
        IoCSourceId sid = IoCSourceIdBuilder.build(receivedIoc);
        receivedIoc.getSource().setId(sid);

        IoCRecord ioc = archiveService.findActiveIoCRecordBySourceId(
                receivedIoc.getSource().getId().getValue(),
                receivedIoc.getClassification().getType(),
                receivedIoc.getFeed().getName());

        //active not found in archive
        if (ioc == null) {
            ioc = receivedIoc;
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
        } else {
            //active already in archive

            Date seenLast;
            if (receivedIoc.getTime().getSource() == null) {
                seenLast = receivedIoc.getTime().getObservation();
            } else {
                seenLast = receivedIoc.getTime().getSource();
            }
            if (ioc.getSeen().getLast().before(seenLast) ) {
                ioc.getSeen().setLast(seenLast);
            }
        }

        ioc = archiveService.archiveIoCRecord(ioc);

        //always add to cache
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
            iocs = archiveService.findIoCsForDeactivation(CoreServiceEJB.IOC_ACTIVE_HOURS);
            if (!iocs.isEmpty()) {
                for (IoCRecord ioc : iocs) {
                    archiveService.deactivateRecord(ioc);
                    cacheService.removeFromCache(ioc);
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

    @Asynchronous
    @Override
    public Future<EventLogRecord> logEvent(
            EventLogAction action,
            String clientUid,
            String requestIp,
            String requestRaw,
            String reasonFqdn,
            String reasonIp,
            String[] matchedIoCs
    ) throws ArchiveException {
        EventLogRecord logRecord = new EventLogRecord();

        EventDNSRequest request = new EventDNSRequest();
        request.setIp(requestIp);
        request.setRaw(requestRaw);
        logRecord.setRequest(request);

        EventReason reason = new EventReason();
        reason.setIp(reasonIp);
        reason.setFqdn(reasonFqdn);
        logRecord.setReason(reason);

        logRecord.setAction(action);
        logRecord.setClient(clientUid);
        logRecord.setLogged(Calendar.getInstance().getTime());
        //logRecord.setMatchedIocs(matchedIoCs);

        List<MatchedIoC> matchedIoCsList = new ArrayList<>();
        for (String iocId : matchedIoCs) {
            IoCRecord ioc = archiveService.getIoCRecordById(iocId);
            if (ioc == null) {
                log.warning("Match IoC with id " + iocId + " was not found -> skipping.");
            }
            ioc.setVirusTotalReports(null);
            ioc.getSeen().setLast(null);
            ioc.setRaw(null);
            ioc.setActive(null);

            MatchedIoC matchedIoc = new MatchedIoC();
            matchedIoc.setDocumentId(iocId);
            matchedIoc.setIoc(ioc);
            matchedIoCsList.add(matchedIoc);
        }
        MatchedIoC[] matchedIoCsArray = matchedIoCsList.toArray(new MatchedIoC[matchedIoCsList.size()]);
        logRecord.setMatchedIocs(matchedIoCsArray);

        VirusTotalRequest vtReq = new VirusTotalRequest();
        vtReq.setStatus(VirusTotalRequestStatus.WAITING);
        logRecord.setVirusTotalRequest(vtReq);

        archiveService.archiveEventLogRecord(logRecord);

        return new AsyncResult<>(logRecord);
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
