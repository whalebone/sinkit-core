package biz.karms.sinkit.ejb;

import biz.karms.sinkit.eventlog.EventLogRecord;
import biz.karms.sinkit.exception.ArchiveException;
import biz.karms.sinkit.ioc.IoCRecord;
import biz.karms.sinkit.ioc.IoCVirusTotalReport;

import javax.ejb.Local;
import java.util.List;

/**
 * @author Michal Karm Babacek
 */
@Local
public interface ArchiveService {

    List<IoCRecord> findIoCsForDeactivation(int hours) throws ArchiveException;

    boolean archiveReceivedIoCRecord(IoCRecord ioc) throws ArchiveException;

    IoCRecord deactivateRecord(IoCRecord ioc) throws ArchiveException;

    EventLogRecord archiveEventLogRecord(EventLogRecord logRecord) throws ArchiveException;

    List<IoCRecord> getActiveIoCs(int from, int size) throws ArchiveException;

    IoCRecord getIoCRecordById(String id) throws ArchiveException;

    IoCRecord getIoCRecordByUniqueRef(String uniqueRef) throws ArchiveException;

    EventLogRecord getLogRecordWaitingForVTScan() throws ArchiveException;

    EventLogRecord getLogRecordWaitingForVTReport() throws ArchiveException;

    boolean setVirusTotalReportToIoCRecord(IoCRecord ioc, IoCVirusTotalReport[] reports) throws ArchiveException;
}
