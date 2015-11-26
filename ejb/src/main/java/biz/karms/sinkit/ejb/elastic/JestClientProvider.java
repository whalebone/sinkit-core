package biz.karms.sinkit.ejb.elastic;

import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by tkozel on 24.6.15.
 */
@ApplicationScoped
public class JestClientProvider {

    @Inject
    private Logger log;

    private JestClient jestClient;

    public JestClient getJestClient() {

        if (this.jestClient == null) {
            log.log(Level.INFO, "JestClient does not exist - constructing a new one");
            final String elasticHost = System.getenv("SINKIT_ELASTIC_HOST");
            final String elasticPort = System.getenv("SINKIT_ELASTIC_PORT");
            if (elasticHost == null || elasticPort == null) {
                log.log(Level.SEVERE, "SINKIT_ELASTIC_HOST and SINKIT_ELASTIC_PORT must be set.");
            }
            JestClientFactory factory = new JestClientFactory();
            factory.setHttpClientConfig(new HttpClientConfig
                    .Builder("http://" + elasticHost + ":" + elasticPort)
                    .multiThreaded(true)
                    .build());

            this.jestClient = factory.getObject();
        }

        return jestClient;
    }
}
