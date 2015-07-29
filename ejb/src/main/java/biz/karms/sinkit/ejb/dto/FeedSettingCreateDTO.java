package biz.karms.sinkit.ejb.dto;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

/**
 * @author Michal Karm Babacek
 *         <p/>
 *         Create feed settings PORTAL -> CORE
 *         When new feed is created - default settings from customer policies are created
 *         customer_id - integer
 *         customer_name - string [a-z0-9 _\-\.]
 *         <p/>
 *         POST /sinkit/rest/feed/create
 *         {
 *         feed_uid: <feed_uid>,
 *         settings: [
 *         {
 *         customer_id: <customer_id>,
 *         customer_name: <customer_name>,
 *         dns_clients: [
 *         {
 *         dns_client: “<cidr>”,
 *         mode: <L|S|D>,
 *         },
 *         …
 *         ]
 *         },
 *         …
 *         ]
 *         }
 */
public class FeedSettingCreateDTO implements Serializable {

    private static final long serialVersionUID = 1012324324335691L;
    /**
     * "CIDR"
     */
    private String feedUid;
    /**
     * {"customerId" : {"dns_client <cidr>" : "mode <L|S|D>", ...}}
     */
    private HashMap<Integer, HashMap<String, String>> settings;

    public String getFeedUid() {
        return feedUid;
    }

    public HashMap<Integer, HashMap<String, String>> getSettings() {
        return settings;
    }
}
