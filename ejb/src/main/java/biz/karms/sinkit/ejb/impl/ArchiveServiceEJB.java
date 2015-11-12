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

import javax.annotation.PostConstruct;
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
 * Created by tkozel on 24.6.15.
 */
@Stateless
public class ArchiveServiceEJB implements ArchiveService {

    public static final String ELASTIC_IOC_INDEX = "iocs";
    public static final String ELASTIC_IOC_TYPE = "intelmq";
    public static final String ELASTIC_LOG_INDEX = "logs";
    public static final String ELASTIC_LOG_TYPE = "match";

    private static final String ELASTIC_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
    private static final DateFormat dateFormatter = new SimpleDateFormat(ELASTIC_DATE_FORMAT);

    @Inject
    private Logger log;

    @EJB
    private ElasticService elasticService;

    @PostConstruct
    public void setup() {
        if (elasticService == null) {
            throw new IllegalArgumentException("ElasticServiceEJB must be injected.");
        }
    }

    @Override
    public List<IoCRecord> findIoCsForDeactivation(int hours) throws ArchiveException {
        //log.info("Searching archive for active IoCs with seen.last older than " + hours + " hours.");

        Calendar c = Calendar.getInstance();
        c.add(Calendar.HOUR, -hours);
        String tooOld = dateFormatter.format(c.getTime());

        String query = "{\n" +
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

        return elasticService.search(query, ELASTIC_IOC_INDEX,
                ELASTIC_IOC_TYPE, IoCRecord.class);
    }

    @Override
    public boolean archiveReceivedIoCRecord(IoCRecord ioc) throws ArchiveException {

        //compute documentId
        ioc.setDocumentId(IoCIdentificationUtils.computeHashedId(ioc));
        //compute uniqueReference
        ioc.setUniqueRef(IoCIdentificationUtils.computeUniqueReference(ioc));

        String seenLast = dateFormatter.format(ioc.getSeen().getLast());

        String upsertScript = "{\n" +
                "    \"script\" : \"ctx._source.seen.last = seenLast\"\n," +
                "    \"params\" : {\n" +
                "        \"seenLast\" : \"" +seenLast + "\"\n" +
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
    public boolean setVirusTotalReportToIoCRecord(IoCRecord ioc, IoCVirusTotalReport[] reports) throws ArchiveException{

        String updateScript = "{\n" +
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
    public IoCRecord deactivateRecord(IoCRecord ioc) throws ArchiveException {

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
        ioc = elasticService.index(ioc, ELASTIC_IOC_INDEX, ELASTIC_IOC_TYPE);

        return ioc;
    }

    @Override
    public EventLogRecord archiveEventLogRecord(EventLogRecord logRecord) throws ArchiveException {
        DateFormat df = new SimpleDateFormat("YYYY-MM-dd");
        String index = ELASTIC_LOG_INDEX + "-" + df.format(logRecord.getLogged());
        log.log(Level.FINE, "elasticService.index logging logrecord, index="+index);
        return elasticService.index(logRecord, index, ELASTIC_LOG_TYPE);
    }

    @Override
    public List<IoCRecord> getActiveIoCs(int from, int size) throws ArchiveException {
        //log.info("Searching archive for active IoCs with seen.last older than " + hours + " hours.");
        String query = "{\n" +
                "   \"query\": {\n" +
                "               \"term\": {\n" +
                "                   \"active\": true\n" +
                "               }\n" +
                "   }\n" +
                "}\n";

        return elasticService.search(query, ELASTIC_IOC_INDEX,
                ELASTIC_IOC_TYPE, from, size, IoCRecord.class);
    }

    @Override
    public IoCRecord getIoCRecordById(String id) throws ArchiveException {
        //log.log(Level.WARNING, "getIoCRecordById: id: "+id+", ELASTIC_IOC_INDEX: "+ELASTIC_IOC_INDEX+", ELASTIC_IOC_TYPE: "+ELASTIC_IOC_TYPE);
        return elasticService.getDocumentById(id, ELASTIC_IOC_INDEX, ELASTIC_IOC_TYPE, IoCRecord.class);
    }

    @Override
    public IoCRecord getIoCRecordByUniqueRef(String uniqueRef) throws ArchiveException {
        String query = "{\n" +
                "   \"query\": {\n" +
                "               \"term\": {\n" +
                "                   \"unique_ref\": \"" + uniqueRef + "\"\n" +
                "               }\n" +
                "   },\n" +
                "   \"sort\": { \"time.received_by_core\": { \"order\": \"desc\" }}\n" +
                "}\n";
        List<IoCRecord> iocs = elasticService.search(query, ELASTIC_IOC_INDEX, ELASTIC_IOC_TYPE, IoCRecord.class);
        if (iocs.isEmpty()) return null;
        if (iocs.size() > 1) {
            log.warning("Search for IoC with uniqueRef: " + uniqueRef + " returned " + iocs.size() + " records, expected max one. " +
                    "Record with document_id: " + iocs.get(0).getDocumentId() + " was used as a  reference. Please fix this inconsistency.");
        }
        return iocs.get(0);
    }

    @Override
    public EventLogRecord getLogRecordWaitingForVTScan() throws ArchiveException {
        String status = new GsonBuilder().create().toJson(VirusTotalRequestStatus.WAITING);
        String query = "{\n" +
                "   \"query\": {\n" +
                "               \"term\": {\n" +
                "                   \"virus_total_request.status\": " + status + "\n" +
                "               }\n" +
                "   },\n" +
                "   \"sort\": { \"logged\": { \"order\": \"asc\" }}\n" +
                "}\n";

        List<EventLogRecord> logRecords =
                elasticService.search(query, ELASTIC_LOG_INDEX + "-*",
                        ELASTIC_LOG_TYPE, 0, 1, EventLogRecord.class);

        if (logRecords.size() == 0) return null;

        return logRecords.get(0);
    }

    @Override
    public EventLogRecord getLogRecordWaitingForVTReport() throws ArchiveException {
        String status = new GsonBuilder().create().toJson(VirusTotalRequestStatus.WAITING_FOR_REPORT);

        String query = "{\n" +
                "   \"query\": {\n" +
                "               \"term\": {\n" +
                "                   \"virus_total_request.status\": " + status + "\n" +
                "               }\n" +
                "   },\n" +
                "   \"sort\": { \"virus_total_request.processed\": { \"order\": \"asc\", \"ignore_unmapped\" : true}}\n" +
                "}\n";

        List<EventLogRecord> logRecords =
                elasticService.search(query, ELASTIC_LOG_INDEX + "-*",
                        ELASTIC_LOG_TYPE, 0, 1, EventLogRecord.class);

        if (logRecords.size() == 0) return null;

        return logRecords.get(0);
    }
}
