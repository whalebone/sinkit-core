package biz.karms.sinkit.ejb.gsb.util;

import biz.karms.sinkit.ejb.gsb.dto.FullHashLookupResponse;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Created by tom on 12/19/15.
 *
 * @author Tomas Kozel
 */
public class FullHashLookupResponseReaderTest {

    FullHashLookupResponseReader reader;

    @Before
    public void setUp() {
        reader = new FullHashLookupResponseReader();
    }

    @Test
    public void isReadableTest() throws Exception {
        assertTrue(reader.isReadable(FullHashLookupResponse.class, null, null, MediaType.APPLICATION_OCTET_STREAM_TYPE));
        assertFalse(reader.isReadable(Object.class, null, null, MediaType.TEXT_PLAIN_TYPE));
    }

    @Test
    public void emptyResponseTest() throws Exception {
        String rawResponse = "600";
        InputStream is = new ByteArrayInputStream(rawResponse.getBytes());
        FullHashLookupResponse response = reader.readFrom(FullHashLookupResponse.class, null, null, MediaType.APPLICATION_OCTET_STREAM_TYPE, null, is);
        assertNotNull(response);
        assertEquals(600, response.getValidSeconds());
        assertNotNull(response.getFullHashes());
        assertTrue(response.getFullHashes().isEmpty());
    }

    @Test
    public void parseSimpleTest() throws Exception {
        String rawResponse =
                "600\n" +
                "googpub-phish-shavar:32:1\n" +
                "01234567890123456789012345678901";
        InputStream is = new ByteArrayInputStream(rawResponse.getBytes());
        FullHashLookupResponse response = reader.readFrom(FullHashLookupResponse.class, null, null, MediaType.APPLICATION_OCTET_STREAM_TYPE, null, is);
        assertNotNull("Expected response not to be null.", response);
        assertEquals("Expected valid seconds to be 600, but got " + response.getValidSeconds(), 600, response.getValidSeconds());
        assertNotNull("Expected map of blacklists not not to be null", response.getFullHashes());
        assertEquals("Expected number of blacklists is 1, but got " + response.getFullHashes().size(), 1, response.getFullHashes().size());
        assertTrue("Expected blacklist name in response to be 'googpub-phish-shavar'", response.getFullHashes().keySet().contains("googpub-phish-shavar"));
        assertArrayEquals("01234567890123456789012345678901".getBytes(), response.getFullHashes().get("googpub-phish-shavar").get(0).getKey());
        assertNull("Expected metadata to be null.", response.getFullHashes().get("googpub-phish-shavar").get(0).getValue());
    }

    @Test
    public void parseWithMetadataTest() throws Exception {
        String rawResponse =
                "900\n" +
                "goog-malware-shavar:32:2:m\n" +
                "01234567890123456789012345678901987654321098765432109876543210982\n" +
                "AA3\n" +
                "BBBgoogpub-phish-shavar:32:1\n" +
                "01234567890123456789012345678901";
        InputStream is = new ByteArrayInputStream(rawResponse.getBytes());
        FullHashLookupResponse response = reader.readFrom(FullHashLookupResponse.class, null, null, MediaType.APPLICATION_OCTET_STREAM_TYPE, null, is);
        assertNotNull("Expected response not to be null.", response);
        assertEquals("Expected valid seconds to be 900, but got " + response.getValidSeconds(), 900, response.getValidSeconds());
        assertNotNull("Expected map of blacklists not not to be null", response.getFullHashes());
        assertEquals("Expected number of blacklists is 2, but got " + response.getFullHashes().size(), 2, response.getFullHashes().size());
        assertTrue("Expected blacklist name in response to be 'goog-malware-shavar'", response.getFullHashes().keySet().contains("goog-malware-shavar"));
        assertArrayEquals("01234567890123456789012345678901".getBytes(), response.getFullHashes().get("goog-malware-shavar").get(0).getKey());
        assertArrayEquals("AA".getBytes(), response.getFullHashes().get("goog-malware-shavar").get(0).getValue());
        assertArrayEquals("98765432109876543210987654321098".getBytes(), response.getFullHashes().get("goog-malware-shavar").get(1).getKey());
        assertArrayEquals("BBB".getBytes(), response.getFullHashes().get("goog-malware-shavar").get(1).getValue());
        assertTrue("Expected blacklist name in response to be 'googpub-phish-shavar'", response.getFullHashes().keySet().contains("googpub-phish-shavar"));
        assertArrayEquals("01234567890123456789012345678901".getBytes(), response.getFullHashes().get("googpub-phish-shavar").get(0).getKey());
        assertNull("Expected metadata to be null.", response.getFullHashes().get("googpub-phish-shavar").get(0).getValue());
    }

    @Test
    public void parseNonPrintableBytesTest() throws Exception {

        byte[] hash = new byte[18];
        for (int i = 0; i < 18; i++) {
            hash[i] = (byte) (i);
        }
        byte[] hash2 = new byte[18];
        new Random().nextBytes(hash2);

        byte[] metadata = new byte[3];
        metadata[0] = (byte) 0; //NULL
        metadata[1] = (byte) 3; //END OF TEXT
        metadata[2] = (byte) 10; //LINE FEED

        byte[] metadata2 = new byte[4];
        new Random().nextBytes(metadata2);
        byte[] rawResponse = ArrayUtils.addAll("122\ngoog-malware-shavar:18:2:m\n".getBytes(), hash);
        rawResponse = ArrayUtils.addAll(rawResponse, hash2);
        rawResponse = ArrayUtils.addAll(rawResponse, String.valueOf(metadata.length).getBytes());
        rawResponse = ArrayUtils.addAll(rawResponse, "\n".getBytes());
        rawResponse = ArrayUtils.addAll(rawResponse, metadata);
        rawResponse = ArrayUtils.addAll(rawResponse, String.valueOf(metadata2.length).getBytes());
        rawResponse = ArrayUtils.addAll(rawResponse, "\n".getBytes());
        rawResponse = ArrayUtils.addAll(rawResponse, metadata2);


        System.out.println(new String(rawResponse));
        InputStream is = new ByteArrayInputStream(rawResponse);
        FullHashLookupResponse response = reader.readFrom(FullHashLookupResponse.class, null, null, MediaType.APPLICATION_OCTET_STREAM_TYPE, null, is);
        assertNotNull("Expected response not to be null.", response);
        assertEquals("Expected valid seconds to be 122, but got " + response.getValidSeconds(), 122, response.getValidSeconds());
        assertNotNull("Expected map of blacklists not not to be null", response.getFullHashes());
        assertEquals("Expected number of blacklists is 1, but got " + response.getFullHashes().size(), 1, response.getFullHashes().size());
        assertTrue("Expected blacklist name in response to be 'goog-malware-shavar'", response.getFullHashes().keySet().contains("goog-malware-shavar"));
        assertArrayEquals(hash, response.getFullHashes().get("goog-malware-shavar").get(0).getKey());
        assertArrayEquals(metadata, response.getFullHashes().get("goog-malware-shavar").get(0).getValue());
        assertArrayEquals(hash2, response.getFullHashes().get("goog-malware-shavar").get(1).getKey());
        assertArrayEquals(metadata2, response.getFullHashes().get("goog-malware-shavar").get(1).getValue());
//        assertTrue("Expected blacklist name in response to be 'googpub-phish-shavar'", response.getFullHashes().keySet().contains("googpub-phish-shavar"));
//        assertArrayEquals("01234567890123456789012345678901".getBytes(), response.getFullHashes().get("googpub-phish-shavar").get(0).getKey());
//        assertNull("Expected metadata to be null.", response.getFullHashes().get("googpub-phish-shavar").get(0).getValue());
    }
}
