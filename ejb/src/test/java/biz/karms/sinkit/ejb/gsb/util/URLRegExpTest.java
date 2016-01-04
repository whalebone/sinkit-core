package biz.karms.sinkit.ejb.gsb.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by tom on 1/2/16.
 *
 * @author Tomas Kozel
 */
@RunWith(Parameterized.class)
public class URLRegExpTest {

    @Parameterized.Parameters(name = "{index}: {0} => proto: {1}, host: {2}, port: {3}, path: {4}, query: {5}")
    public static Iterable<Object[]> data() {

        return Arrays.asList(new Object[][]{
                {"http://google.com", "http", "google.com", null, null, null},
                {"https://www.google.com/some/path?and=some&query", "https", "www.google.com", null, "/some/path", "?and=some&query"},
                {"sftp://query/is/missing/", "sftp", "query", null, "/is/missing/", null},
                {"ftp://1.2.3.4/?", "ftp", "1.2.3.4", null, "/", "?"},
                {"NonE://nonsense?not=to&be=query", "NonE", "nonsense?not=to&be=query", null, null, null},
                {"google.com/path", null, "google.com", null, "/path", null },
                {"gooGle.com", null, "gooGle.com", null, null, null},
                {"1", null, "1", null, null, null},
                {"1:blabla", null, "1:blabla", null, null, null},
                {"google.com:8080/some/path?with=query&string", null, "google.com", "8080", "/some/path", "?with=query&string"},
                {"google.com:/some/path?with=query&string", null, "google.com", null, "/some/path", "?with=query&string"}
        });
    }

    private String url;
    private String expectedProto;
    private String expectedHost;
    private String expectedPort;
    private String expectedPath;
    private String expectedQuery;

    public URLRegExpTest(String url, String expectedProto, String expectedHost, String expectedPort, String expectedPath, String expectedQuery) {
        this.url = url;
        this.expectedProto = expectedProto;
        this.expectedHost = expectedHost;
        this.expectedPort = expectedPort;
        this.expectedPath = expectedPath;
        this.expectedQuery = expectedQuery;
    }

    @Test
    public void testURLRegExp() {
        Pattern urlPattern = Pattern.compile(GSBUtils.URL_REGEXP);
        Matcher matcher = urlPattern.matcher(url);
        assertTrue("URL '" + url + "' doesn't match regexp '" + GSBUtils.URL_REGEXP + "'",
                matcher.find());
        assertEquals(expectedProto, matcher.group(GSBUtils.PROTO_GROUP));
        assertEquals(expectedHost, matcher.group(GSBUtils.HOST_GROUP));
        assertEquals(expectedPort, matcher.group(GSBUtils.PORT_GROUP));
        assertEquals(expectedPath, matcher.group(GSBUtils.PATH_GROUP));
        assertEquals(expectedQuery, matcher.group(GSBUtils.QUERY_GROUP));
    }

}
