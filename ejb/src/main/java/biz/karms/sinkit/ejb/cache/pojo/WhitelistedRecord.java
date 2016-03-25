package biz.karms.sinkit.ejb.cache.pojo;

import java.io.Serializable;
import java.util.Calendar;

/**
 * @author Tomas Kozel
 */
public class WhitelistedRecord implements Serializable {

    private static final long serialVersionUID = -562033533677059L;
    private String rawId;
    private String sourceName;
    private Calendar expiresAt;
    private boolean completed;

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
