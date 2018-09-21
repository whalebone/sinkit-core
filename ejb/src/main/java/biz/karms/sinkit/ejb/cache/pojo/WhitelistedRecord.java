package biz.karms.sinkit.ejb.cache.pojo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Calendar;

/**
 * @author Tomas Kozel
 */
@Getter
@Setter
public class WhitelistedRecord implements Serializable {

    private static final long serialVersionUID = -562033533677059L;
    private String rawId;
    private String sourceName;
    private Calendar expiresAt;
    private boolean completed;

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
