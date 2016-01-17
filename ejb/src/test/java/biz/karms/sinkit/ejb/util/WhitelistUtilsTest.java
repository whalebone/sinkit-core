package biz.karms.sinkit.ejb.util;

import jdk.nashorn.internal.ir.WhileNode;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
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

    @Test
    public void explodeDomainsTest() {
        String fqdn = "one.two.three.four.five.com";
        String[] domains = new String[] {
                "five.com",
                "four.five.com",
                "three.four.five.com",
                "two.three.four.five.com",
                "one.two.three.four.five.com",
        };
        assertArrayEquals(domains, WhitelistUtils.explodeDomains(fqdn));
        assertArrayEquals(new String[]{"none"}, WhitelistUtils.explodeDomains("none"));
        assertArrayEquals(new String[]{"whalebone.io"}, WhitelistUtils.explodeDomains("whalebone.io"));
    }
}
