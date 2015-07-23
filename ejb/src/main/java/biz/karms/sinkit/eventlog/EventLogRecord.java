package biz.karms.sinkit.eventlog;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;

/**
 * Created by tkozel on 23.7.15.
 */
public class EventLogRecord implements Serializable {

    private EventLogAction action;
    private String client;
    private EventDNSRequest request;
    private EventReason reason;
    private Date logged;

    @SerializedName("matched_iocs")
    private String[] matchedIocs;

    public EventLogAction getAction() {
        return action;
    }

    public void setAction(EventLogAction action) {
        this.action = action;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public EventDNSRequest getRequest() {
        return request;
    }

    public void setRequest(EventDNSRequest request) {
        this.request = request;
    }

    public EventReason getReason() {
        return reason;
    }

    public void setReason(EventReason reason) {
        this.reason = reason;
    }

    public Date getLogged() {
        return logged;
    }

    public void setLogged(Date logged) {
        this.logged = logged;
    }

    public String[] getMatchedIocs() {
        return matchedIocs;
    }

    public void setMatchedIocs(String[] matchedIocs) {
        this.matchedIocs = matchedIocs;
    }

    @Override
    public String toString() {
        return "EventLogRecord{" +
                "action=" + action +
                ", client='" + client + '\'' +
                ", request=" + request +
                ", reason=" + reason +
                ", logged=" + logged +
                ", matchedIocs=" + Arrays.toString(matchedIocs) +
                '}';
    }
}
