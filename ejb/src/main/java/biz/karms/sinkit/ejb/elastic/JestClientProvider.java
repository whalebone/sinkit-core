package biz.karms.sinkit.ejb.elastic;

import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;

import javax.ejb.Singleton;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.logging.Logger;

/**
 * Created by tkozel on 24.6.15.
 */
@ApplicationScoped
@Singleton
public class JestClientProvider {

    @Inject
    private Logger log;

    private JestClient jestClient;

    public JestClient getJestClient() {

        if (this.jestClient == null) {
            log.info("\n\n JestClient does not exist - constructing a new one\n\n");

            JestClientFactory factory = new JestClientFactory();
            factory.setHttpClientConfig(new HttpClientConfig
                    .Builder("http://" + System.getenv("SINKIT_ELASTIC_HOST") + ":" + System.getenv("SINKIT_ELASTIC_PORT"))
                    .multiThreaded(true)
                    .build()) ;


            this.jestClient = factory.getObject();
        }

        return jestClient;
    }
}
