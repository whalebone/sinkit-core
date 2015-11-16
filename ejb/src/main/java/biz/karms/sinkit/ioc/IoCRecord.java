package biz.karms.sinkit.ioc;

import biz.karms.sinkit.ejb.elastic.Indexable;
import com.google.gson.annotations.SerializedName;
import io.searchbox.annotations.JestId;

import java.util.Arrays;

/**
 * Created by tkozel on 24.6.15.
 */
public class IoCRecord implements Indexable {

    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";

    private static final long serialVersionUID = -595246622767555283L;

    @JestId
    @SerializedName("document_id")
    private String documentId;
    @SerializedName("unique_ref")
    private String uniqueRef;
    private IoCFeed feed;
    private IoCDescription description;
    private IoCClassification classification;
    private IoCProtocol protocol;
    private String raw;
    private IoCSource source;
    private IoCTime time;
    private IoCSeen seen;
    private Boolean active;

    @SerializedName("virus_total_reports")
    private IoCVirusTotalReport[] virusTotalReports;

    public IoCRecord() {}

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getUniqueRef() {
        return uniqueRef;
    }

    public void setUniqueRef(String uniqueRef) {
        this.uniqueRef = uniqueRef;
    }

    public IoCFeed getFeed() {
        return feed;
    }

    public void setFeed(IoCFeed feed) {
        this.feed = feed;
    }

    public IoCDescription getDescription() {
        return description;
    }

    public void setDescription(IoCDescription description) {
        this.description = description;
    }

    public IoCClassification getClassification() {
        return classification;
    }

    public void setClassification(IoCClassification classification) {
        this.classification = classification;
    }

    public IoCProtocol getProtocol() {
        return protocol;
    }

    public void setProtocol(IoCProtocol protocol) {
        this.protocol = protocol;
    }

    public String getRaw() {
        return raw;
    }

    public void setRaw(String raw) {
        this.raw = raw;
    }

    public IoCSource getSource() {
        return source;
    }

    public void setSource(IoCSource source) {
        this.source = source;
    }

    public IoCTime getTime() {
        return time;
    }

    public void setTime(IoCTime time) {
        this.time = time;
    }

    public IoCSeen getSeen() {
        return seen;
    }

    public void setSeen(IoCSeen seen) {
        this.seen = seen;
    }

    public Boolean isActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public IoCVirusTotalReport[] getVirusTotalReports() {
        return virusTotalReports;
    }

    public void setVirusTotalReports(IoCVirusTotalReport[] virusTotalReports) {
        this.virusTotalReports = virusTotalReports;
    }

    @Override
    public String toString() {
        return "IoCRecord{" +
                "documentId='" + documentId + '\'' +
                ", feed=" + feed +
                ", description=" + description +
                ", classification=" + classification +
                ", protocol=" + protocol +
                ", raw='" + raw + '\'' +
                ", source=" + source +
                ", time=" + time +
                ", seen=" + seen +
                ", active=" + active +
                ", virusTotalReports=" + Arrays.toString(virusTotalReports) +
                '}';
    }
}
