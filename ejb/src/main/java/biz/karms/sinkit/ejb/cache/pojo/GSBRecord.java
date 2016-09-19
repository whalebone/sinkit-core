package biz.karms.sinkit.ejb.cache.pojo;

import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by tom on 11/27/15.
 *
 * @author Tomas Kozel
 */

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

    public String getHashPrefix() {
        return hashPrefix;
    }

    public void setHashPrefix(String hashPrefix) {
        this.hashPrefix = hashPrefix;
    }

    public Calendar getFullHashesExpireAt() {
        return fullHashesExpireAt;
    }

    public void setFullHashesExpireAt(Calendar fullHashesExpireAt) {
        this.fullHashesExpireAt = fullHashesExpireAt;
    }

    public HashMap<String, HashSet<String>> getFullHashes() {
        return fullHashes;
    }

    public void setFullHashes(HashMap<String, HashSet<String>> fullHashes) {
        this.fullHashes = fullHashes;
    }
}
