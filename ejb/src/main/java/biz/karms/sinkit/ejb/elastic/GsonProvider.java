package biz.karms.sinkit.ejb.elastic;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Tomas Kozel
 */
@ApplicationScoped
public class GsonProvider {

    @Inject
    private Logger log;

    private Gson gson;

    public Gson getGson() {
        if (gson == null) {
            log.log(Level.INFO, "Gson doesn't exist, creating new one");
            gson = new GsonBuilder()
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                    .setDateFormat(ElasticResources.ELASTIC_DATE_FORMAT)
                    .create();
        }
        return gson;
    }
}
