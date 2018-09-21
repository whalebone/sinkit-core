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
 * @author Tomas Kozel
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
                        .settings(ImmutableSettings.settingsBuilder()
                                .put("http.enabled", false)
                                .put("network.host", "_" + System.getenv("SINKIT_NIC") + ":ipv4_")
                                .putArray("discovery.zen.ping.unicast.hosts", System.getenv("SINKIT_ELASTIC_HOST") + ":" + System.getenv("SINKIT_ELASTIC_PORT"))
                                .put("cluster.name", System.getenv("SINKIT_ELASTIC_CLUSTER"))
                                .put("discovery.zen.ping.multicast.enabled", true)
                                .put("discovery.zen.ping.timeout", "3s")
                                .put("discovery.zen.minimum_master_nodes", 1)
                        )
                        .client(true)
                        .data(false)
                        .node();
            }
            client = node.client();
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
    }
}
