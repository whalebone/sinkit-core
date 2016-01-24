package biz.karms.sinkit.ejb.cache.pojo;

import org.jboss.marshalling.Pair;

import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;

/**
 * @author Michal Karm Babacek
 */
public class BlacklistedRecord implements Serializable {

    private static final long serialVersionUID = 2184999923427771L;

    private String blackListedDomainOrIP;

    private Calendar listed;

    /**
     * Feed : {Type, IoCID}
     */
    private HashMap<String, Pair<String, String>> sources;

    public BlacklistedRecord(String blackListedDomainOrIP, Calendar listed, HashMap<String, Pair<String, String>> sources) {
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

    public HashMap<String, Pair<String, String>> getSources() {
        return sources;
    }

    public void setSources(HashMap<String, Pair<String, String>> sources) {
        this.sources = sources;
    }

}
