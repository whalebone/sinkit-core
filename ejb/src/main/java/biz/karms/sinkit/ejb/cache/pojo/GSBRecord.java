package biz.karms.sinkit.ejb.cache.pojo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;

/**
 * @author Tomas Kozel
 */
@Getter
@Setter
public class GSBRecord implements Serializable {

    private static final long serialVersionUID = -8345339432357942885L;

    private String hashPrefix;
    private Calendar fullHashesExpireAt;
    private HashMap<String, HashSet<String>> fullHashes;

    public GSBRecord() {
    }

    public GSBRecord(String hashPrefix, Calendar fullHashesExpireAt, HashMap<String, HashSet<String>> fullHashes) {
        this.hashPrefix = hashPrefix;
        this.fullHashesExpireAt = fullHashesExpireAt;
        this.fullHashes = fullHashes;
    }

    @Override
    public int hashCode() {
        if (hashPrefix == null) return 0;
        return hashPrefix.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GSBRecord that = (GSBRecord) o;

        return this.hashPrefix.equals(that.hashPrefix);
    }
}
