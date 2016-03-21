package biz.karms.sinkit.ejb.elastic;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Tomas Kozel
 */
@ApplicationScoped
public class ElasticClientProvider {

    @Inject
    private Logger log;

    private Client client;
    private Node node;

    public Client getClient() {
        if (client == null) {
            log.log(Level.INFO, "Elastic client doesn't exists, creating new one");
            if (node == null) {
                log.log(Level.INFO, "Elastic client node doesn't exists, creating new one");
                node = NodeBuilder.nodeBuilder()
                        .settings(ImmutableSettings.settingsBuilder().put("http.enabled", false))
                        .client(true)
                        .data(false)
                        .node();
            }
            client = node.client();
//            client = new TransportClient()
//                    .addTransportAddress(new InetSocketTransportAddress("localhost", 9300));
        }
        return client;
    }

    @PreDestroy
    public void shutDown() {
        if (client != null) {
            log.log(Level.INFO, "Closing elastic client");
            client.close();
        }
        if (node != null) {
            log.log(Level.INFO, "Closing elastic client node");
            node.close();
        }
//        if (client != null) {
//            client.close();
//        }
    }
}
