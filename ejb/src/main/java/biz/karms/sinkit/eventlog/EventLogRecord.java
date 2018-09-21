package biz.karms.sinkit.eventlog;

import biz.karms.sinkit.ejb.elastic.Indexable;
import biz.karms.sinkit.ioc.IoCRecord;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.Date;

/**
 * @author Tomas Kozel
 */
public class EventLogRecord implements Indexable {

    private static final long serialVersionUID = 423449239443309837L;

    //@JestId
    private transient String documentId;

    private EventLogAction action;
    private String client;
    private EventDNSRequest request;
    private EventReason reason;
    private Date logged;
    private Accuracy accuracy;
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

    public IoCRecord[] getMatchedIocs() {
        return matchedIocs;
    }

    public void setMatchedIocs(IoCRecord[] matchedIocs) {
        this.matchedIocs = matchedIocs;
    }

    public Accuracy getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Accuracy accuracy) {
        this.accuracy = accuracy;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
