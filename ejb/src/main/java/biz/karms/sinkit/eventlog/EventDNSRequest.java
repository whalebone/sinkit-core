package biz.karms.sinkit.eventlog;

/**
 * Created by tkozel on 23.7.15.
 */
public class EventDNSRequest {

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
