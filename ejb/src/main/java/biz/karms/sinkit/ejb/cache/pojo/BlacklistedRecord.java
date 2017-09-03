package biz.karms.sinkit.ejb.cache.pojo;

import com.google.gson.Gson;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;

/**
 * @author Michal Karm Babacek
 */
public class BlacklistedRecord implements Serializable {

    private static final long serialVersionUID = -8738664627706834541L;

    /**
     * Stored as an MD5 hash
     */
    private String blackListedDomainOrIP;

    private Calendar listed;

    private Boolean presentOnWhiteList;

    /**
     * Feed : {Type, IoCID}
     */
    private HashMap<String, ImmutablePair<String, String>> sources;

    /**
     * Feed : {Source name : non-negative integer}
     */
    private HashMap<String, HashMap<String, Integer>> accuracy;

    public BlacklistedRecord(String blackListedDomainOrIP, Calendar listed, HashMap<String, ImmutablePair<String, String>> sources, HashMap<String, HashMap<String, Integer>> accuracy, Boolean presentOnWhiteList) {
        this.blackListedDomainOrIP = blackListedDomainOrIP;
        this.listed = listed;
        this.sources = sources;
        this.accuracy = accuracy;
        this.presentOnWhiteList = presentOnWhiteList;
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

    public HashMap<String, ImmutablePair<String, String>> getSources() {
        return sources;
    }

    public void setSources(HashMap<String, ImmutablePair<String, String>> sources) {
        this.sources = sources;
    }

    public HashMap<String, HashMap<String, Integer>> getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(HashMap<String, HashMap<String, Integer>> accuracy) {
        this.accuracy = accuracy;
    }

    public Boolean isPresentOnWhiteList() {
        return presentOnWhiteList;
    }

    public void setPresentOnWhiteList(Boolean presentOnWhiteList) {
        this.presentOnWhiteList = presentOnWhiteList;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
