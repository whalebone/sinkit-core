package biz.karms.sinkit.ejb.elastic;

import biz.karms.sinkit.exception.ArchiveException;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.params.Parameters;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by tkozel on 4.8.15.
 */
@Singleton
public class ElasticServiceEJB {

    public static final String ELASTIC_IOC_INDEX = "iocs";
    public static final String ELASTIC_IOC_TYPE = "intelmq";
    public static final String ELASTIC_LOG_INDEX = "logs";
    public static final String ELASTIC_LOG_TYPE = "match";

    private static final String PARAMETER_FROM = "from";
    private static final int DEF_LIMIT = 1000;

    @Inject
    private Logger log;

    @Inject
    private JestClient elasticClient;

    @PostConstruct
    public void setup() {
        if (elasticClient == null) {
            throw new IllegalArgumentException("JestClient must be injected.");
        }
    }

    public JestClient getElasticClient() {
        return elasticClient;
    }

    /**
     * Searches index of given type using query for single object of class T
     *
     * @param query Elastic Search query in JSON format
     * @param index Document index
     * @param type  Document type
     * @param clazz Class of object being found
     * @param <T>   Class of object being found
     * @return Found object of class T
     * @throws ArchiveException when communication with elastic went wrong or more than single hit is found
     */
    public <T extends Indexable> T searchForSingleHit(
            String query, String index, String type, Class<T> clazz) throws ArchiveException {

        List<T> hits = this.search(query, index, type, clazz);

        if (hits.isEmpty()) return null;
        else if (hits.size() > 1) {
            log.severe("Query returned more than single result: " + query);
            throw new ArchiveException("Search returned " + hits.size() + " hits. Expecting max one -> panic!");
        }
        return hits.get(0);
    }

    /**
     * Searches index of given type using query for objects of class T (max DEF_LIMIT of hits is returned)
     *
     * @param query Elastic Search query in JSON format
     * @param index Document index
     * @param type  Document type
     * @param clazz Class of object being found
     * @param <T>   Class of object being found
     * @return Found objects of class T
     * @throws ArchiveException when communication with elastic went wrong
     */
    public <T extends Indexable> List<T> search(String query, String index, String type, Class<T> clazz)
            throws ArchiveException {

        return search(query, index, type, 0, DEF_LIMIT, clazz);
    }

    /**
     * Searches index of given type using query for objects of class T. Search returns max "size" objects and
     * search starts at "from" offset
     *
     * @param query Elastic Search query in JSON format
     * @param index Document index
     * @param type  Document type
     * @param from  Offset of search
     * @param size  limit of returned objects
     * @param clazz Class of object being found
     * @param <T>   Class of object being found
     * @return Found objects of class T
     * @throws ArchiveException when communication with elastic went wrong
     */
    public <T extends Indexable> List<T> search(
            String query, String index, String type, int from, int size, Class<T> clazz) throws ArchiveException {

        Search search = new Search.Builder(query)
                .addIndex(index)
                .addType(type)
                .setParameter(PARAMETER_FROM, from)
                .setParameter(Parameters.SIZE, size)
                .build();

        //log.info("Searching archive with query: \n" + query);

        SearchResult result;
        try {
            result = elasticClient.execute(search);
        } catch (IOException e) {
            throw new ArchiveException("Elastic search went wrong.", e);
        }

        if (!result.isSucceeded()) {
            throw new ArchiveException(result.getErrorMessage());
        }

        //log.info("Found " + result.getTotal() + " hits");

        //log.info(result.getJsonString());
        if (result.getTotal() < 1) return new ArrayList<>();

        return result.getSourceAsObjectList(clazz);
    }

    /**
     * Stores object that implements Indexable interface
     *
     * @param document object being stored
     * @param index    Document index
     * @param type     Document type
     * @param <T>      Class of object being stored
     * @return indexed object
     * @throws ArchiveException when communication with Elastic Search server went wrong
     */
    public <T extends Indexable> T index(T document, String index, String type)
            throws ArchiveException {

        Index indexRequest = new Index.Builder(document)
                .index(index)
                .type(type)
                .setParameter(Parameters.REFRESH, true)
                .build();
        //log.info("Indexing ioc [" + ioc.toString() + "]");

        JestResult result;
        try {
            result = elasticClient.execute(indexRequest);
        } catch (IOException e) {
            throw new ArchiveException("Indexing IoC went wrong.", e);
        }

        if (result.isSucceeded()) {
            String docId = result.getJsonObject().getAsJsonPrimitive("_id").getAsString();
            document.setDocumentId(docId);
            //log.info("Indexed ioc: [" + ioc.toString() + "]");
        } else {
            log.severe("Archive returned error: " + result.getErrorMessage());
            throw new ArchiveException(result.getErrorMessage());
        }
        return document;
    }
}
