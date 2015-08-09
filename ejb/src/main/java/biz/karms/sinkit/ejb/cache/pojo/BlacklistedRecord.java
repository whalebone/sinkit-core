package biz.karms.sinkit.ejb.cache.pojo;

import org.hibernate.search.annotations.*;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Map;

/**
 * @author Michal Karm Babacek
 */
@Indexed
public class BlacklistedRecord implements Serializable {

    private static final long serialVersionUID = 2184815523047755671L;

    @Field(analyze = Analyze.YES)
    private String blackListedDomainOrIP;

    @Field
    @CalendarBridge(resolution = Resolution.HOUR)
    private Calendar listed;

    /**
     * Feed : Type
     */
    @IndexedEmbedded
    private Map<String, String> sources;

    public BlacklistedRecord(String blackListedDomainOrIP, Calendar listed, Map<String, String> sources) {
        this.blackListedDomainOrIP = blackListedDomainOrIP;
        this.listed = listed;
        this.sources = sources;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlacklistedRecord)) return false;
        BlacklistedRecord that = (BlacklistedRecord) o;
        return blackListedDomainOrIP.equals(that.blackListedDomainOrIP);
    }

    @Override
    public int hashCode() {
        return blackListedDomainOrIP.hashCode();
    }

    public String getBlackListedDomainOrIP() {
        return blackListedDomainOrIP;
    }

    public void setBlackListedDomainOrIP(String blackListedDomainOrIP) {
        this.blackListedDomainOrIP = blackListedDomainOrIP;
    }

    public Calendar getListed() {
        return listed;
    }

    public void setListed(Calendar listed) {
        this.listed = listed;
    }

    public Map<String, String> getSources() {
        return sources;
    }

    public void setSources(Map<String, String> sources) {
        this.sources = sources;
    }
}
