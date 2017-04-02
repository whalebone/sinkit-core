package biz.karms.sinkit.ejb.gsb;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Tomas Kozel
 */
public class GSBBlacklistTest {

    @Test
    public void parseGSBNameTest() {
        assertEquals(GSBBlacklist.MALWARE, GSBBlacklist.parseGSBName("googpub-malware-shavar"));
        assertEquals(GSBBlacklist.MALWARE, GSBBlacklist.parseGSBName("goog-malware-shavar"));
        assertEquals(GSBBlacklist.PHISHING, GSBBlacklist.parseGSBName("googpub-phish-shavar"));
        assertEquals(GSBBlacklist.PHISHING, GSBBlacklist.parseGSBName("goog-phish-shavar"));
        assertEquals(GSBBlacklist.UNWANTED, GSBBlacklist.parseGSBName("googpub-unwanted-shavar"));
        assertEquals(GSBBlacklist.UNWANTED, GSBBlacklist.parseGSBName("goog-unwanted-shavar"));
        assertEquals(GSBBlacklist.UNKNOWN, GSBBlacklist.parseGSBName("unknown"));
        assertEquals(GSBBlacklist.UNKNOWN, GSBBlacklist.parseGSBName(""));
        assertEquals(GSBBlacklist.UNKNOWN, GSBBlacklist.parseGSBName(null));
    }
}
