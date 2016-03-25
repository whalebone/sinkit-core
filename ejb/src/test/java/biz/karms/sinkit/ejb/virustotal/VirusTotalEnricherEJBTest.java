package biz.karms.sinkit.ejb.virustotal;

import biz.karms.sinkit.ejb.ArchiveService;
import biz.karms.sinkit.ejb.virustotal.impl.VirusTotalEnricherEJB;
import biz.karms.sinkit.eventlog.EventLogRecord;
import biz.karms.sinkit.eventlog.EventReason;
import biz.karms.sinkit.eventlog.VirusTotalRequest;
import biz.karms.sinkit.eventlog.VirusTotalRequestStatus;
import biz.karms.sinkit.ioc.IoCRecord;
import biz.karms.sinkit.ioc.IoCVirusTotalReport;
import com.kanishka.virustotal.dto.FileScanReport;
import com.kanishka.virustotal.dto.VirusScanInfo;
import com.kanishka.virustotal.exception.InvalidArguentsException;
import com.kanishka.virustotal.exception.UnauthorizedAccessException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

/**
 * Created by Tomas Kozel
 */
@RunWith(MockitoJUnitRunner.class)
public class VirusTotalEnricherEJBTest {

    @Mock
    private ArchiveService archiveService;

    @Mock
    private VirusTotalService virusTotalService;

    @Mock
    private Logger log;

    @Captor
    ArgumentCaptor<EventLogRecord> logRecordCaptor;

    @Captor
    ArgumentCaptor<IoCVirusTotalReport[]> reportCaptor;

    @InjectMocks
    private VirusTotalEnricherEJB enricher;

    @Test
    public void testSuccessfulEnrichment() throws Exception {
        IoCRecord ioc = createIoCRecord("unique", null);
        EventLogRecord logRecord = createLogRecord("6.6.6.6", null, VirusTotalRequestStatus.WAITING, new String[]{"unique"});
        when(archiveService.getLogRecordWaitingForVTReport(2))
                .thenReturn(null)
                .thenReturn(logRecord)
                .thenReturn(null);
        when(archiveService.getLogRecordWaitingForVTScan(2))
                .thenReturn(logRecord)
                .thenReturn(null);
        when(archiveService.getIoCRecordByUniqueRef("unique")).thenReturn(ioc);
        enricher.doEnrichment();
        verify(archiveService, times(1)).getLogRecordWaitingForVTReport(2);
        verify(archiveService, times(2)).getLogRecordWaitingForVTScan(2);
        verify(archiveService, times(1)).getIoCRecordByUniqueRef("unique");
        verify(virusTotalService, times(1)).scanUrl("http://6.6.6.6/");
        verify(archiveService).archiveEventLogRecord(logRecordCaptor.capture());
        assertNotNull(logRecordCaptor.getValue());
        assertNotNull(logRecordCaptor.getValue().getVirusTotalRequest());
        assertEquals(VirusTotalRequestStatus.WAITING_FOR_REPORT, logRecordCaptor.getValue().getVirusTotalRequest().getStatus());

        when(virusTotalService.getUrlScanReport("http://6.6.6.6/")).thenReturn(createScanReport("now", "whalebone", "malware"));
        when(virusTotalService.parseDate("now")).thenReturn(new Date());
        enricher.doEnrichment();
        verify(archiveService, times(3)).getLogRecordWaitingForVTReport(2);
        verify(archiveService, times(3)).getLogRecordWaitingForVTScan(2);
        verify(virusTotalService).getUrlScanReport("http://6.6.6.6/");
        verify(virusTotalService).parseDate("now");
        verify(archiveService, times(2)).getIoCRecordByUniqueRef("unique");
        verify(archiveService, times(1)).setVirusTotalReportToIoCRecord(eq(ioc), reportCaptor.capture());
        assertNotNull(reportCaptor.getValue());
        assertEquals(1, reportCaptor.getValue().length);
        assertEquals("6.6.6.6", reportCaptor.getValue()[0].getIp());
        assertNotNull(reportCaptor.getValue()[0].getScanDate());
        assertNotNull(reportCaptor.getValue()[0].getUrlReport());
        assertNotNull(reportCaptor.getValue()[0].getUrlReport().getScans());
        assertNotNull(reportCaptor.getValue()[0].getUrlReport().getScans().get("whalebone"));
        assertEquals("malware", reportCaptor.getValue()[0].getUrlReport().getScans().get("whalebone").getResult());
        assertTrue(reportCaptor.getValue()[0].getUrlReport().getScans().get("whalebone").isDetected());
        verify(archiveService, times(2)).archiveEventLogRecord(logRecordCaptor.capture());
        assertNotNull(logRecordCaptor.getValue());
        assertNotNull(logRecordCaptor.getValue().getVirusTotalRequest());
        assertEquals(VirusTotalRequestStatus.FINISHED, logRecordCaptor.getValue().getVirusTotalRequest().getStatus());
        verifyNoMoreInteractions(archiveService, virusTotalService);
    }

    @Test
    public void testFailedScan() throws Exception {
        IoCRecord ioc = createIoCRecord("unique", null);
        EventLogRecord logRecord = createLogRecord(null, "whalebone.io", VirusTotalRequestStatus.WAITING, new String[]{"unique"});
        when(archiveService.getLogRecordWaitingForVTScan(2))
                .thenReturn(logRecord)  // first try
                .thenReturn(null)
                .thenReturn(logRecord)  // second try
                .thenReturn(null)
                .thenReturn(logRecord)  // third try
                .thenReturn(null);
        when(virusTotalService.scanUrl("http://whalebone.io/"))
                .thenThrow(new IOException("Connection failed"))                // first fail
                .thenThrow(new UnauthorizedAccessException("Wrong API key"))    // second fail
                .thenThrow(new InvalidArguentsException("Muhaha"));             // third fail
        when(archiveService.getIoCRecordByUniqueRef("unique")).thenReturn(ioc);

        // first try
        Date before = Calendar.getInstance().getTime();
        Thread.sleep(1);
        enricher.doEnrichment();
        verify(archiveService).archiveEventLogRecord(logRecordCaptor.capture());
        assertNotNull(logRecordCaptor.getValue());
        assertNotNull(logRecordCaptor.getValue().getVirusTotalRequest());
        assertEquals(VirusTotalRequestStatus.WAITING, logRecordCaptor.getValue().getVirusTotalRequest().getStatus());
        assertEquals("java.io.IOException: Connection failed", logRecordCaptor.getValue().getVirusTotalRequest().getCauseOfFailure());
        assertEquals(new Integer(1), logRecordCaptor.getValue().getVirusTotalRequest().getFailedAttempts());
        assertNotNull(logRecordCaptor.getValue().getVirusTotalRequest().getFailed());
        assertTrue(before.before(logRecordCaptor.getValue().getVirusTotalRequest().getFailed()));

        // second try
        before = Calendar.getInstance().getTime();
        Thread.sleep(1);
        enricher.doEnrichment();
        verify(archiveService, times(2)).archiveEventLogRecord(logRecordCaptor.capture());
        assertNotNull(logRecordCaptor.getValue());
        assertNotNull(logRecordCaptor.getValue().getVirusTotalRequest());
        assertEquals(VirusTotalRequestStatus.WAITING, logRecordCaptor.getValue().getVirusTotalRequest().getStatus());
        assertEquals("com.kanishka.virustotal.exception.UnauthorizedAccessException: Wrong API key",
                logRecordCaptor.getValue().getVirusTotalRequest().getCauseOfFailure());
        assertEquals(new Integer(2), logRecordCaptor.getValue().getVirusTotalRequest().getFailedAttempts());
        assertNotNull(logRecordCaptor.getValue().getVirusTotalRequest().getFailed());
        assertTrue(before.before(logRecordCaptor.getValue().getVirusTotalRequest().getFailed()));

        // third try
        before = Calendar.getInstance().getTime();
        Thread.sleep(1);
        enricher.doEnrichment();
        verify(archiveService, times(3)).archiveEventLogRecord(logRecordCaptor.capture());
        assertNotNull(logRecordCaptor.getValue());
        assertNotNull(logRecordCaptor.getValue().getVirusTotalRequest());
        assertEquals(VirusTotalRequestStatus.FAILED, logRecordCaptor.getValue().getVirusTotalRequest().getStatus());
        assertEquals("com.kanishka.virustotal.exception.InvalidArguentsException: Muhaha",
                logRecordCaptor.getValue().getVirusTotalRequest().getCauseOfFailure());
        assertEquals(new Integer(3), logRecordCaptor.getValue().getVirusTotalRequest().getFailedAttempts());
        assertNotNull(logRecordCaptor.getValue().getVirusTotalRequest().getFailed());
        assertTrue(before.before(logRecordCaptor.getValue().getVirusTotalRequest().getFailed()));
    }

    @Test
    public void testFailedReport() throws Exception {
        IoCRecord ioc = createIoCRecord("unique", null);
        EventLogRecord logRecord = createLogRecord(null, "whalebone.io", VirusTotalRequestStatus.WAITING_FOR_REPORT, new String[]{"unique"});
        when(archiveService.getLogRecordWaitingForVTReport(2))
                .thenReturn(logRecord)  // first try
                .thenReturn(null)
                .thenReturn(logRecord)  // second try
                .thenReturn(null)
                .thenReturn(logRecord)  // third try
                .thenReturn(null);
        when(virusTotalService.getUrlScanReport("http://whalebone.io/"))
                .thenThrow(new IOException("Connection failed"))                // first fail
                .thenReturn(createScanReport(null, "whalebone", "virus"))       // second fail (null scan date)
                .thenThrow(new InvalidArguentsException("Muhaha"));             // third fail
        when(archiveService.getIoCRecordByUniqueRef("unique")).thenReturn(ioc);

        // first try
        Date before = Calendar.getInstance().getTime();
        Thread.sleep(1);
        enricher.doEnrichment();
        verify(archiveService).archiveEventLogRecord(logRecordCaptor.capture());
        assertNotNull(logRecordCaptor.getValue());
        assertNotNull(logRecordCaptor.getValue().getVirusTotalRequest());
        assertEquals(VirusTotalRequestStatus.WAITING_FOR_REPORT, logRecordCaptor.getValue().getVirusTotalRequest().getStatus());
        assertEquals("java.io.IOException: Connection failed", logRecordCaptor.getValue().getVirusTotalRequest().getCauseOfFailure());
        assertEquals(new Integer(1), logRecordCaptor.getValue().getVirusTotalRequest().getFailedAttempts());
        assertNotNull(logRecordCaptor.getValue().getVirusTotalRequest().getFailed());
        assertTrue(before.before(logRecordCaptor.getValue().getVirusTotalRequest().getFailed()));

        // second try
        before = Calendar.getInstance().getTime();
        Thread.sleep(1);
        enricher.doEnrichment();
        verify(archiveService, times(2)).archiveEventLogRecord(logRecordCaptor.capture());
        assertNotNull(logRecordCaptor.getValue());
        assertNotNull(logRecordCaptor.getValue().getVirusTotalRequest());
        assertEquals(VirusTotalRequestStatus.WAITING_FOR_REPORT, logRecordCaptor.getValue().getVirusTotalRequest().getStatus());
        assertEquals("Receiving scan report failed: received scan date is null, something is really wrong",
                logRecordCaptor.getValue().getVirusTotalRequest().getCauseOfFailure());
        assertEquals(new Integer(2), logRecordCaptor.getValue().getVirusTotalRequest().getFailedAttempts());
        assertNotNull(logRecordCaptor.getValue().getVirusTotalRequest().getFailed());
        assertTrue(before.before(logRecordCaptor.getValue().getVirusTotalRequest().getFailed()));

        // third try
        before = Calendar.getInstance().getTime();
        Thread.sleep(1);
        enricher.doEnrichment();
        verify(archiveService, times(3)).archiveEventLogRecord(logRecordCaptor.capture());
        assertNotNull(logRecordCaptor.getValue());
        assertNotNull(logRecordCaptor.getValue().getVirusTotalRequest());
        assertEquals(VirusTotalRequestStatus.FAILED, logRecordCaptor.getValue().getVirusTotalRequest().getStatus());
        assertEquals("com.kanishka.virustotal.exception.InvalidArguentsException: Muhaha",
                logRecordCaptor.getValue().getVirusTotalRequest().getCauseOfFailure());
        assertEquals(new Integer(3), logRecordCaptor.getValue().getVirusTotalRequest().getFailedAttempts());
        assertNotNull(logRecordCaptor.getValue().getVirusTotalRequest().getFailed());
        assertTrue(before.before(logRecordCaptor.getValue().getVirusTotalRequest().getFailed()));
    }

    private EventLogRecord createLogRecord(String ip, String fqdn, VirusTotalRequestStatus status, String[] matchedIoCUniqueRefs) {
        EventLogRecord request = new EventLogRecord();
        request.setReason(new EventReason());
        request.getReason().setIp(ip);
        request.getReason().setFqdn(fqdn);
        request.setVirusTotalRequest(new VirusTotalRequest());
        request.getVirusTotalRequest().setStatus(status);
        IoCRecord[] matchedIoCs = new IoCRecord[matchedIoCUniqueRefs.length];
        for (int i = 0; i < matchedIoCs.length; i++) {
            matchedIoCs[i] = new IoCRecord();
            matchedIoCs[i].setUniqueRef(matchedIoCUniqueRefs[i]);
        }
        request.setMatchedIocs(matchedIoCs);
        return request;
    }

    private IoCRecord createIoCRecord(String uniqueRef, IoCVirusTotalReport[] reports) {
        IoCRecord ioc = new IoCRecord();
        ioc.setUniqueRef(uniqueRef);
        ioc.setVirusTotalReports(reports);
        return ioc;
    }

    private FileScanReport createScanReport(String scanDate, String scanner, String result) {
        FileScanReport report = new FileScanReport();
        report.setScanDate(scanDate);
        VirusScanInfo scanInfo = new VirusScanInfo();
        scanInfo.setDetected(true);
        scanInfo.setResult(result);
        report.setScans(new HashMap<>());
        report.getScans().put(scanner, scanInfo);
        return report;
    }
}
