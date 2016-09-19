package biz.karms.sinkit.ejb.dto.support;

import java.io.Serializable;
import java.util.List;

/**
 * @author Michal Karm Babacek
 */
public class FeedDTO implements Serializable {

    private static final long serialVersionUID = 8656399324324325691L;
    /**
     * "new-test-feed"
     */
    private String feedUid;
    private List<DNSClientDTO> dnsClients;

    public String getFeedUid() {
        return feedUid;
    }

    public void setFeedUid(String feedUid) {
        this.feedUid = feedUid;
    }

    public List<DNSClientDTO> getDnsClients() {
        return dnsClients;
    }

    public void setDnsClients(List<DNSClientDTO> dnsClients) {
        this.dnsClients = dnsClients;
    }
}
