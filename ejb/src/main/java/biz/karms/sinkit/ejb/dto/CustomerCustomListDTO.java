package biz.karms.sinkit.ejb.dto;

import java.io.Serializable;
import java.util.HashMap;

/**
 * @author Michal Karm Babacek
 *         <p>
 *         Set custom lists for customer PORTAL -> CORE
 *         Sets ip/domain blacklist/whitelist of customer - changes all custom list records for one customer
 *         block, log, allow lists for each DNS client
 *         customer_id - integer
 *         cidr - <ip>/<mask_length>
 *         dns - ([a-z0-9_\-]+.)*\.[a-z0-9_\-], The ? represents a single character and * represents any character sequence.
 *         <p>
 *         PUT /sinkit/rest/lists/<customer_id>
 *         [
 *         {
 *         dns_client: “<cidr>”,
 *         lists [{“<cidr | dns>” : "<B | W | L>"}]
 *         },
 *         ...
 *         ]
 *         <p>
 *         dns - could contain * and ?
 *         B|W|L - only B|W implemented ATTOW
 */
public class CustomerCustomListDTO implements Serializable {

    private static final long serialVersionUID = 10123243266665691L;
    /**
     * "CIDR"
     */
    private String dnsClient;
    /**
     * [“<cidr | dns>” : "<B | W | L>", ...]
     */
    private HashMap<String, String> lists;

    public String getDnsClient() {
        return dnsClient;
    }

    public void setDnsClient(String dnsClient) {
        this.dnsClient = dnsClient;
    }

    public HashMap<String, String> getLists() {
        return lists;
    }

    public void setLists(HashMap<String, String> lists) {
        this.lists = lists;
    }
}
