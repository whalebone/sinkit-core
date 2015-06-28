package biz.karms.sinkit.ejb;

import biz.karms.sinkit.exception.ArchiveException;
import biz.karms.sinkit.ioc.IoCRecord;
import biz.karms.sinkit.ioc.IoCSeen;

import javax.ejb.Schedule;
import javax.ejb.Schedules;
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
public class CoreServiceEJB {

    public static final int IOC_ACTIVE_HOURS = 72;

    @Inject
    private Logger log;

    @Inject
    private ArchiveServiceEJB archiveService;

    public IoCRecord processIoCRecord(IoCRecord recievedIoc) throws ArchiveException {

        IoCRecord ioc = null;
        if ( recievedIoc.getSource().getDomainName() != null ) {
            ioc = archiveService.findActiveIoCRecordByIp(
                    recievedIoc.getSource().getDomainName(),
                    recievedIoc.getClassification().getType(),
                    recievedIoc.getFeed().getName());
        } else {
            ioc = archiveService.findActiveIoCRecordByIp(
                    recievedIoc.getSource().getIp(),
                    recievedIoc.getClassification().getType(),
                    recievedIoc.getFeed().getName());
        }

        //not found in archive
        if (ioc == null) {
            ioc = recievedIoc;
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

            //put IoC to Cache as active
        } else {

            if (recievedIoc.getTime().getSource() == null) {
                ioc.getSeen().setLast(recievedIoc.getTime().getObservation());
            } else {
                Date lastSource = this.addWindow(recievedIoc.getTime().getSource());
                if (ioc.getSeen().getLast().before(lastSource) ) {
                    ioc.getSeen().setLast(lastSource);
                }
            }

            ioc = archiveService.archiveIoCRecord(ioc);
            //nothing to do in cache
        }
        return ioc;
    }

    @Schedules({
        @Schedule(hour = "*", minute = "*", second = "0"),
        @Schedule(hour = "*", minute = "*", second = "10"),
        @Schedule(hour = "*", minute = "*", second = "20"),
        @Schedule(hour = "*", minute = "*", second = "30"),
        @Schedule(hour = "*", minute = "*", second = "40"),
        @Schedule(hour = "*", minute = "*", second = "50")
    })
    //@Schedule (hour = "*")
    public int deactivateIocs() throws ArchiveException {

        log.info("Deactivation job started");
        List<IoCRecord> iocs = archiveService.findIoCsForDeactivation(IOC_ACTIVE_HOURS);

        if (iocs.isEmpty()) {
            log.info("No IoC for deactivation found. Ending job...");
            return 0;
        }

        for (IoCRecord ioc : iocs) {
            archiveService.deactivateRecord(ioc);
            //remove ioc from cache
        }

        return iocs.size();
    }

    private Date addWindow(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.HOUR, IOC_ACTIVE_HOURS);
        return c.getTime();
    }
}
