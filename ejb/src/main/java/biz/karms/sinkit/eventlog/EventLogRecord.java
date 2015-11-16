package biz.karms.sinkit.eventlog;

import biz.karms.sinkit.ejb.elastic.Indexable;
import biz.karms.sinkit.ioc.IoCRecord;
import com.google.gson.annotations.SerializedName;
import io.searchbox.annotations.JestId;

import java.util.Arrays;
import java.util.Date;

/**
 * Created by tkozel on 23.7.15.
 */
public class EventLogRecord implements Indexable {

    private static final long serialVersionUID = 423449239443309837L;

    @JestId
    private transient String documentId;

    private EventLogAction action;
    private String client;
    private EventDNSRequest request;
    private EventReason reason;
    private Date logged;

    @SerializedName("virus_total_request")
    private VirusTotalRequest virusTotalRequest;

    @SerializedName("matched_iocs")
    private IoCRecord[] matchedIocs;

    @Override
    public String getDocumentId() {
        return documentId;
    }

    @Override
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

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

    public VirusTotalRequest getVirusTotalRequest() {
        return virusTotalRequest;
    }

    public void setVirusTotalRequest(VirusTotalRequest virusTotalRequest) {
        this.virusTotalRequest = virusTotalRequest;
    }

    public IoCRecord[] getMatchedIocs() {
        return matchedIocs;
    }

    public void setMatchedIocs(IoCRecord[] matchedIocs) {
        this.matchedIocs = matchedIocs;
    }

    @Override
    public String toString() {
        return "EventLogRecord{" +
                "documentId='" + documentId + '\'' +
                ", action=" + action +
                ", client='" + client + '\'' +
                ", request=" + request +
                ", reason=" + reason +
                ", logged=" + logged +
                ", virusTotalRequest=" + virusTotalRequest +
                ", matchedIocs=" + Arrays.toString(matchedIocs) +
                '}';
    }
}
