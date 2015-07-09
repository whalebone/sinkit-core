package biz.karms.sinkit.ejb;

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
public class JestClientProvider {

    @Inject
    private Logger log;

    private JestClient jestClient;

    public JestClient getJestClient() {

        if (this.jestClient == null) {
            log.info("\n\n JestClient does not exist - constructing a new one\n\n");

            JestClientFactory factory = new JestClientFactory();
            factory.setHttpClientConfig(new HttpClientConfig
                    .Builder("http://46.101.181.215:9200")
                    .multiThreaded(true)
                    .build()) ;


            this.jestClient = factory.getObject();
        }

        return jestClient;
    }
}
