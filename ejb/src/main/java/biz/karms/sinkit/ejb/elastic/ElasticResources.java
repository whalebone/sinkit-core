package biz.karms.sinkit.ejb.elastic;

import com.google.gson.Gson;
import org.elasticsearch.client.Client;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

/**
 * Created by Tomas Kozel
 */
@Dependent
public class ElasticResources {

    public static final String ELASTIC_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

    @Inject
    private ElasticClientProvider elasticClientProvider;

    @Inject
    private GsonProvider gsonProvider;

    @Produces
    @Default
    public Client getElasticClient() {
        return elasticClientProvider.getClient();
    }

    @Produces
    @Default
    public Gson getGson() {
        return gsonProvider.getGson();
    }
}
