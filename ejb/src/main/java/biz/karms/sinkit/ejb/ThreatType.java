package biz.karms.sinkit.ejb;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Tomas Kozel
 */
public enum ThreatType {

    C_C("c&c"),
    MALWARE("malware"),
    RANSOMWARE("ransomware"),
    MALWARE_CONF("malware configuration"),
    PHISHING("phishing"),
    BLACKLIST("blacklist"),
    UNWANTED("unwanted software");

    //Arrays.asList("c&c", "malware", "ransomware", "malware configuration", "phishing", "blacklist", "unwanted software");

    ThreatType(String name) {
        this.name = name;
    }

    private String name;

    public String getName() {
        return name;
    }

    public static ThreatType parseName(String threatTypeName) {
        for (ThreatType type : ThreatType.values()) {
            if (StringUtils.isNotBlank(type.getName()) && type.getName().equals(threatTypeName)) {
                return type;
            }
        }
        return null;
    }
}
