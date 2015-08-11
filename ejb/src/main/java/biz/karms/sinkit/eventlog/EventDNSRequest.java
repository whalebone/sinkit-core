package biz.karms.sinkit.eventlog;

import java.io.Serializable;

/**
 * Created by tkozel on 23.7.15.
 */
public class EventDNSRequest implements Serializable {

    private static final long serialVersionUID = -8270442960082815073L;

    private String ip;
    private String raw;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getRaw() {
        return raw;
    }

    public void setRaw(String raw) {
        this.raw = raw;
    }

    @Override
    public String toString() {
        return "EventDNSRequest{" +
                "ip='" + ip + '\'' +
                ", raw='" + raw + '\'' +
                '}';
    }
}
