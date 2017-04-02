package biz.karms.sinkit.eventlog;

import java.io.Serializable;

/**
 * @author Tomas Kozel
 */
public class EventReason implements Serializable {

    private static final long serialVersionUID = 8092255064427923820L;

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
