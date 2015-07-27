package biz.karms.sinkit.ejb.dto;

import java.io.Serializable;
import java.util.HashMap;

/**
 * @author Michal Karm Babacek
 *         <p/>
 *         Send all DNS client settings PORTAL -> CORE
 *         Sends all existing DNS clients and their policy with settings for feeds.
 *         S - Sink, L - log, D - disabled - log only internally for statistics
 *         customer_id - integer
 *         <p/>
 *         POST /sinkit/rest/rules/all
 *         [
 *         {
 *         dns_client: “<cidr>”,
 *         customer_id: 1,
 *         settings: [
 *         {
 *         feed_uid: "new-test-feed",
 *         mode: "<L|S|D>"
 *         },
 *         {
 *         feed_uid: "new-test-feed2",
 *         mode: "S"
 *         }
 *         ]
 *         },
 *         ...
 *         ]
 */
public class AllDNSSettingDTO implements Serializable {

    private static final long serialVersionUID = 1012324324325691L;
    /**
     * "CIDR"
     */
    private String dnsClient;
    private int customerId;
    /**
     * ["new-test-feed" : "<L|S|D>", ...]
     */
    private HashMap<String, String> settings;

    public String getDnsClient() {
        return dnsClient;
    }

    public void setDnsClient(String dnsClient) {
        this.dnsClient = dnsClient;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public HashMap<String, String> getSettings() {
        return settings;
    }

    public void setSettings(HashMap<String, String> settings) {
        this.settings = settings;
    }
}
