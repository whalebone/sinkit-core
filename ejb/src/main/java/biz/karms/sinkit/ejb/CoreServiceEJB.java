package biz.karms.sinkit.ejb;

import biz.karms.sinkit.ejb.virustotal.VirusTotalEnricherEJB;
import biz.karms.sinkit.eventlog.*;
import biz.karms.sinkit.exception.ArchiveException;
import biz.karms.sinkit.exception.IoCValidationException;
import biz.karms.sinkit.ioc.*;
import biz.karms.sinkit.ioc.util.IoCSourceIdBuilder;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Singleton;
import javax.inject.Inject;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Future;
import java.util.logging.Logger;

/**
 * Created by tkozel on 25.6.15.
 */
@Singleton
public class CoreServiceEJB {

    public static final int IOC_ACTIVE_HOURS = 72;

    @Inject
    private Logger log;

    @Inject
    private ArchiveServiceEJB archiveService;

    @Inject
    private ServiceEJB cacheService;

    @Inject
    private CacheBuilderEJB cacheBuilder;

    @Inject
    private VirusTotalEnricherEJB virusTotal;

    public synchronized IoCRecord processIoCRecord(IoCRecord receivedIoc)
            throws ArchiveException, IoCValidationException {

        // validate ioc
        this.validateIoCRecord(receivedIoc);

        // try to construct source ID
        IoCSourceId sid = IoCSourceIdBuilder.build(receivedIoc);
        receivedIoc.getSource().setId(sid);

        if (receivedIoc.getTime().getSource() != null) {
            Date sourceTime = receivedIoc.getTime().getSource();
            if (this.addWindow(sourceTime).before(new Date())) {
                //log.info("Not processing too old IoC [" + recievedIoc + "]");
                return receivedIoc;
            }
        }

        IoCRecord ioc = archiveService.findActiveIoCRecordBySourceId(
                receivedIoc.getSource().getId().getValue(),
                receivedIoc.getClassification().getType(),
                receivedIoc.getFeed().getName());

        //not found in archive
        if (ioc == null) {
            ioc = receivedIoc;
            ioc.setActive(true);

            IoCSeen seen = new IoCSeen();

            seen.setFirst(ioc.getTime().getObservation());

            // if ioc record does not provide timestamp of source
            if (ioc.getTime().getSource() == null) {
                seen.setLast(ioc.getTime().getObservation());
            } else { // else add the defined window to time.source and set it as last seen
                seen.setLast(this.addWindow(ioc.getTime().getSource()));
            }
            ioc.setSeen(seen);
            ioc = archiveService.archiveIoCRecord(ioc);

            if (ioc.getSource().getId().getType() == IoCSourceIdType.FQDN || ioc.getSource().getId().getType() == IoCSourceIdType.IP)
                cacheService.addToCache(ioc);

        } else {

            if (receivedIoc.getTime().getSource() == null) {
                ioc.getSeen().setLast(receivedIoc.getTime().getObservation());
            } else {
                Date lastSource = this.addWindow(receivedIoc.getTime().getSource());
                if (ioc.getSeen().getLast().before(lastSource) ) {
                    ioc.getSeen().setLast(lastSource);
                }
            }

            ioc = archiveService.archiveIoCRecord(ioc);
            //nothing to do in cache
        }
        return ioc;
    }

    @Asynchronous
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
        logRecord.setLogged(new Date());
        logRecord.setMatchedIocs(matchedIoCs);

        VirusTotalRequest vtReq = new VirusTotalRequest();
        vtReq.setStatus(VirusTotalRequestStatus.WAITING);
        logRecord.setVirusTotalRequest(vtReq);

        archiveService.archiveEventLogRecord(logRecord);

        return new AsyncResult<>(logRecord);
    }

    private IoCRecord validateIoCRecord(IoCRecord ioc) throws IoCValidationException {

        if (ioc.getFeed() == null || ioc.getFeed().getName() == null) {
            throw new IoCValidationException("IoC record doesn't have mandatory field 'feed.name'");
        }
        if (ioc.getSource() == null || (
                ioc.getSource().getFQDN() == null && ioc.getSource().getIp() == null && ioc.getSource().getUrl() == null
        )) {
            throw new IoCValidationException("IoC can't have all IP and Domain and URL set as null");
        }
        if (ioc.getClassification() == null || ioc.getClassification().getType() == null) {
            throw new IoCValidationException("IoC record doesn't have mandatory field 'classification.type'");
        }
        if (ioc.getTime() == null || ioc.getTime().getObservation() == null) {
            throw new IoCValidationException("IoC record doesn't have mandatory field 'time.observation'");
        }

        return ioc;
    }

    public synchronized boolean runCacheRebuilding() {

        if (cacheBuilder.isCacheRebuildRunning()) {
            log.info("Cache rebuilding still in process -> skipping");
            return false;
        }

        cacheBuilder.runCacheRebuilding();
        return true;
    }

    public void enrich() {
        virusTotal.runEnrichmentProcess();
    }

    private Date addWindow(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.HOUR, IOC_ACTIVE_HOURS);
        return c.getTime();
    }
}
