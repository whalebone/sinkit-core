package biz.karms.sinkit.ejb.dto;

import java.io.Serializable;

/**
 * @author Michal Karm Babacek
 */
public class Sinkhole implements Serializable {

    private static final long serialVersionUID = 109238475691L;
    /**
     * Could be either IPv4 or IPv6 address.
     */
    private String sinkhole;

    // If there is any need for adding TXT info to DNS response, we add it here.

    public Sinkhole(String sinkhole) {
        this.sinkhole = sinkhole;
    }

    public String getSinkhole() {
        return sinkhole;
    }

    public void setSinkhole(String sinkhole) {
        this.sinkhole = sinkhole;
    }
}
