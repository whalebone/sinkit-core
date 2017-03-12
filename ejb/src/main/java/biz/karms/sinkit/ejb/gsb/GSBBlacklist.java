package biz.karms.sinkit.ejb.gsb;

import biz.karms.sinkit.ejb.ThreatType;

import java.util.Arrays;
import java.util.List;

/**
 * @author Tomas Kozel
 */
public enum GSBBlacklist {

    MALWARE(Arrays.asList("googpub-malware-shavar", "goog-malware-shavar"), ThreatType.MALWARE),
    PHISHING(Arrays.asList("googpub-phish-shavar", "goog-phish-shavar"), ThreatType.PHISHING),
    UNWANTED(Arrays.asList("googpub-unwanted-shavar", "goog-unwanted-shavar"), ThreatType.UNWANTED),
    UNKNOWN(null, ThreatType.BLACKLIST);

    private List<String> gsbNames;
    private ThreatType threatType;

    GSBBlacklist(List<String> gsbNames, ThreatType threatType) {
        this.gsbNames = gsbNames;
        this.threatType = threatType;
    }

    public ThreatType getThreatType() {
        return threatType;
    }

    public static GSBBlacklist parseGSBName(String gsbName) {
        for (GSBBlacklist gsbBlacklist : GSBBlacklist.values()) {
            if (gsbBlacklist.gsbNames != null && gsbBlacklist.gsbNames.contains(gsbName)) {
                return gsbBlacklist;
            }
        }
        return UNKNOWN;
    }
}
