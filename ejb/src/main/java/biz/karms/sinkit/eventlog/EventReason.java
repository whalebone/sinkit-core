package biz.karms.sinkit.eventlog;

import java.io.Serializable;

/**
 * Created by tkozel on 23.7.15.
 */
public class EventReason implements Serializable {

    private String fqdn;
    private String ip;

    public String getFqdn() {
        return fqdn;
    }

    public void setFqdn(String fqdn) {
        this.fqdn = fqdn;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    @Override
    public String toString() {
        return "EventReason{" +
                "fqdn='" + fqdn + '\'' +
                ", ip='" + ip + '\'' +
                '}';
    }
}
