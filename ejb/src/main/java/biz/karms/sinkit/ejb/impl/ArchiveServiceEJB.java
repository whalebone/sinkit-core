package biz.karms.sinkit.ejb.impl;

import biz.karms.sinkit.ejb.ArchiveService;
import biz.karms.sinkit.ejb.elastic.ElasticService;
import biz.karms.sinkit.ejb.elastic.logstash.LogstashClient;
import biz.karms.sinkit.ejb.util.IoCIdentificationUtils;
import biz.karms.sinkit.eventlog.EventLogRecord;
import biz.karms.sinkit.eventlog.VirusTotalRequestStatus;
import biz.karms.sinkit.exception.ArchiveException;
import biz.karms.sinkit.ioc.IoCRecord;
import biz.karms.sinkit.ioc.IoCSeen;
import biz.karms.sinkit.ioc.IoCVirusTotalReport;
import com.google.gson.Gson;
import org.apache.commons.collections.CollectionUtils;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static biz.karms.sinkit.ejb.elastic.ElasticResources.ELASTIC_DATE_FORMAT;

/**
 * @author Tomas Kozel
 */
@Stateless
public class ArchiveServiceEJB implements ArchiveService {

    public static final String ELASTIC_IOC_INDEX = "iocs";
    public static final String ELASTIC_IOC_TYPE = "intelmq";
    public static final String ELASTIC_LOG_INDEX = "logs";
    public static final String ELASTIC_LOG_TYPE = "match";

    private static final DateFormat DATEFORMATTER = new SimpleDateFormat(ELASTIC_DATE_FORMAT);

    @Inject
    private Logger log;

    @Inject
    private Gson gson;

    @EJB
    private ElasticService elasticService;

    @Inject
    private LogstashClient logstashClient;

    @Override
    public List<IoCRecord> findIoCsForDeactivation(final int hours) throws ArchiveException {
        //log.info("Searching archive for active IoCs with seen.last older than " + hours + " hours.");
        final Calendar c = Calendar.getInstance();
        c.add(Calendar.HOUR, -hours);
        //TODO: Is this thread safe?
        final String tooOld = DATEFORMATTER.format(c.getTime());
        final QueryBuilder query = QueryBuilders.filteredQuery(
                QueryBuilders.termQuery("active", true),
                FilterBuilders.rangeFilter("seen.last").lt(tooOld)
        );
        return elasticService.search(query, null, ELASTIC_IOC_INDEX, ELASTIC_IOC_TYPE, IoCRecord.class);
    }

    @Override
    public List<IoCRecord> findIoCsForWhitelisting(final String sourceId) throws ArchiveException {
        //log.info("Searching archive for active IoCs with seen.last older than " + hours + " hours.");
        final String queryString = "active: true AND NOT whitelist_name: * AND " +
                "(source.id.value: " + sourceId + " OR source.id.value: *." + sourceId + ")";
        final QueryBuilder query = QueryBuilders.queryStringQuery(queryString).analyzeWildcard(true);
        return elasticService.search(query, null, ELASTIC_IOC_INDEX, ELASTIC_IOC_TYPE, IoCRecord.class);
    }

    @Override
    public boolean archiveReceivedIoCRecord(final IoCRecord ioc) throws ArchiveException {
        //compute documentId
        ioc.setDocumentId(IoCIdentificationUtils.computeHashedId(ioc));
        //compute uniqueReference
        ioc.setUniqueRef(IoCIdentificationUtils.computeUniqueReference(ioc));

        final IoCRecord fieldsToUpdate = new IoCRecord();
        IoCSeen seen = new IoCSeen();
        seen.setLast(ioc.getSeen().getLast());
        fieldsToUpdate.setAccuracy(ioc.getAccuracy());
        fieldsToUpdate.setSeen(seen);

        return elasticService.update(ioc.getDocumentId(), fieldsToUpdate, ELASTIC_IOC_INDEX, ELASTIC_IOC_TYPE, ioc);
    }

    @Override
    public boolean setVirusTotalReportToIoCRecord(final IoCRecord ioc, final IoCVirusTotalReport[] reports) throws ArchiveException {
        final Map<String, IoCVirusTotalReport[]> vtReports = new HashMap<>();
        vtReports.put("virus_total_reports", reports);
        return elasticService.update(ioc.getDocumentId(), vtReports, ELASTIC_IOC_INDEX, ELASTIC_IOC_TYPE, null);
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
    public EventLogRecord archiveEventLogRecordUsingLogstash(EventLogRecord logRecord) throws ArchiveException {
        final DateFormat df = new SimpleDateFormat("YYYY-MM-dd");
        final String index = ELASTIC_LOG_INDEX + "-" + df.format(logRecord.getLogged());
        log.log(Level.FINE, "archiveService.archiveEventLogRecordUsingLogstash logging logrecord, index=" + index);
        logstashClient.sentToLogstash(logRecord, index, ELASTIC_LOG_TYPE);
        return logRecord;
    }

    @Override
    public List<IoCRecord> getActiveNotWhitelistedIoCs(final int from, final int size) throws ArchiveException {
        final QueryBuilder query = QueryBuilders.filteredQuery(
                QueryBuilders.termQuery("active", true),
                FilterBuilders.missingFilter("whitelist_name")
        );

        return elasticService.search(query, null, ELASTIC_IOC_INDEX, ELASTIC_IOC_TYPE, from, size, IoCRecord.class);
    }

    @Override
    public IoCRecord getIoCRecordById(final String id) throws ArchiveException {
        //log.log(Level.WARNING, "getIoCRecordById: id: "+id+", ELASTIC_IOC_INDEX: "+ELASTIC_IOC_INDEX+", ELASTIC_IOC_TYPE: "+ELASTIC_IOC_TYPE);
        return elasticService.getDocumentById(id, ELASTIC_IOC_INDEX, ELASTIC_IOC_TYPE, IoCRecord.class);
    }

    @Override
    public IoCRecord getIoCRecordByUniqueRef(final String uniqueRef) throws ArchiveException {
        final QueryBuilder query = QueryBuilders.termQuery("unique_ref", uniqueRef);
        final SortBuilder sort = SortBuilders.fieldSort("time.received_by_core").order(SortOrder.DESC);

        final List<IoCRecord> iocs = elasticService.search(query, sort, ELASTIC_IOC_INDEX, ELASTIC_IOC_TYPE, IoCRecord.class);
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
        final QueryBuilder query = getWaitingLogRecordQuery(VirusTotalRequestStatus.WAITING, notAllowedFailedMinutes);
        final SortBuilder sort = SortBuilders.fieldSort("logged").order(SortOrder.ASC);
        final List<EventLogRecord> logRecords =
                elasticService.search(query, sort, ELASTIC_LOG_INDEX + "-*",
                        ELASTIC_LOG_TYPE, 0, 1, EventLogRecord.class);
        if (CollectionUtils.isEmpty(logRecords)) {
            return null;
        }
        return logRecords.get(0);
    }

    @Override
    public EventLogRecord getLogRecordWaitingForVTReport(final int notAllowedFailedMinutes) throws ArchiveException {
        final QueryBuilder query = getWaitingLogRecordQuery(VirusTotalRequestStatus.WAITING_FOR_REPORT, notAllowedFailedMinutes);
        final SortBuilder sort = SortBuilders.fieldSort("virus_total_request.processed")
                .order(SortOrder.ASC)
                .unmappedType("date");
        final List<EventLogRecord> logRecords =
                elasticService.search(query, sort, ELASTIC_LOG_INDEX + "-*",
                        ELASTIC_LOG_TYPE, 0, 1, EventLogRecord.class);
        if (CollectionUtils.isEmpty(logRecords)) {
            return null;
        }

        return logRecords.get(0);
    }

    private QueryBuilder getWaitingLogRecordQuery(final VirusTotalRequestStatus status, final int notAllowedFailedMinutes) {
        final String statusTerm = gson.toJson(status).replace("\"", "");
        final String notAllowedFailedRange = "now-" + notAllowedFailedMinutes + "m";
        return QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("virus_total_request.status", statusTerm))
                .mustNot(QueryBuilders.rangeQuery("virus_total_request.failed").gt(notAllowedFailedRange));
    }
}
