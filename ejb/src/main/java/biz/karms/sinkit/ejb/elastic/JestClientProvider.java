package biz.karms.sinkit.ejb.elastic;

import javax.enterprise.context.ApplicationScoped;

/**
 * @author Tomas Kozel
 */
@ApplicationScoped
public class JestClientProvider {

//    @Inject
//    private Logger log;
//
//    private JestClient jestClient;
//
//    public JestClient getJestClient() {
//
//        if (this.jestClient == null) {
//            log.log(Level.INFO, "JestClient does not exist - constructing a new one");
//            final String elasticHost = System.getenv("SINKIT_ELASTIC_HOST");
//            final String elasticPort = System.getenv("SINKIT_ELASTIC_PORT");
//            if (elasticHost == null || elasticPort == null) {
//                log.log(Level.SEVERE, "SINKIT_ELASTIC_HOST and SINKIT_ELASTIC_PORT must be set.");
//            }
//            JestClientFactory factory = new JestClientFactory();
//            factory.setHttpClientConfig(new HttpClientConfig
//                    .Builder("http://" + elasticHost + ":" + elasticPort)
//                    .multiThreaded(true)
//                    .connTimeout(10000)
//                    .readTimeout(10000)
//                    .build());
//
//            this.jestClient = factory.getObject();
//        }
//
//        return jestClient;
//    }
}
