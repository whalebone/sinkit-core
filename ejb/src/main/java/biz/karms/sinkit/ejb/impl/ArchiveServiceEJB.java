package biz.karms.sinkit.ejb.impl;

import biz.karms.sinkit.ejb.ArchiveService;
import biz.karms.sinkit.ejb.elastic.ElasticService;
import biz.karms.sinkit.ejb.elastic.logstash.LogstashClient;
import biz.karms.sinkit.ejb.util.IoCIdentificationUtils;
import biz.karms.sinkit.eventlog.EventLogRecord;
import biz.karms.sinkit.exception.ArchiveException;
import biz.karms.sinkit.ioc.IoCAccuCheckerReport;
import biz.karms.sinkit.ioc.IoCRecord;
import biz.karms.sinkit.ioc.IoCSeen;
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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static biz.karms.sinkit.ejb.elastic.ElasticResources.ELASTIC_DATE_FORMAT;

/**
 * @author Tomas Kozel
 */
@Stateless
public class ArchiveServiceEJB implements ArchiveService {

    public static final String ELASTIC_IOC_INDEX_ACTIVE = "iocs_v3";
    public static final String ELASTIC_IOC_INDEX_INACTIVE = "iocs-inactive";
    public static final String ELASTIC_IOC_INDEX_INACTIVE_SUFFIX_FORMAT = "yyyy-MM";
    public static final String ELASTIC_IOC_ALIAS = "iocs";
    public static final String ELASTIC_IOC_TYPE = "intelmq";
    public static final String ELASTIC_LOG_INDEX = "logs";
    public static final String ELASTIC_LOG_INDEX_SUFFIX_FORMAT = "yyyy-MM-dd";
    public static final String ELASTIC_LOG_TYPE = "match";

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
        final String tooOld = new SimpleDateFormat(ELASTIC_DATE_FORMAT).format(c.getTime());
        final QueryBuilder query = QueryBuilders.filteredQuery(
                QueryBuilders.termQuery("active", true),
                FilterBuilders.rangeFilter("seen.last").lt(tooOld)
        );
        return elasticService.search(query, null, ELASTIC_IOC_INDEX_ACTIVE, ELASTIC_IOC_TYPE, IoCRecord.class);
    }

    @Override
    public List<IoCRecord> findIoCsForWhitelisting(final String sourceId) throws ArchiveException {
        //log.info("Searching archive for active IoCs with seen.last older than " + hours + " hours.");
        final String queryString = "active: true AND NOT whitelist_name: * AND " +
                "(source.id.value: " + sourceId + " OR source.id.value: *." + sourceId + ")";
        final QueryBuilder query = QueryBuilders.queryStringQuery(queryString).analyzeWildcard(true);
        return elasticService.search(query, null, ELASTIC_IOC_INDEX_ACTIVE, ELASTIC_IOC_TYPE, IoCRecord.class);
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
        fieldsToUpdate.setMetadata(ioc.getMetadata());

        return elasticService.update(ioc.getDocumentId(), fieldsToUpdate, ELASTIC_IOC_INDEX_ACTIVE, ELASTIC_IOC_TYPE, ioc);
    }

    /**
     * Wrapper method for elasticService.update
     *
     * @param report
     * @param documentId
     * @return true if update is processed
     * @throws ArchiveException
     */
    @Override
    public boolean setReportToIoCRecord(final IoCAccuCheckerReport report, final String documentId) throws ArchiveException {
        return elasticService.update(documentId, report, ELASTIC_IOC_INDEX_ACTIVE, ELASTIC_IOC_TYPE, null);

    }

    /**
     * Deactivation in archive: Old active IoC record is deleted from active index and new one inactive is created
     * in inactive index
     */
    @Override
    public IoCRecord deactivateRecord(final IoCRecord ioc) throws ArchiveException {
        //delete old active ioc
        elasticService.delete(ioc, ELASTIC_IOC_INDEX_ACTIVE, ELASTIC_IOC_TYPE);

        //archive deactivated ioc with new id
        ioc.getTime().setDeactivated(Calendar.getInstance().getTime());
        ioc.setActive(false);

        //IMPORTANT - id has to be computed after the deactivated time is set because it's part of the hash
        ioc.setDocumentId(IoCIdentificationUtils.computeHashedId(ioc));

        return elasticService.index(ioc, iocIndexInactive(ioc), ELASTIC_IOC_TYPE);
    }

    @Override
    public IoCRecord setRecordWhitelisted(final IoCRecord ioc, final String whitelistName) throws ArchiveException {
        ioc.getTime().setWhitelisted(Calendar.getInstance().getTime());
        ioc.setWhitelistName(whitelistName);
        return elasticService.index(ioc, ELASTIC_IOC_INDEX_ACTIVE, ELASTIC_IOC_TYPE);
    }

    @Override
    public EventLogRecord archiveEventLogRecord(final EventLogRecord logRecord) throws ArchiveException {
        final String index = logIndex(logRecord);
        log.log(Level.FINE, "elasticService.index logging logrecord, index=" + index);
        return elasticService.index(logRecord, index, ELASTIC_LOG_TYPE);
    }

    @Override
    public EventLogRecord archiveEventLogRecordUsingLogstash(EventLogRecord logRecord) throws ArchiveException {
        final String index = logIndex(logRecord);
        log.log(Level.FINE, "archiveService.archiveEventLogRecordUsingLogstash logging logrecord, index=" + index);
        logstashClient.sentToLogstash(logRecord, index, ELASTIC_LOG_TYPE);
        return logRecord;
    }

    /**
     * Lists matching active entries, lists up to DEF_LIMIT of them (defined in ElasticServiceEJB.class)
     *
     * @param name  name of the field to be matched against
     * @param value value of this field
     * @return List of matching entries
     * @throws ArchiveException
     */
    @Override
    public List<IoCRecord> getMatchingActiveEntries(String name, String value) throws ArchiveException {
        final QueryBuilder query = QueryBuilders.boolQuery().must(
                QueryBuilders.termQuery(name, value)).must(QueryBuilders.termQuery("active", true));
        return elasticService.search(query, null, ELASTIC_IOC_INDEX_ACTIVE, ELASTIC_IOC_TYPE, IoCRecord.class);
    }

    @Override
    public List<IoCRecord> getActiveNotWhitelistedIoCs(final int from, final int size) throws ArchiveException {
        final QueryBuilder query = QueryBuilders.filteredQuery(
                QueryBuilders.termQuery("active", true),
                FilterBuilders.missingFilter("whitelist_name")
        );

        return elasticService.search(query, null, ELASTIC_IOC_INDEX_ACTIVE, ELASTIC_IOC_TYPE, from, size, IoCRecord.class);
    }

    @Override
    public IoCRecord getActiveIoCRecordById(final String id) throws ArchiveException {
        //log.log(Level.WARNING, "getActiveIoCRecordById: id: "+id+", ELASTIC_IOC_INDEX: "+ELASTIC_IOC_INDEX+", ELASTIC_IOC_TYPE: "+ELASTIC_IOC_TYPE);
        return elasticService.getDocumentById(id, ELASTIC_IOC_INDEX_ACTIVE, ELASTIC_IOC_TYPE, IoCRecord.class);
    }

    @Override
    public IoCRecord getIoCRecordByUniqueRef(final String uniqueRef) throws ArchiveException {
        final QueryBuilder query = QueryBuilders.termQuery("unique_ref", uniqueRef);
        final SortBuilder sort = SortBuilders.fieldSort("time.received_by_core").order(SortOrder.DESC);

        final List<IoCRecord> iocs = elasticService.search(query, sort, ELASTIC_IOC_ALIAS, ELASTIC_IOC_TYPE, IoCRecord.class);
        if (CollectionUtils.isEmpty(iocs)) {
            return null;
        }
        if (iocs.size() > 1) {
            log.warning("Search for IoC with uniqueRef: " + uniqueRef + " returned " + iocs.size() + " records, expected max one. " +
                    "Record with document_id: " + iocs.get(0).getDocumentId() + " was used as a reference. Please fix this inconsistency.");
        }
        return iocs.get(0);
    }

    private static String logIndex(EventLogRecord logRecord) {
        final DateFormat df = new SimpleDateFormat(ELASTIC_LOG_INDEX_SUFFIX_FORMAT);
        return ELASTIC_LOG_INDEX + "-" + df.format(logRecord.getLogged());
    }

    private static String iocIndexInactive(IoCRecord inactiveIoC) {
        final DateFormat df = new SimpleDateFormat(ELASTIC_IOC_INDEX_INACTIVE_SUFFIX_FORMAT);
        return ELASTIC_IOC_INDEX_INACTIVE + "-" + df.format(inactiveIoC.getTime().getDeactivated());
    }
}
