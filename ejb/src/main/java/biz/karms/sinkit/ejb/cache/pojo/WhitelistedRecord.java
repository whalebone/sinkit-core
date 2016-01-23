package biz.karms.sinkit.ejb.cache.pojo;

import org.hibernate.search.annotations.*;

import javax.persistence.Entity;
import java.io.Serializable;
import java.util.Calendar;

/**
 * Created by tkozel on 1/9/16.
 */
//@Indexed(index = "WhitelistedRecord")
//@Entity
public class WhitelistedRecord  implements Serializable {

    private static final long serialVersionUID = -4530501033533677059L;

    // @Field(index = Index.YES, analyze = Analyze.NO)
    String rawId;

    // @Field(index = Index.YES, analyze = Analyze.NO)
    String sourceName;

    // @Field
    // @CalendarBridge(resolution = Resolution.SECOND)
    Calendar expiresAt;

    boolean completed;

    public Calendar getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Calendar expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getRawId() {
        return rawId;
    }

    public void setRawId(String rawId) {
        this.rawId = rawId;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WhitelistedRecord that = (WhitelistedRecord) o;

        return !(rawId != null ? !rawId.equals(that.rawId) : that.rawId != null);

    }

    @Override
    public int hashCode() {
        return rawId != null ? rawId.hashCode() : 0;
    }
}
