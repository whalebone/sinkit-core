package biz.karms.sinkit.ejb;

import biz.karms.sinkit.eventlog.EventDNSRequest;
import biz.karms.sinkit.eventlog.EventLogAction;
import biz.karms.sinkit.eventlog.EventLogRecord;
import biz.karms.sinkit.eventlog.EventReason;
import biz.karms.sinkit.exception.ArchiveException;
import biz.karms.sinkit.exception.IoCSourceIdException;
import biz.karms.sinkit.ioc.IoCRecord;
import biz.karms.sinkit.ioc.IoCSeen;
import biz.karms.sinkit.ioc.IoCSourceId;
import biz.karms.sinkit.ioc.IoCSourceIdType;
import biz.karms.sinkit.ioc.util.IoCSourceIdBuilder;

import javax.ejb.Schedule;
import javax.ejb.Schedules;
import javax.ejb.Singleton;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
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

    public synchronized IoCRecord processIoCRecord(IoCRecord receivedIoc) throws ArchiveException, IoCSourceIdException {

        if (receivedIoc.getTime().getSource() != null) {
            Date sourceTime = receivedIoc.getTime().getSource();
            if (this.addWindow(sourceTime).before(new Date())) {
                //log.info("Not processing too old IoC [" + recievedIoc + "]");
                return receivedIoc;
            }
        }

        IoCSourceId sid = IoCSourceIdBuilder.build(receivedIoc);
        receivedIoc.getSource().setId(sid);

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

    public EventLogRecord logEvent(
            EventLogAction action,
            String clientUid,
            String requestIp,
            String requestRaw,
            String reasonFqdn,
            String reasonIp,
            String[] matchedIoCs
    ) {
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

        archiveService.archiveEventLogRecord(logRecord);

        return logRecord;
    }


    private Date addWindow(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.HOUR, IOC_ACTIVE_HOURS);
        return c.getTime();
    }
}
