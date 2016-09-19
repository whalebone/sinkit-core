package biz.karms.sinkit.ejb.elastic.impl;

import biz.karms.sinkit.ejb.elastic.ElasticService;
import biz.karms.sinkit.ejb.elastic.Indexable;
import biz.karms.sinkit.exception.ArchiveException;
import com.google.gson.Gson;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortBuilder;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Tomas Kozel
 */
@Stateless
public class ElasticNativeServiceEJB implements ElasticService {

    private static final int DEF_LIMIT = 1000;

    @Inject
    private Logger log;

    @Inject
    private Client client;

    @Inject
    private Gson gson;

    @PostConstruct
    public void setup() {
        if (client == null) {
            throw new IllegalArgumentException("Client must be injected.");
        }
    }

    @Override
    public <T extends Indexable> T getDocumentById(String id, String index, String type, Class<T> clazz) throws ArchiveException {
        try {
            GetResponse response = client.prepareGet(index, type, id)
                    .execute()
                    .actionGet();
            if (!response.isExists()) {
                log.log(Level.WARNING, "Can't get document with id:  " + id + ". Document doesn't exists.");
                return null;
            }
            if (response.isSourceEmpty()) {
                log.log(Level.WARNING, "Can't get document with id:  " + id + ". Elastic returned empty source.");
                return null;
            }
            T document = gson.fromJson(response.getSourceAsString(), clazz);
            document.setDocumentId(response.getId());
            return document;
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Getting document by id from elastic failed: " + ex.getMessage());
            ex.printStackTrace();
            throw new ArchiveException("Getting object by id failed", ex);
        }
    }

    @Override
    public <T extends Indexable> List<T> search(QueryBuilder query, SortBuilder sort, String index, String type, Class<T> clazz) throws ArchiveException {
        return search(query, sort, index, type, 0, DEF_LIMIT, clazz);
    }

    @Override
    public <T extends Indexable> List<T> search(final QueryBuilder query,
                                                final SortBuilder sort,
                                                final String index,
                                                final String type,
                                                final int from,
                                                final int size,
                                                final Class<T> clazz) throws ArchiveException {
        try {
            final SearchRequestBuilder search = client.prepareSearch(index)
                    .setQueryCache(false)
                    .setExplain(true)
                    .setTypes(type)
                    .setFrom(from)
                    .setSize(size)
                    .setQuery(query)
                    .setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
            if (sort != null) {
                search.addSort(sort);
            }

            final SearchResponse response = search.execute()
                    .actionGet();
            if (response.isTimedOut()) {
                throw new ArchiveException("Elastic search timed out");
            }
//            if (response.isTerminatedEarly()) {
//                throw new ArchiveException("Elastic search has been terminated early");
//            }
            if (!RestStatus.OK.equals(response.status())) {
                throw new ArchiveException("Elastic search has not ended successfully: " + response.status());
            }
            if (response.getHits() == null || response.getHits().totalHits() == 0) {
                return new ArrayList<>();
            } else {
                List<T> docs = new ArrayList<>((int) response.getHits().getTotalHits());
                for (SearchHit hit : response.getHits()) {
                    T doc = gson.fromJson(hit.sourceAsString(), clazz);
                    doc.setDocumentId(hit.getId());
                    docs.add(doc);
                }
                return docs;
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Elastic search has failed: " + ex.getMessage());
            ex.printStackTrace();
            throw new ArchiveException("Elastic search has failed", ex);
        }
    }

    @Override
    public <T extends Indexable> T index(final T document, String index, String type) throws ArchiveException {
        try {
            final IndexResponse response = client.prepareIndex(index, type)
                    .setId(document.getDocumentId()) // can be null in that case is id created
                    .setRefresh(true)
                    .setSource(gson.toJson(document))
                    .execute()
                    .actionGet();
            document.setDocumentId(response.getId());
            return document;
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Indexing document failed: " + ex.getMessage());
            ex.printStackTrace();
            throw new ArchiveException("Indexing document failed", ex);
        }
    }

    @Override
    public boolean update(String documentId, Object updateDoc, String index, String type, Object upsertDoc) throws ArchiveException {
        try {
            UpdateRequestBuilder update = client.prepareUpdate(index, type, documentId)
                    .setDoc(gson.toJson(updateDoc))
                    .setRefresh(true)
                    .setRetryOnConflict(5);
            if (upsertDoc != null) {
                IndexRequest upsert = new IndexRequest(index, type, documentId)
                        .source(gson.toJson(upsertDoc));
                update.setUpsert(upsert);
            }
            update.execute().actionGet();
            return true;
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Upsert went wrong: " + ex.getMessage());
            ex.printStackTrace();
            throw new ArchiveException("Upsert went wrong: " + ex.getMessage());
        }
    }

    @Override
    public <T extends Indexable> boolean delete(final T document, String index, String type) throws ArchiveException {
        try {
            final DeleteResponse response = client.prepareDelete(index, type, document.getDocumentId())
                    .setRefresh(true)
                    .execute()
                    .actionGet();
            return response.isFound();
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Delete went wrong: " + ex.getMessage());
            ex.printStackTrace();
            throw new ArchiveException("Delete went wrong: " + ex.getMessage());
        }
    }
}
