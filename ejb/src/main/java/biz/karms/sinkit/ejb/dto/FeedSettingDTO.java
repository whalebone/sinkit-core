package biz.karms.sinkit.ejb.dto;

import biz.karms.sinkit.ejb.dto.support.DNSClientModeDTO;

import java.io.Serializable;
import java.util.List;

/**
 * @author Michal Karm Babacek
 *         <p/>
 *         Set feed settings PORTAL -> CORE
 *         When default feed mode is updated - all policies using default feed mode are changed
 *         Does not contain customer settings that does NOT use default feed mode
 *         <p/>
 *         customer_id - integer
 *         customer_name - string [a-z0-9 _\-\.]
 *         <p/>
 *         PUT /sinkit/rest/feed/<feed_uid>
 *         [
 *         {
 *         customer_id: <customer_id>,
 *         customer_name: <customer_name>,
 *         dns_clients: [
 *         {
 *         dns_client: "<cidr>",
 *         mode: <L|S|D>,
 *         },
 *         ...
 *         ]
 *         },
 *         ...
 *         ]
 */
public class FeedSettingDTO implements Serializable {

    private static final long serialVersionUID = 1012324324325691L;
    /**
     * "CIDR"
     */
    private int customerId;
    private String customerName;
    private List<DNSClientModeDTO> dnsClients;

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public List<DNSClientModeDTO> getDnsClients() {
        return dnsClients;
    }

    public void setDnsClients(List<DNSClientModeDTO> dnsClients) {
        this.dnsClients = dnsClients;
    }
}
