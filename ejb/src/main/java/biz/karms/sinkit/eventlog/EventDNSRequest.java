package biz.karms.sinkit.eventlog;

import java.io.Serializable;

/**
 * Created by tkozel on 23.7.15.
 */
public class EventDNSRequest implements Serializable {

    private static final long serialVersionUID = -8270442960082815073L;

    private String ip;
    private String fqdn;
    private String type;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getFqdn() {
        return fqdn;
    }

    public void setFqdn(String fqdn) {
        this.fqdn = fqdn;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "EventDNSRequest{" +
                "ip='" + ip + '\'' +
                ", fqdn='" + fqdn + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
