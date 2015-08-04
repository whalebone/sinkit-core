package biz.karms.sinkit.ejb;

import biz.karms.sinkit.eventlog.EventLogRecord;
import biz.karms.sinkit.exception.ArchiveException;
import biz.karms.sinkit.ioc.IoCRecord;

import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.client.JestResultHandler;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.Update;
import io.searchbox.params.Parameters;

import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by tkozel on 24.6.15.
 */
@Singleton
//TODO: Note that you will end up with many instances, each for a caller. No Lock? No TX?
public class ArchiveServiceEJB {

    @Inject
    private Logger log;

    @Inject
    private JestClient elasticClient;

    private static final String ELASTIC_IOC_INDEX = "iocs";
    private static final String ELASTIC_IOC_TYPE = "intelmq";
    private static final String ELASTIC_LOG_INDEX = "logs";
    private static final String ELASTIC_LOG_TYPE = "match";

    private static final String PARAMETER_FROM = "from";

    public IoCRecord findActiveIoCRecordBySourceId(
            String sourceId, String classificationType, String feedName) throws ArchiveException {

//        log.info("searching elastic [ source.id : " + sourceId.replace("\\","\\\\") + ", " +
//                "classification.type : " + classificationType.replace("\\","\\\\") + ", " +
//                "feed.name : " + feedName.replace("\\","\\\\") + "]");

        String query = "{\n" +
                "   \"query\" : {\n" +
                "       \"filtered\" : {\n"+
                "           \"query\" : {\n" +
                "               \"query_string\" : {\n" +
                "                   \"query\": \"active : true AND source.id.value : \\\"" + sourceId.replace("\\","\\\\") + "\\\" AND " +
                                                "classification.type: \\\"" + classificationType.replace("\\","\\\\") + "\\\" AND " +
                                                "feed.name : \\\"" + feedName.replace("\\","\\\\") +"\\\"\"\n" +
                "               }\n" +
                "           }\n" +
                "       }\n" +
                "   }\n" +
                "}\n";

        return this.searchArchiveForSingleHit(query);
    }

//    public IoCRecord findActiveIoCRecordByIp(String ip, String type, String feed) throws ArchiveException {
//
//        //log.info("searching elastic [ ip : " + ip + ", type : " + type + ", feed : " + feed + "]");
//
//        String query = "{\n" +
//                "   \"query\" : {\n" +
//                "       \"filtered\" : {\n"+
//                "           \"query\" : {\n" +
//                "               \"query_string\" : {\n" +
//                "                   \"query\": \"active : true AND source.ip : \\\"" + ip + "\\\" AND classification.type: \\\"" + type + "\\\" AND feed.name : \\\"" + feed +"\\\"\"\n" +
//                "               }\n" +
//                "           }\n" +
//                "       }\n" +
//                "   }\n" +
//                "}\n";
//
//        return this.searchArchiveForSingleHit(query);
//    }
//
//    public IoCRecord findActiveIoCRecordByFQDN(String fqdn, String type, String feed) throws ArchiveException {
//
//        //log.info("searching elastic [ fqdn : " + fqdn + ", type : " + type + ", feed : " + feed + "]");
//
//        String query = "{\n" +
//                "   \"query\" : {\n" +
//                "       \"filtered\" : {\n"+
//                "           \"query\" : {\n" +
//                "               \"query_string\" : {\n" +
//                "                   \"query\": \"active : true AND source.fqdn : \\\"" + fqdn + "\\\" AND classification.type: \\\"" + type + "\\\" AND feed.name : \\\"" + feed +"\\\"\"\n" +
//                "               }\n" +
//                "           }\n" +
//                "       }\n" +
//                "   }\n" +
//                "}\n";
//
//       return this.searchArchiveForSingleHit(query);
//    }

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

        return this.searchArchive(query);
    }

    private IoCRecord searchArchiveForSingleHit(String query) throws ArchiveException {

        List<IoCRecord> hits = this.searchArchive(query);

        if (hits.isEmpty()) return null;
        else if (hits.size() > 1) {
            log.severe("Query returned more than single result: " + query);
            throw new ArchiveException("Search returned " + hits.size() + " hits. Expecting max one -> panic!");
        }


        return hits.get(0);
    }

    private List<IoCRecord> searchArchive(String query) throws ArchiveException {

        return searchArchive(query, 0, 1000);
    }

    private List<IoCRecord> searchArchive(String query, int from, int size) throws ArchiveException {

        Search search = new Search.Builder(query)
                            .addIndex(ELASTIC_IOC_INDEX)
                            .addType(ELASTIC_IOC_TYPE)
                            .setParameter(PARAMETER_FROM, from)
                            .setParameter(Parameters.SIZE, size)
                            .build();

        //log.info("Searching archive with query: \n" + query);

        SearchResult result;
        try {
            result = elasticClient.execute(search);
        } catch (IOException e) {
            throw new ArchiveException("IoC search went wrong.", e);
        }

        if (!result.isSucceeded()) {
            throw new ArchiveException(result.getErrorMessage());
        }

        //log.info("Found " + result.getTotal() + " hits");

        //log.info(result.getJsonString());
        if (result.getTotal() < 1) return new ArrayList<>();

        return result.getSourceAsObjectList(IoCRecord.class);
    }

    public IoCRecord archiveIoCRecord(IoCRecord ioc) throws ArchiveException {

        Index index = new Index.Builder(ioc)
                .index(ELASTIC_IOC_INDEX)
                .type(ELASTIC_IOC_TYPE)
                .setParameter(Parameters.REFRESH, true)
                .build();
        //log.info("Indexing ioc [" + ioc.toString() + "]");

        JestResult result;
        try {
            result = elasticClient.execute(index);
        } catch (IOException e) {
            throw new ArchiveException("Indexing IoC went wrong.", e);
        }

        if (result.isSucceeded()) {
            String docId = result.getJsonObject().getAsJsonPrimitive("_id").getAsString();
            ioc.setDocumentId(docId);
            //log.info("Indexed ioc: [" + ioc.toString() + "]");
        } else {
            log.severe("Archive returned error: " + result.getErrorMessage());
            throw new ArchiveException(result.getErrorMessage());
        }
        return ioc;
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
                    elasticClient.execute(
                            new Update.Builder(query)
                                    .index(ELASTIC_IOC_INDEX)
                                    .type(ELASTIC_IOC_TYPE)
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

    public void archiveEventLogRecord(EventLogRecord logRecord) {

        String indexName = ELASTIC_LOG_INDEX + "-" + new SimpleDateFormat("yyyy.MM.dd").format(logRecord.getLogged());

        Index index = new Index.Builder(logRecord)
                .index(indexName)
                .type(ELASTIC_LOG_TYPE)
                .setParameter(Parameters.REFRESH, true)
                .build();
        //log.finest("Indexing logRecord [" + logRecord.toString() + "]");

        elasticClient.executeAsync(index, new JestResultHandler<JestResult>() {

            @Override
            public void completed(JestResult result) {
                if (!result.isSucceeded()) {
                    log.severe("Archive returned error: " + result.getErrorMessage());
                }
            }

            @Override
            public void failed(Exception ex) {
                log.severe("Indexing eventLog went wrong: " + ex.getMessage());
            }
        });
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

        return this.searchArchive(query, from, size);
    }
}
