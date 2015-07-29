package biz.karms.sinkit.ejb.dto.support;

import java.io.Serializable;

/**
 * @author Michal Karm Babacek
 */
public class DNSClientDTO implements Serializable {

    private static final long serialVersionUID = 7656399324324325691L;
    /**
     * "CIDR"
     */
    private String dnsClient;
    /**
     * "<L|S|D>"
     */
    private String mode;
    private int customerId;

    public String getDnsClient() {
        return dnsClient;
    }

    public void setDnsClient(String dnsClient) {
        this.dnsClient = dnsClient;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }
}
