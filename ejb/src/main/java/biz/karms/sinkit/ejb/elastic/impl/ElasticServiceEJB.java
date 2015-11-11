package biz.karms.sinkit.ejb.elastic.impl;

import biz.karms.sinkit.ejb.elastic.ElasticService;
import biz.karms.sinkit.ejb.elastic.Indexable;
import biz.karms.sinkit.exception.ArchiveException;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.*;
import io.searchbox.params.Parameters;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by tkozel on 4.8.15.
 */
@Stateless
public class ElasticServiceEJB implements ElasticService {

    private static final String PARAMETER_FROM = "from";
    private static final int DEF_LIMIT = 1000;

    public static final String TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

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
    @Override
    public <T extends Indexable> T searchForSingleHit(String query, String index, String type, Class<T> clazz) throws ArchiveException {
        List<T> hits = this.search(query, index, type, clazz);

        if (hits.isEmpty()) return null;
        else if (hits.size() > 1) {
            log.severe("Query returned more than single result: " + query);
            throw new ArchiveException("Search returned " + hits.size() + " hits. Expecting max one -> panic!");
        }
        return hits.get(0);
    }

    @Override
    public <T extends Indexable> T getDocumentById(String id, String index, String type, Class<T> clazz) throws ArchiveException {
        JestResult result;
        T document;
        log.log(Level.FINE, "getDocumentById called: id:" + id);
        Get get = new Get.Builder(index, id).type(type).build();
        try {

            result = elasticClient.execute(get);
            document = result.getSourceAsObject(clazz);

        } catch (Exception e) {
            throw new ArchiveException("IoC search went wrong.", e);
        }

        if (!result.isSucceeded()) {
            log.log(Level.WARNING, "Can't get document with id:  " + id + ". Elastic returned: " + result.getResponseCode() + ", response code: " + result.getResponseCode());
            return null;
        }
        log.log(Level.FINE, "getDocumentById returning document id: " + document.getDocumentId());
        return document;
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
    @Override
    public <T extends Indexable> List<T> search(String query, String index, String type, Class<T> clazz) throws ArchiveException {
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
    @Override
    public <T extends Indexable> List<T> search(String query, String index, String type, int from, int size, Class<T> clazz) throws ArchiveException {
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
        } catch (Exception e) {
            throw new ArchiveException("Elastic search went wrong.", e);
        }

        if (!result.isSucceeded()) {
            if (result.getResponseCode() == 404) {
                //when 404 is returned then perhaps the index is missing, but we can consider it as no hits were found
                //index will be automatically created during the first indexation of a document of given index
                log.warning("Searching the Archive returned code 404, error message: " + result.getErrorMessage());
                return new ArrayList<>();
            } else {
                throw new ArchiveException("Elastic search went wrong:" + result.getErrorMessage() + ", response code: " + result.getResponseCode());
            }
        }

        //log.info("Found " + result.getTotal() + " hits");

        //log.info(result.getJsonString());
        if (result.getTotal() < 1) return new ArrayList<>();
        //TODO: Kozel, use something that ain't deprecated.
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
    @Override
    public <T extends Indexable> T index(T document, String index, String type) throws ArchiveException {
        Index indexRequest = new Index.Builder(document)
                .index(index)
                .type(type)
                .setParameter(Parameters.REFRESH, true)
                .build();
        log.log(Level.FINE, "Indexing ioc [" + document.toString() + "]");

        JestResult result;
        try {
            result = elasticClient.execute(indexRequest);
        } catch (Exception e) {
            throw new ArchiveException("Indexing IoC went wrong.", e);
        }

        if (result.isSucceeded()) {
            String docId = result.getJsonObject().getAsJsonPrimitive("_id").getAsString();
            document.setDocumentId(docId);
            log.log(Level.FINE, "Indexed ioc: [" + document.toString() + "]");
        } else {
            log.severe("Archive returned error: " + result.getErrorMessage());
            throw new ArchiveException(result.getErrorMessage());
        }
        return document;
    }

    /**
     * Stores object that implements Indexable interface
     *
     * @param documentId object being stored
     * @param index    Document index
     * @param type     Document type
     * //@param <T>      Class of object being stored
     * @return indexed object
     * @throws ArchiveException when communication with Elastic Search server went wrong
     */
    @Override
    public boolean update(String documentId, String script, String index, String type) throws ArchiveException {

        Update updateRequest = new Update.Builder(script)
                .index(index)
                .type(type)
                .id(documentId)
                .setParameter(Parameters.REFRESH, true)         // immediate refresh
                .setParameter(Parameters.RETRY_ON_CONFLICT, 5)  // 5 tries in case of conflict
                .build();

        JestResult result;
        try {
            result = elasticClient.execute(updateRequest);
        } catch (Exception e) {
            String message;
            if (e.getCause() != null) {
                message = e.getCause().getMessage();
            } else {
                message = e.getMessage();
            }
            throw new ArchiveException("Elastic upsert went wrong: " + message, e);
        }

        if (result.isSucceeded()) {
            String docId = result.getJsonObject().getAsJsonPrimitive("_id").getAsString();
            log.info("Indexed docId [" + docId + "]");
        } else {
            log.severe("Archive returned error: " + result.getErrorMessage());
            throw new ArchiveException(result.getErrorMessage());
        }
        return result.isSucceeded();
    }

    @Override
    public <T extends Indexable> boolean delete(T document, String index, String type) throws ArchiveException {

        JestResult result;
        try {
            result = elasticClient.execute(new Delete.Builder(document.getDocumentId())
                    .index(index)
                    .type(type)
                    .setParameter(Parameters.REFRESH, true)         // immediate refresh
                    .build());
        } catch (Exception e) {

            String message;
            if (e.getCause() != null) {
                message = e.getCause().getMessage();
            } else {
                message = e.getMessage();
            }
            throw new ArchiveException("Elastic delete went wrong: " + message, e);
        }

        if (result.isSucceeded()) {
            String docId = result.getJsonObject().getAsJsonPrimitive("_id").getAsString();
            log.info("Deleted docId [" + docId + "]");
        } else {
            log.severe("Archive returned error: " + result.getErrorMessage());
            throw new ArchiveException(result.getErrorMessage());
        }

        return result.isSucceeded();
    }
}
