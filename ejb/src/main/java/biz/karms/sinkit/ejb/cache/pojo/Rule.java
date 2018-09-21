package biz.karms.sinkit.ejb.cache.pojo;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.HashMap;

/**
 * @author Michal Karm Babacek
 */
@Getter
@Setter
public class Rule implements Serializable {

    private static final long serialVersionUID = 187732233347691L;

    private String startAddress;

    private String endAddress;

    private String cidrAddress;

    private int customerId;

    /**
     * Feed UID : Mode <L|S|D>
     */
    private HashMap<String, String> sources;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Rule)) return false;

        Rule rule = (Rule) o;

        if (customerId != rule.customerId) return false;
        if (startAddress != null ? !startAddress.equals(rule.startAddress) : rule.startAddress != null) return false;
        if (endAddress != null ? !endAddress.equals(rule.endAddress) : rule.endAddress != null) return false;
        if (!cidrAddress.equals(rule.cidrAddress)) return false;
        return sources.equals(rule.sources);

    }

    @Override
    public int hashCode() {
        int result = startAddress != null ? startAddress.hashCode() : 0;
        result = 31 * result + (endAddress != null ? endAddress.hashCode() : 0);
        result = 31 * result + cidrAddress.hashCode();
        result = 31 * result + customerId;
        result = 31 * result + sources.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
