package biz.karms.sinkit.ejb.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by tkozel on 1/10/16.
 */
public class WhitelistUtilsTest {

    @Test
    public void stripSubdomainsTest() {
        assertEquals("google.com", WhitelistUtils.stripSubdomains("google.com"));
        assertEquals("google.com", WhitelistUtils.stripSubdomains("www.google.com"));
        assertEquals("google.com", WhitelistUtils.stripSubdomains("a.b.c.d.e.google.com"));
        assertEquals("google.com", WhitelistUtils.stripSubdomains("...google.com"));
        assertEquals("google", WhitelistUtils.stripSubdomains("google"));
    }
}
