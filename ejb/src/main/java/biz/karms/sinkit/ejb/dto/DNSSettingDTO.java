package biz.karms.sinkit.ejb.dto;

import biz.karms.sinkit.ejb.dto.support.FeedDTO;

import java.io.Serializable;
import java.util.List;

/**
 * @author Michal Karm Babacek
 *         <p/>
 *         Get all DNS settings CORE -> PORTAL
 *         Returns all existing feed settings for DNS clients of all customers
 *         GET /api/v1/settings
 *         {
 *         feeds:  [
 *         {
 *         feed_uid: "new-test-feed",
 *         dns_clients: [
 *         {
 *         dns_client: “<cidr>”,
 *         mode: "<L|S|D>",
 *         customer_id: <customer_id>
 *         },
 *         ...
 *         ]
 *         },
 *         ...
 *         ],
 *         status: "ok",
 *         }
 */
public class DNSSettingDTO implements Serializable {

    private static final long serialVersionUID = 8656399324324325691L;
    private List<FeedDTO> feeds;
    private String status;

    public List<FeedDTO> getFeeds() {
        return feeds;
    }

    public void setFeeds(List<FeedDTO> feeds) {
        this.feeds = feeds;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
