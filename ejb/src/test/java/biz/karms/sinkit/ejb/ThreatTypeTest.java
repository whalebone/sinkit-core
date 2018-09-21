package biz.karms.sinkit.ejb;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by tkozel on 5/23/16.
 */
public class ThreatTypeTest {

    @Test
    public void parseTest() {
        assertEquals(ThreatType.BLACKLIST, ThreatType.parseName("blacklist"));
        assertEquals(ThreatType.C_C, ThreatType.parseName("c&c"));
        assertEquals(ThreatType.MALWARE, ThreatType.parseName("malware"));
        assertEquals(ThreatType.MALWARE_CONF, ThreatType.parseName("malware configuration"));
        assertEquals(ThreatType.PHISHING, ThreatType.parseName("phishing"));
        assertEquals(ThreatType.RANSOMWARE, ThreatType.parseName("ransomware"));
        assertEquals(ThreatType.UNWANTED, ThreatType.parseName("unwanted software"));
        assertNull(ThreatType.parseName("unknown threat type"));
    }
}
