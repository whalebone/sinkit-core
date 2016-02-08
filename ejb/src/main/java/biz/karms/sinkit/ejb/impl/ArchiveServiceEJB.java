package biz.karms.sinkit.ejb.impl;

import biz.karms.sinkit.ejb.ArchiveService;
import biz.karms.sinkit.ejb.elastic.ElasticService;
import biz.karms.sinkit.ejb.util.IoCIdentificationUtils;
import biz.karms.sinkit.eventlog.EventLogRecord;
import biz.karms.sinkit.eventlog.VirusTotalRequestStatus;
import biz.karms.sinkit.exception.ArchiveException;
import biz.karms.sinkit.ioc.IoCRecord;
import biz.karms.sinkit.ioc.IoCVirusTotalReport;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import org.apache.commons.collections.CollectionUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Tomas Kozel
 */
@Stateless
public class ArchiveServiceEJB implements ArchiveService {

    public static final String ELASTIC_IOC_INDEX = "iocs";
    public static final String ELASTIC_IOC_TYPE = "intelmq";
    public static final String ELASTIC_LOG_INDEX = "logs";
    public static final String ELASTIC_LOG_TYPE = "match";

    private static final String ELASTIC_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
    private static final DateFormat DATEFORMATTER = new SimpleDateFormat(ELASTIC_DATE_FORMAT);

    @Inject
    private Logger log;

    @EJB
    private ElasticService elasticService;

    @Override
    public List<IoCRecord> findIoCsForDeactivation(final int hours) throws ArchiveException {
        //log.info("Searching archive for active IoCs with seen.last older than " + hours + " hours.");
        final Calendar c = Calendar.getInstance();
        c.add(Calendar.HOUR, -hours);
        final String tooOld = DATEFORMATTER.format(c.getTime());

        final String query = "{\n" +
                "   \"query\": {\n" +
                "       \"filtered\": {\n" +
                "           \"query\": {\n" +
                "               \"term\": {\n" +
                "                   \"active\": true\n" +
                "               }\n" +
                "           }\n," +
                "           \"filter\": {\n" +
                "               \"range\": {\n" +
                "                   \"seen.last\": {\n" +
                "                       \"lt\" : \"" + tooOld + "\"\n" +
                "                   }\n" +
                "               }\n" +
                "           }\n" +
                "       }\n" +
                "   }\n" +
                "}\n";

        return elasticService.search(query, ELASTIC_IOC_INDEX, ELASTIC_IOC_TYPE, IoCRecord.class);
    }

    @Override
    public List<IoCRecord> findIoCsForWhitelisting(final String sourceId) throws ArchiveException {
        //log.info("Searching archive for active IoCs with seen.last older than " + hours + " hours.");
        final String query_string = "\"active: true AND NOT whitelist_name: * AND " +
                "(source.id.value: " + sourceId + " OR source.id.value: *." + sourceId + ")\"";
        final String query = "{\n" +
                "   \"query\": {\n" +
                "       \"filtered\": {\n" +
                "           \"query\": {\n" +
                "               \"query_string\": {\n" +
                "                   \"query\": " + query_string + ",\n" +
                "                   \"analyze_wildcard\": true\n" +
                "               }\n" +
                "           }\n" +
                "       }\n" +
                "   }\n" +
                "}\n";

        return elasticService.search(query, ELASTIC_IOC_INDEX, ELASTIC_IOC_TYPE, IoCRecord.class);
    }

    @Override
    public boolean archiveReceivedIoCRecord(final IoCRecord ioc) throws ArchiveException {
        //compute documentId
        ioc.setDocumentId(IoCIdentificationUtils.computeHashedId(ioc));
        //compute uniqueReference
        ioc.setUniqueRef(IoCIdentificationUtils.computeUniqueReference(ioc));
        final String seenLast = DATEFORMATTER.format(ioc.getSeen().getLast());
        final String upsertScript = "{\n" +
                "    \"script\" : \"ctx._source.seen.last = seenLast\"\n," +
                "    \"params\" : {\n" +
                "        \"seenLast\" : \"" + seenLast + "\"\n" +
                "    },\n" +
                "   \"upsert\" : " + new GsonBuilder()
                .setDateFormat(ELASTIC_DATE_FORMAT)
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create()
                .toJson(ioc) + "\n" +
                "}";
        return elasticService.update(ioc.getDocumentId(), upsertScript, ELASTIC_IOC_INDEX, ELASTIC_IOC_TYPE);
    }

    @Override
    public boolean setVirusTotalReportToIoCRecord(final IoCRecord ioc, final IoCVirusTotalReport[] reports) throws ArchiveException {
        final String updateScript = "{\n" +
                "   \"doc\" : {\n" +
                "       \"virus_total_reports\" : " +
                new GsonBuilder()
                        .setDateFormat(ELASTIC_DATE_FORMAT)
                        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                        .create()
                        .toJson(reports) + "\n" +
                "   }\n" +
                "}";
        ioc.setVirusTotalReports(reports);
        return elasticService.update(ioc.getDocumentId(), updateScript, ELASTIC_IOC_INDEX, ELASTIC_IOC_TYPE);
    }

    @Override
    public IoCRecord deactivateRecord(final IoCRecord ioc) throws ArchiveException {
        /**
         * Deactivation in archive: Old active IoC record is deleted and new one inactive is created.
         * This is done because in elastic is not possible to update id of record.
         */
        //delete old active ioc
        elasticService.delete(ioc, ELASTIC_IOC_INDEX, ELASTIC_IOC_TYPE);

        //archive deactivated ioc with new id
        ioc.getTime().setDeactivated(Calendar.getInstance().getTime());
        ioc.setActive(false);

        //IMPORTANT - id has to be computed after the deactivated time is set because it's part of the hash
        ioc.setDocumentId(IoCIdentificationUtils.computeHashedId(ioc));

        return elasticService.index(ioc, ELASTIC_IOC_INDEX, ELASTIC_IOC_TYPE);
    }

    @Override
    public IoCRecord setRecordWhitelisted(final IoCRecord ioc, final String whitelistName) throws ArchiveException {
        ioc.getTime().setWhitelisted(Calendar.getInstance().getTime());
        ioc.setWhitelistName(whitelistName);
        return elasticService.index(ioc, ELASTIC_IOC_INDEX, ELASTIC_IOC_TYPE);
    }

    @Override
    public EventLogRecord archiveEventLogRecord(final EventLogRecord logRecord) throws ArchiveException {
        final DateFormat df = new SimpleDateFormat("YYYY-MM-dd");
        final String index = ELASTIC_LOG_INDEX + "-" + df.format(logRecord.getLogged());
        log.log(Level.FINE, "elasticService.index logging logrecord, index=" + index);
        return elasticService.index(logRecord, index, ELASTIC_LOG_TYPE);
    }

    @Override
    public List<IoCRecord> getActiveNotWhitelistedIoCs(final int from, final int size) throws ArchiveException {
        //log.info("Searching archive for active IoCs with seen.last older than " + hours + " hours.");
        final String query = "{\n" +
                "   \"query\":{\n" +
                "       \"filtered\":{\n" +
                "           \"query\":{\n" +
                "               \"term\":{\n" +
                "                   \"active\":true\n" +
                "               }\n" +
                "           },\n" +
                "           \"filter\":{\n" +
                "               \"missing\":{\n" +
                "                   \"field\":\"whitelist_name\"\n" +
                "               }\n" +
                "           }\n" +
                "       }\n" +
                "   }\n" +
                "}\n";
        return elasticService.search(query, ELASTIC_IOC_INDEX, ELASTIC_IOC_TYPE, from, size, IoCRecord.class);
    }

    @Override
    public IoCRecord getIoCRecordById(final String id) throws ArchiveException {
        //log.log(Level.WARNING, "getIoCRecordById: id: "+id+", ELASTIC_IOC_INDEX: "+ELASTIC_IOC_INDEX+", ELASTIC_IOC_TYPE: "+ELASTIC_IOC_TYPE);
        return elasticService.getDocumentById(id, ELASTIC_IOC_INDEX, ELASTIC_IOC_TYPE, IoCRecord.class);
    }

    @Override
    public IoCRecord getIoCRecordByUniqueRef(final String uniqueRef) throws ArchiveException {
        final String query = "{\n" +
                "   \"query\": {\n" +
                "               \"term\": {\n" +
                "                   \"unique_ref\": \"" + uniqueRef + "\"\n" +
                "               }\n" +
                "   },\n" +
                "   \"sort\": { \"time.received_by_core\": { \"order\": \"desc\" }}\n" +
                "}\n";
        final List<IoCRecord> iocs = elasticService.search(query, ELASTIC_IOC_INDEX, ELASTIC_IOC_TYPE, IoCRecord.class);
        if (CollectionUtils.isEmpty(iocs)) {
            return null;
        }
        if (iocs.size() > 1) {
            log.warning("Search for IoC with uniqueRef: " + uniqueRef + " returned " + iocs.size() + " records, expected max one. " +
                    "Record with document_id: " + iocs.get(0).getDocumentId() + " was used as a reference. Please fix this inconsistency.");
        }
        return iocs.get(0);
    }

    @Override
    public EventLogRecord getLogRecordWaitingForVTScan(final int notAllowedFailedMinutes) throws ArchiveException {
        String query = "{\n" +
                getWaitingLogRecordQuery(VirusTotalRequestStatus.WAITING, notAllowedFailedMinutes) + ",\n" +
                "   \"sort\":{\n" +
                "       \"logged\":{\n" +
                "           \"order\":\"asc\"\n" +
                "       }\n" +
                "   }\n" +
                "}";
        final List<EventLogRecord> logRecords =
                elasticService.search(query, ELASTIC_LOG_INDEX + "-*",
                        ELASTIC_LOG_TYPE, 0, 1, EventLogRecord.class);
        if (CollectionUtils.isEmpty(logRecords)) {
            return null;
        }
        return logRecords.get(0);
    }

    @Override
    public EventLogRecord getLogRecordWaitingForVTReport(final int notAllowedFailedMinutes) throws ArchiveException {
        String query = "{\n" +
                getWaitingLogRecordQuery(VirusTotalRequestStatus.WAITING_FOR_REPORT, notAllowedFailedMinutes) + ",\n" +
                "   \"sort\":{\n" +
                "       \"virus_total_request.processed\":{\n" +
                "           \"order\": \"asc\",\n" +
                "           \"ignore_unmapped\" : true\n" +
                "       }\n" +
                "   }\n" +
                "}";

        final List<EventLogRecord> logRecords =
                elasticService.search(query, ELASTIC_LOG_INDEX + "-*",
                        ELASTIC_LOG_TYPE, 0, 1, EventLogRecord.class);
        if (CollectionUtils.isEmpty(logRecords)) {
            return null;
        }

        return logRecords.get(0);
    }

    private String getWaitingLogRecordQuery(final VirusTotalRequestStatus status, final int notAllowedFailedMinutes) {
        final String statusTerm = new GsonBuilder().create().toJson(status);
        final String notAllowedFailedRange = "\"now-" + notAllowedFailedMinutes + "m\"";
        return "    \"query\":{\n" +
                "        \"bool\":{\n" +
                "            \"filter\":{\n" +
                "                \"term\":{\n" +
                "                    \"virus_total_request.status\":" + statusTerm + "\n" +
                "                }\n" +
                "            },\n" +
                "            \"must_not\":{\n" +
                "                \"range\":{\n" +
                "                    \"virus_total_request.failed\":{\n" +
                "                        \"gt\":" + notAllowedFailedRange + "\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "    }";
    }
}
