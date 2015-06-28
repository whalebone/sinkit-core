package biz.karms.sinkit.ioc;

import com.google.gson.annotations.SerializedName;
import io.searchbox.annotations.JestId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.util.Calendar;

/**
 * Created by tkozel on 24.6.15.
 */
@Indexed
public class IoCRecord implements Serializable {

    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";

    private static final long serialVersionUID = 2184815523047755697L;

    @JestId
    private transient String documentId;

    @Field
    private IoCFeed feed;

    @Field
    private IoCDescription description;

    @Field
    private IoCClassification classification;

    @Field
    private IoCProtocol protocol;

    @Field
    private String raw;

    @Field
    private IoCSource source;

    @Field
    private IoCTime time;

    @Field
    private IoCSeen seen;

    @Field
    private boolean active;

    public IoCRecord() {}

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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
                '}';
    }
}
