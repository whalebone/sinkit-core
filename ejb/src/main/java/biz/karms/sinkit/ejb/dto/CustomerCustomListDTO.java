package biz.karms.sinkit.ejb.dto;

import java.io.Serializable;
import java.util.List;

/**
 * @author Michal Karm Babacek
 *         <p/>
 *         Set custom lists for customer PORTAL -> CORE
 *         Sets ip/domain blacklist/whitelist of customer - changes all custom list records for one customer
 *         block, log, allow lists for each DNS client
 *         customer_id - integer
 *         cidr - <ip>/<mask_length>
 *         dns - ([a-z0-9_\-]+.)*\.[a-z0-9_\-]
 *         <p/>
 *         PUT /sinkit/rest/lists/<customer_id>
 *         [
 *         {
 *         dns_client: “<cidr>”,
 *         block: [“<cidr | dns>”,
 *         ...
 *         ],
 *         log: [“<cidr | dns>”,
 *         ...
 *         ],
 *         allow: [“<cidr | dns>”,
 *         ...
 *         ]
 *         },
 *         ...
 *         ]
 */
public class CustomerCustomListDTO implements Serializable {

    private static final long serialVersionUID = 1012324324325691L;
    /**
     * "CIDR"
     */
    private String dnsClient;
    private List<String> block;
    private List<String> log;
    private List<String> allow;

    public String getDnsClient() {
        return dnsClient;
    }

    public void setDnsClient(String dnsClient) {
        this.dnsClient = dnsClient;
    }

    public List<String> getBlock() {
        return block;
    }

    public void setBlock(List<String> block) {
        this.block = block;
    }

    public List<String> getLog() {
        return log;
    }

    public void setLog(List<String> log) {
        this.log = log;
    }

    public List<String> getAllow() {
        return allow;
    }

    public void setAllow(List<String> allow) {
        this.allow = allow;
    }
}
