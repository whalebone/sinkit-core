package biz.karms.sinkit.ejb.cache.pojo;

import com.google.gson.Gson;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.HashMap;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * @author Michal Karm Babacek
 */
@Getter
@Setter
public class BlacklistedRecord implements Serializable {

    private static final long serialVersionUID = -8738664627706834541L;

    /**
     * Stored as an MD5 hash
     */
    private String blackListedDomainOrIP;

    private String crc64Hash;

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

    public BlacklistedRecord(String blackListedDomainOrIP, String crc64Key, Calendar listed,
            HashMap<String, ImmutablePair<String, String>> sources, HashMap<String, HashMap<String, Integer>> accuracy, Boolean presentOnWhiteList) {
        this.blackListedDomainOrIP = blackListedDomainOrIP;
        this.crc64Hash = crc64Key;
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

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
