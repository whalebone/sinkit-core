package biz.karms.sinkit.ejb;

import biz.karms.sinkit.eventlog.EventLogAction;
import biz.karms.sinkit.eventlog.EventLogRecord;
import biz.karms.sinkit.exception.ArchiveException;
import biz.karms.sinkit.exception.IoCValidationException;
import biz.karms.sinkit.ioc.IoCRecord;

import javax.ejb.Local;
import java.util.concurrent.Future;

/**
 * @author Michal Karm Babacek
 */
@Local
public interface CoreService {
    IoCRecord processIoCRecord(IoCRecord receivedIoc) throws ArchiveException, IoCValidationException;

    int deactivateIocs() throws ArchiveException;

    Future<EventLogRecord> logEvent(
            EventLogAction action,
            String clientUid,
            String requestIp,
            String requestRaw,
            String reasonFqdn,
            String reasonIp,
            String[] matchedIoCs
    ) throws ArchiveException;

    boolean runCacheRebuilding();

    void enrich();
}
