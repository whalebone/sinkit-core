package biz.karms.sinkit.ejb.gsb.util;

import biz.karms.sinkit.ejb.util.GSBUrl;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Created by tom on 12/6/15.
 *
 * Testing date provided by google -> ensuring our hashing works the same way as theirs.
 *
 * @author Tomas Kozel
 */
public class GSBUtilsTest {

    @Test
    public void testHashing() throws Exception {

        String message = "abc";
        byte[] hashPrefix = Arrays.copyOf(GSBUtils.computeHash(message), 4);
        assertArrayEquals(new byte[] {(byte)0xba, (byte) 0x78, (byte) 0x16, (byte) 0xbf}, hashPrefix);

        String message2 = "abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq";
        byte[] hashPrefix2 = Arrays.copyOf(GSBUtils.computeHash(message2), 6);;
        assertArrayEquals(new byte[]{(byte) 0x24, (byte) 0x8d, (byte) 0x6a, (byte) 0x61, (byte) 0xd2, (byte) 0x06}, hashPrefix2);
    }
}
