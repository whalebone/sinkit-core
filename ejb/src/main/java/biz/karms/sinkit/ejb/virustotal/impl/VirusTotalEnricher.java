package biz.karms.sinkit.ejb.virustotal.impl;

import biz.karms.sinkit.ejb.ArchiveService;
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
import org.apache.commons.lang3.StringUtils;
import org.infinispan.context.InvocationContext;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.ScheduleExpression;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.inject.Inject;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Tomas Kozel
 */
@Singleton
@LocalBean
@Startup
public class VirusTotalEnricher {

    // we can call the Virus Total API only 4 times per minute
    public static final int SHOTS_PER_RUN = 4;
    // max attempts of VT enrichment before VT request status is set to FAILED
    public static final int MAX_FAILED_ATTEMPTS = 3;
    // minutes that have to last after unsuccessful VT enrichment attempt than VT request can be processed again
    // this prevents the VT request to be processed immediately again after it fails
    public static final int NOT_ALLOWED_FAILED_MINUTES = 2;
    // max of VT request that can be processed whiting single run of enrichment
    // this prevents enrichment to go through all requests in archive within single run in case that each VT request fails
    public static final int MAX_RECORDS_PER_RUN = 50;

    public static final boolean SINKIT_VIRUS_TOTAL_SKIP = (System.getenv().containsKey("SINKIT_VIRUS_TOTAL_SKIP")) && Boolean.parseBoolean(System.getenv("SINKIT_VIRUS_TOTAL_SKIP"));

    @Inject
    private Logger log;

    @EJB
    private ArchiveService archiveService;

    @EJB
    private VirusTotalService virusTotalService;

    @Resource
    private TimerService timerService;

    @PostConstruct
    private void initialize() {
        if (!SINKIT_VIRUS_TOTAL_SKIP) {
            timerService.createCalendarTimer(new ScheduleExpression().hour("*").minute("*").second("0/40"), new TimerConfig("VirusTotalEnricher", false));
        }
    }

    @PreDestroy
    public void stop() {
        log.info("Stop all existing VirusTotalEnricher timers.");
        for (Timer timer : timerService.getTimers()) {
            log.fine("Stop VirusTotalEnricher timer: " + timer.getInfo());
            timer.cancel();
        }
    }

    @Timeout
    public void scheduler(Timer timer) {
        log.info("VirusTotalEnricher HASingletonTimer: Info=" + timer.getInfo());
        doEnrichment();
    }

    public void doEnrichment() {
        int availableVTCalls = SHOTS_PER_RUN;
        int recordsToProcess = MAX_RECORDS_PER_RUN;
        boolean reportRequestQueueEmpty = false;
        try {
            while (availableVTCalls > 0 && recordsToProcess > 0) {
                if (!reportRequestQueueEmpty) {
                    EventLogRecord reportRequest = archiveService.getLogRecordWaitingForVTReport(NOT_ALLOWED_FAILED_MINUTES);
                    if (reportRequest != null) {
                        try {
                            processUrlScanReportRequest(reportRequest);
                            availableVTCalls--;
                            reportRequest.getVirusTotalRequest().setStatus(VirusTotalRequestStatus.FINISHED);
                            reportRequest.getVirusTotalRequest().setReportReceived(new Date());
                        } catch (VirusTotalException e) {
                            if (e.isApiCalled()) {
                                availableVTCalls--;
                            }
                            setFailedAttempt(reportRequest, e.getMessage());
                        }
                        archiveService.archiveEventLogRecord(reportRequest);
                    } else {
                        reportRequestQueueEmpty = true;
                    }
                } else {
                    EventLogRecord scanRequest = archiveService.getLogRecordWaitingForVTScan(NOT_ALLOWED_FAILED_MINUTES);
                    if (scanRequest != null) {

                        boolean needEnrichment = false;
                        try {
                            // API is being called here
                            needEnrichment = processUrlScanRequest(scanRequest);
                            if (needEnrichment) {
                                availableVTCalls--;
                                scanRequest.getVirusTotalRequest().setStatus(VirusTotalRequestStatus.WAITING_FOR_REPORT);
                            } else {
                                scanRequest.getVirusTotalRequest().setStatus(VirusTotalRequestStatus.NOT_NEEDED);
                            }
                            scanRequest.getVirusTotalRequest().setProcessed(new Date());
                        } catch (VirusTotalException e) {
                            if (e.isApiCalled()) {
                                availableVTCalls--;
                            }
                            setFailedAttempt(scanRequest, e.getMessage());
                        }
                        archiveService.archiveEventLogRecord(scanRequest);
                    } else {
                        // there is no more records waiting for processing nor waiting for report in this run -> we can end the whole process
                        break;
                    }
                }
                recordsToProcess--;
            }
        } catch (QuotaExceededException e) {
            log.warning("VirusTutotal enrichment: quota exceeded before than expected -> skipping next runs in batch");
        } catch (ArchiveException e) {
            log.severe("Virus Total enrichment went wrong: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean processUrlScanRequest(final EventLogRecord enrichmentRequest) throws ArchiveException, QuotaExceededException, VirusTotalException {
        log.finest("Processing URL scan request: " + enrichmentRequest.toString());

        String scanTarget = getScanTarget(enrichmentRequest);
        if (scanTarget == null) {
            throw new VirusTotalException("EventLog can't have both reason.fqdn and reason.ip set as null", false);
        }
        boolean needEnrichment = false;
        String uniqueRef;
        IoCRecord ioc;
        iocsLoop:
        for (IoCRecord matchedIoC : enrichmentRequest.getMatchedIocs()) {
            uniqueRef = matchedIoC.getUniqueRef();
            ioc = archiveService.getIoCRecordByUniqueRef(uniqueRef);
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
                boolean fqdnOrIpFound = false;
                for (IoCVirusTotalReport report : ioc.getVirusTotalReports()) {
                    if (scanTarget.equals(report.getFqdn()) || scanTarget.equals(report.getIp())) {
                        log.finest("VT report for same FQDN or IP found: " + report.toString());
                        fqdnOrIpFound = true;
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
                if (!fqdnOrIpFound) {
                    needEnrichment = true;
                    log.finest("VT report with the same FQDN or IP was not found -> FQDN/IP will be scanned");
                    break;
                }
            }
        }

        if (needEnrichment) {
            try {
                virusTotalService.scanUrl("http://" + scanTarget + "/");
            } catch (InvalidArguentsException | UnauthorizedAccessException e) {
                throw new VirusTotalException(e, true);
            } catch (IOException e) {
                throw new VirusTotalException(e, false);
            }
        } else {
            log.finest("Enrichment is not needed.");
        }

        return needEnrichment;
    }

    private void processUrlScanReportRequest(final EventLogRecord enrichmentRequest) throws ArchiveException, QuotaExceededException, VirusTotalException {
        log.finest("Processing URL scan report request: " + enrichmentRequest.toString());

        String scanTarget = getScanTarget(enrichmentRequest);
        if (scanTarget == null) {
            throw new VirusTotalException("EventLog can't have both reason.fqdn and reason.ip set as null", false);
        }

        FileScanReport report;
        try {
            report = virusTotalService.getUrlScanReport("http://" + scanTarget + "/");
        } catch (InvalidArguentsException | UnauthorizedAccessException e) {
            throw new VirusTotalException(e, true);
        } catch (IOException e) {
            throw new VirusTotalException(e, false);
        }

        IoCVirusTotalReport total = new IoCVirusTotalReport();
        if (enrichmentRequest.getReason().getFqdn() != null) {
            total.setFqdn(scanTarget);
        } else {
            total.setIp(scanTarget);
        }
        total.setUrlReport(report);
        if (report.getScanDate() == null) {
            throw new VirusTotalException("Receiving scan report failed: received scan date is null, something is really wrong", true);
        }
        try {
            total.setScanDate(virusTotalService.parseDate(report.getScanDate()));
        } catch (ParseException e) {
            throw new VirusTotalException("Cannot parse scan date of report: " + report.getScanDate(), e, true);
        }

        String uniqueRef;
        for (IoCRecord matchedIoC : enrichmentRequest.getMatchedIocs()) {
            uniqueRef = matchedIoC.getUniqueRef();

            IoCRecord ioc = archiveService.getIoCRecordByUniqueRef(uniqueRef);
            if (ioc == null) {
                log.warning("VirusTotal - IoC with uniqueRef: " + uniqueRef + " does not exist -> can't be enriched.");
                continue;
            }

            if (ioc.getVirusTotalReports() == null || ioc.getVirusTotalReports().length == 0) {
                log.finest("IoC does not have any report yet -> adding new one");
                archiveService.setVirusTotalReportToIoCRecord(ioc, new IoCVirusTotalReport[]{total});
            } else {
                List<IoCVirusTotalReport> reportsList = new ArrayList<>(Arrays.asList(ioc.getVirusTotalReports()));
                reportsList.add(total);
                archiveService.setVirusTotalReportToIoCRecord(ioc, reportsList.toArray(new IoCVirusTotalReport[reportsList.size()]));
                log.finest("IoC does have some reports already -> adding new one");
            }
        }
    }

    private String getScanTarget(EventLogRecord request) {
        if (request == null || request.getReason() == null) {
            return null;
        }
        String scanTarget = request.getReason().getFqdn();
        if (StringUtils.isBlank(scanTarget)) {
            scanTarget = request.getReason().getIp();
        }
        return scanTarget;
    }

    private void setFailedAttempt(EventLogRecord record, String causeOfFailure) {
        int failedAttempts = 0;
        if (record.getVirusTotalRequest().getFailedAttempts() != null) {
            failedAttempts = record.getVirusTotalRequest().getFailedAttempts();
        }
        record.getVirusTotalRequest().setFailedAttempts(++failedAttempts);
        record.getVirusTotalRequest().setCauseOfFailure(causeOfFailure);
        record.getVirusTotalRequest().setFailed(Calendar.getInstance().getTime());
        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            record.getVirusTotalRequest().setStatus(VirusTotalRequestStatus.FAILED);
        }
    }
}
