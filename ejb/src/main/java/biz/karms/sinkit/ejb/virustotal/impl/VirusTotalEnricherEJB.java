package biz.karms.sinkit.ejb.virustotal.impl;

import biz.karms.sinkit.ejb.ArchiveService;
import biz.karms.sinkit.ejb.virustotal.VirusTotalEnricher;
import biz.karms.sinkit.ejb.virustotal.VirusTotalService;
import biz.karms.sinkit.ejb.virustotal.exception.VirusTotalException;
import biz.karms.sinkit.eventlog.EventLogRecord;
import biz.karms.sinkit.eventlog.VirusTotalRequestStatus;
import biz.karms.sinkit.exception.ArchiveException;
import biz.karms.sinkit.ioc.IoCRecord;
import biz.karms.sinkit.ioc.IoCVirusTotalReport;
import com.kanishka.virustotal.dto.FileScanReport;
import com.kanishka.virustotal.exception.InvalidArguentsException;
import com.kanishka.virustotal.exception.QuotaExceededException;
import com.kanishka.virustotal.exception.UnauthorizedAccessException;

import javax.annotation.Resource;
import javax.ejb.*;
import javax.ejb.Timer;
import javax.inject.Inject;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by tkozel on 31.7.15.
 */
//TODO
@Stateless
public class VirusTotalEnricherEJB implements VirusTotalEnricher {

    // we can call the Virus Total API only 4 times per minute
    private static final int SHOTS_PER_RUN = 4;

    @Inject
    private Logger log;

    @EJB
    private ArchiveService archiveService;

    @EJB
    private VirusTotalService virusTotalService;

    @Resource
    private TimerService timerService;

    @Override
    public void initialize(String info) {
        ScheduleExpression sexpr = new ScheduleExpression();
        // Every 40 seconds
        sexpr.hour("*").minute("*").second("0/40");
        timerService.createCalendarTimer(sexpr, new TimerConfig(info, false));
    }

    @Override
    public void stop() {
        log.info("Stop all existing VirusTotalEnricher HASingleton timers.");
        for (Timer timer : timerService.getTimers()) {
            log.fine("Stop VirusTotalEnricher HASingleton timer: " + timer.getInfo());
            timer.cancel();
        }
    }

    @Timeout
    public void scheduler(Timer timer) {
        log.info("VirusTotalEnricher HASingletonTimer: Info=" + timer.getInfo());
        if (Boolean.parseBoolean(System.getenv("SINKIT_VIRUS_TOTAL_SKIP"))) {
            return;
        }
        int availableVTCalls = SHOTS_PER_RUN;

        boolean reportRequestQueueEmpty = false;
        try {
            while (availableVTCalls > 0) {
                if (!reportRequestQueueEmpty) {
                    EventLogRecord reportRequest = archiveService.getLogRecordWaitingForVTReport();
                    if (reportRequest != null) {
                        availableVTCalls--;
                        processUrlScanReportRequest(reportRequest);
                        reportRequest.getVirusTotalRequest().setStatus(VirusTotalRequestStatus.FINISHED);
                        reportRequest.getVirusTotalRequest().setReportReceived(new Date());
                        archiveService.archiveEventLogRecord(reportRequest);
                    } else {
                        reportRequestQueueEmpty = true;
                    }
                } else {
                    EventLogRecord scanRequest = archiveService.getLogRecordWaitingForVTScan();
                    if (scanRequest != null) {

                        boolean needEnrichment = false;
                        availableVTCalls--;
                        // the API is called here
                        needEnrichment = processUrlScanRequest(scanRequest);

                        if (needEnrichment) {
                            scanRequest.getVirusTotalRequest().setStatus(VirusTotalRequestStatus.WAITING_FOR_REPORT);
                        } else {
                            scanRequest.getVirusTotalRequest().setStatus(VirusTotalRequestStatus.NOT_NEEDED);
                        }
                        scanRequest.getVirusTotalRequest().setProcessed(new Date());
                        archiveService.archiveEventLogRecord(scanRequest);
                    } else {
                        // there is no record for waiting for processing nor waiting for report -> need to break the cycle
                        break;
                    }
                }
            }
        } catch (QuotaExceededException e) {
            log.warning("VirusTutotal enrichment: quota exceeded before than expected -> skipping next runs in batch");
        } catch (ArchiveException | VirusTotalException e) {
            log.severe("Virus Total enrichment went wrong: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean processUrlScanRequest(final EventLogRecord enrichmentRequest) throws ArchiveException, QuotaExceededException, VirusTotalException {
        log.finest("Processing URL scan request: " + enrichmentRequest.toString());

        String fqdn = enrichmentRequest.getReason().getFqdn();
        boolean needEnrichment = false;

        iocsLoop:
        for (IoCRecord matchedIoC : enrichmentRequest.getMatchedIocs()) {

            String uniqueRef = matchedIoC.getUniqueRef();

            IoCRecord ioc = archiveService.getIoCRecordByUniqueRef(uniqueRef);
            if (ioc == null) {
                log.warning("VirusTotal: IoC with uniqueRef: " + uniqueRef + " does not exist -> skipping scan request.");
                continue;
            } else {
                log.finest("IoC found: " + ioc.toString());
            }

            if (ioc.getVirusTotalReports() == null || ioc.getVirusTotalReports().length == 0) {
                log.finest("IoC does not have any VT reports yet -> FQDN will be scanned");
                needEnrichment = true;
                break;
            } else {
                boolean fqdnFound = false;
                for (IoCVirusTotalReport report : ioc.getVirusTotalReports()) {
                    if (fqdn.equals(report.getFqdn())) {
                        log.finest("VT report for same FQDN found: " + report.toString());
                        fqdnFound = true;
                        // if fqdn was found in reports but the report is older than 24h we need enrichment
                        Calendar timeWindow = Calendar.getInstance();
                        timeWindow.add(Calendar.HOUR_OF_DAY, -24);

                        if (timeWindow.after(report.getScanDate())) {
                            log.finest("Report is too old -> FQDN will be scanned");
                            needEnrichment = true;
                            break iocsLoop;
                        }
                    }
                }

                // if fqdn was not found in reports we need enrichment
                if (!fqdnFound) {
                    needEnrichment = true;
                    log.finest("VT report with the same FQDN was not found -> FQDN will be scanned");
                    break;
                }
            }
        }

        if (needEnrichment) {
            try {
                virusTotalService.scanUrl("http://" + fqdn + "/");
            } catch (InvalidArguentsException | UnauthorizedAccessException | IOException e) {
                throw new VirusTotalException(e);
            }
        } else {
            log.finest("Enrichment is not needed.");
        }

        return needEnrichment;
    }

    private void processUrlScanReportRequest(final EventLogRecord enrichmentRequest) throws ArchiveException, QuotaExceededException, VirusTotalException {
        log.finest("Processing URL scan report request: " + enrichmentRequest.toString());

        String fqdn = enrichmentRequest.getReason().getFqdn();

        FileScanReport report;
        try {
            report = virusTotalService.getUrlScanReport("http://" + fqdn + "/");
        } catch (InvalidArguentsException | UnauthorizedAccessException | IOException e) {
            throw new VirusTotalException(e);
        }

        IoCVirusTotalReport total = new IoCVirusTotalReport();
        total.setFqdn(fqdn);
        total.setUrlReport(report);
        try {
            total.setScanDate(virusTotalService.parseDate(report.getScanDate()));
        } catch (ParseException e) {
            throw new VirusTotalException("Cannot parse scan date of report: " + report.getScanDate(), e);
        }

        for (IoCRecord matchedIoC : enrichmentRequest.getMatchedIocs()) {

            //String iocId = matchedIoC.getDocumentId();
            String uniqueRef = matchedIoC.getUniqueRef();

            IoCRecord ioc = archiveService.getIoCRecordByUniqueRef(uniqueRef);
            if (ioc == null) {
                log.warning("VirusTotal - IoC with uniqueRef: " + uniqueRef + " does not exist -> can't be enriched.");
                continue;
            }

            if (ioc.getVirusTotalReports() == null || ioc.getVirusTotalReports().length == 0) {
                //ioc.setVirusTotalReports(new IoCVirusTotalReport[]{total});
                log.finest("IoC does not have any report yet -> adding new one");
                archiveService.setVirusTotalReportToIoCRecord(ioc, new IoCVirusTotalReport[]{total});
            } else {
                List<IoCVirusTotalReport> reportsList = new ArrayList<>(Arrays.asList(ioc.getVirusTotalReports()));
                reportsList.add(total);
                //ioc.setVirusTotalReports(reportsList.toArray(new IoCVirusTotalReport[reportsList.size()]));
                archiveService.setVirusTotalReportToIoCRecord(ioc,reportsList.toArray(new IoCVirusTotalReport[reportsList.size()]));
                log.finest("IoC does have some reports already -> adding new one");
            }

            //archiveService.archiveVirusTotalReports(ioc);

            //archiveService.archiveIoCRecord(ioc);
        }
    }
}
