package biz.karms.sinkit.ejb.elastic;

import io.searchbox.client.JestClient;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

/**
 * Created by tkozel on 4.8.15.
 */
@Dependent
public class ElasticResources {

    @Inject
    JestClientProvider jestClientProvider;

    @Produces
    @Default
    JestClient getJestClient() {
        return jestClientProvider.getJestClient();
    }
}
