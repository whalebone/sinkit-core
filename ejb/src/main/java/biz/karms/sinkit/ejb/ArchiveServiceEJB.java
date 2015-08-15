package biz.karms.sinkit.ejb;

import biz.karms.sinkit.ejb.elastic.ElasticServiceEJB;
import biz.karms.sinkit.eventlog.EventLogRecord;
import biz.karms.sinkit.eventlog.VirusTotalRequestStatus;
import biz.karms.sinkit.exception.ArchiveException;
import biz.karms.sinkit.ioc.IoCRecord;
import com.google.gson.GsonBuilder;
import io.searchbox.client.JestResult;
import io.searchbox.core.Get;
import io.searchbox.core.Update;
import io.searchbox.params.Parameters;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.inject.Inject;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by tkozel on 24.6.15.
 */
@Singleton
public class ArchiveServiceEJB {

    @Inject
    private Logger log;

    @Inject
    private ElasticServiceEJB elasticService;

    @PostConstruct
    public void setup() {
        if (elasticService == null) {
            throw new IllegalArgumentException("ElasticServiceEJB must be injected.");
        }
    }

    public IoCRecord findActiveIoCRecordBySourceId(
            String sourceId, String classificationType, String feedName) throws ArchiveException {

//        log.info("searching elastic [ source.id : " + sourceId.replace("\\","\\\\") + ", " +
//                "classification.type : " + classificationType.replace("\\","\\\\") + ", " +
//                "feed.name : " + feedName.replace("\\","\\\\") + "]");

        String query = "{\n" +
                "   \"query\" : {\n" +
                "       \"filtered\" : {\n" +
                "           \"query\" : {\n" +
                "               \"query_string\" : {\n" +
                "                   \"query\": \"active : true AND source.id.value : \\\"" + sourceId.replace("\\", "\\\\") + "\\\" AND " +
                "classification.type: \\\"" + classificationType.replace("\\", "\\\\") + "\\\" AND " +
                "feed.name : \\\"" + feedName.replace("\\", "\\\\") + "\\\"\"\n" +
                "               }\n" +
                "           }\n" +
                "       }\n" +
                "   }\n" +
                "}\n";

        return elasticService.searchForSingleHit(query, ElasticServiceEJB.ELASTIC_IOC_INDEX,
                ElasticServiceEJB.ELASTIC_IOC_TYPE, IoCRecord.class);
    }

    public List<IoCRecord> findIoCsForDeactivation(int hours) throws ArchiveException {

        //log.info("Searching archive for active IoCs with seen.last older than " + hours + " hours.");

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
                "                       \"lt\": \"now-" + hours + "h\", \n" +
                "                       \"time_zone\": \"+1:00\"\n" +
                "                   }\n" +
                "               }\n" +
                "           }\n" +
                "       }\n" +
                "   }\n" +
                "}\n";

        return elasticService.search(query, ElasticServiceEJB.ELASTIC_IOC_INDEX,
                ElasticServiceEJB.ELASTIC_IOC_TYPE, IoCRecord.class);
    }

    public IoCRecord archiveIoCRecord(IoCRecord ioc) throws ArchiveException {
        return elasticService.index(ioc, ElasticServiceEJB.ELASTIC_IOC_INDEX,
                ElasticServiceEJB.ELASTIC_IOC_TYPE);
    }

    public IoCRecord deactivateRecord(IoCRecord ioc) throws ArchiveException {

        ioc.setActive(false);

        String query = "{\n" +
                "   \"doc\" : {\n" +
                "       \"active\" : false\n" +
                "   }\n" +
                "}\n";

        //log.info(query);

        JestResult result;
        //log.info("Deactivating ioc [" + ioc.toString() + "]");
        try {
            result =
                    elasticService.getElasticClient().execute(
                            new Update.Builder(query)
                                    .index(ElasticServiceEJB.ELASTIC_IOC_INDEX)
                                    .type(ElasticServiceEJB.ELASTIC_IOC_TYPE)
                                    .id(ioc.getDocumentId())
                                    .setParameter(Parameters.REFRESH, true)
                                    .build()
                    );
        } catch (IOException e) {
            throw new ArchiveException("IoC deactivation went wrong.", e);
        }

        if (!result.isSucceeded()) {
            log.severe("IoC deactovation wasn't successful: " + result.getErrorMessage());
            //log.info(result.getJsonString());
            throw new ArchiveException(result.getErrorMessage());
        }

        return ioc;
    }

    public EventLogRecord archiveEventLogRecord(EventLogRecord logRecord) throws ArchiveException {

        DateFormat df = new SimpleDateFormat("YYYY.MM.dd");
        String index = ElasticServiceEJB.ELASTIC_LOG_INDEX + "-" + df.format(logRecord.getLogged());

        return elasticService.index(logRecord, index,
                ElasticServiceEJB.ELASTIC_LOG_TYPE);
    }

    public List<IoCRecord> getActiveIoCs(int from, int size) throws ArchiveException {

        //log.info("Searching archive for active IoCs with seen.last older than " + hours + " hours.");

        String query = "{\n" +
                "   \"query\": {\n" +
                "               \"term\": {\n" +
                "                   \"active\": true\n" +
                "               }\n" +
                "   }\n" +
                "}\n";

        return elasticService.search(query, ElasticServiceEJB.ELASTIC_IOC_INDEX,
                ElasticServiceEJB.ELASTIC_IOC_TYPE, from, size, IoCRecord.class);
    }

    public IoCRecord getIoCRecordById(String id) throws ArchiveException {

        JestResult result;
        IoCRecord ioc;

        Get get = new Get.Builder(ElasticServiceEJB.ELASTIC_IOC_INDEX, id)
                .type(ElasticServiceEJB.ELASTIC_IOC_TYPE).build();
        try {

            result = elasticService.getElasticClient().execute(get);
            ioc = result.getSourceAsObject(IoCRecord.class);

        } catch (IOException e) {
            throw new ArchiveException("IoC search went wrong.", e);
        }

        if (!result.isSucceeded()) {
            return null;
        }

        return ioc;
    }

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
                elasticService.search(query, ElasticServiceEJB.ELASTIC_LOG_INDEX + "-*",
                        ElasticServiceEJB.ELASTIC_LOG_TYPE, 0, 1, EventLogRecord.class);

        if (logRecords.size() == 0) return null;

        return logRecords.get(0);
    }

    public EventLogRecord getLogRecordWaitingForVTReport() throws ArchiveException {

        String status = new GsonBuilder().create().toJson(VirusTotalRequestStatus.WAITING_FOR_REPORT);

        String query = "{\n" +
                "   \"query\": {\n" +
                "               \"term\": {\n" +
                "                   \"virus_total_request.status\": " + status + "\n" +
                "               }\n" +
                "   },\n" +
                "   \"sort\": { \"virus_total_request.processed\": { \"order\": \"asc\" }}\n" +
                "}\n";

        List<EventLogRecord> logRecords =
                elasticService.search(query, ElasticServiceEJB.ELASTIC_LOG_INDEX + "-*",
                        ElasticServiceEJB.ELASTIC_LOG_TYPE, 0, 1, EventLogRecord.class);

        if (logRecords.size() == 0) return null;

        return logRecords.get(0);
    }
}
