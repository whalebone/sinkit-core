package biz.karms.sinkit.tests.api;


import biz.karms.sinkit.ejb.ArchiveService;
import biz.karms.sinkit.ejb.CacheService;
import biz.karms.sinkit.ejb.CoreService;
import biz.karms.sinkit.ejb.impl.ArchiveServiceEJB;
import biz.karms.sinkit.ejb.impl.CoreServiceEJB;
import biz.karms.sinkit.ioc.IoCRecord;
import biz.karms.sinkit.ioc.IoCSourceIdType;
import biz.karms.sinkit.tests.util.IoCFactory;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.testng.annotations.Test;

import javax.ejb.EJB;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Logger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Michal Karm Babacek
 */
public class ApiIntegrationTest extends Arquillian {

    private static final Logger LOGGER = Logger.getLogger(ApiIntegrationTest.class.getName());
    private static final String TOKEN = System.getenv("SINKIT_ACCESS_TOKEN");

    @EJB
    CacheService cacheService;

    @EJB
    ArchiveService archiveService;

    @EJB
    CoreService coreService;

    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, priority = 1)
    @OperateOnDeployment("ear")
    @RunAsClient
    public void postAllDNSClientSettingsTest(@ArquillianResource URL context) throws Exception {
        WebClient webClient = new WebClient();
        WebRequest requestSettings = new WebRequest(new URL(context + "rest/rules/all"), HttpMethod.POST);
        requestSettings.setAdditionalHeader("Content-Type", "application/json");
        requestSettings.setAdditionalHeader("X-sinkit-token", TOKEN);
        requestSettings.setRequestBody(
                "[{\"dns_client\":\"10.10.10.10/32\"," +
                        "\"settings\":{\"feed-3\":\"D\"," +
                        "\"feed2\":\"S\"," +
                        "\"test-feed1\":\"L\"}," +
                        "\"customer_id\":2," +
                        "\"customer_name\":\"yadayada-2\"}," +
                        "{\"dns_client\":\"10.10.10.11/32\"," +
                        "\"settings\":{\"feed-3\":\"D\"," +
                        "\"feed2\":\"S\"," +
                        "\"test-feed1\":\"L\"}," +
                        "\"customer_id\":2," +
                        "\"customer_name\":\"yadayada-2\"}," +
                        "{\"dns_client\":\"10.11.12.0/24\"," +
                        "\"settings\":{\"test-feed1\":\"L\"," +
                        "\"feed2\":\"S\"," +
                        "\"feed-3\":\"D\"}," +
                        "\"customer_id\":1," +
                        "\"customer_name\":\"test-yadayada\"}," +
                        "{\"dns_client\":\"10.11.30.30/32\"," +
                        "\"settings\":{\"test-feed1\":\"L\"," +
                        "\"feed2\":\"S\"," +
                        "\"feed-3\":\"D\"}," +
                        "\"customer_id\":1," +
                        "\"customer_name\":\"test-yadayada\"}]");
        Page page = webClient.getPage(requestSettings);
        assertEquals(200, page.getWebResponse().getStatusCode());
        String responseBody = page.getWebResponse().getContentAsString();
        LOGGER.info("Response:" + responseBody);
        String expected = "4 RULES PROCESSED 4 PRESENT";
        assertTrue(responseBody.contains(expected), "Expected " + expected + ", but got: " + responseBody);
    }

    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, priority = 2)
    @OperateOnDeployment("ear")
    @RunAsClient
    public void putCustomListsTest(@ArquillianResource URL context) throws Exception {
        WebClient webClient = new WebClient();
        WebRequest requestSettings = new WebRequest(new URL(context + "rest/lists/2"), HttpMethod.PUT);
        requestSettings.setAdditionalHeader("Content-Type", "application/json");
        requestSettings.setAdditionalHeader("X-sinkit-token", TOKEN);
        requestSettings.setRequestBody(
                "[{\"dns_client\":\"10.10.10.10/32\"," +
                        "\"lists\":{\"seznam.cz\":\"W\"," +
                        "\"google.com\":\"B\"," +
                        "\"example.com\":\"L\"}" +
                        "}," +
                        "{\"dns_client\":\"fe80::3ea9:f4ff:fe81:c450/64\"," +
                        "\"lists\":{\"seznam.cz\":\"L\"," +
                        "\"google.com\":\"W\"," +
                        "\"example.com\":\"W\"}" +
                        "}]");
        Page page = webClient.getPage(requestSettings);
        assertEquals(200, page.getWebResponse().getStatusCode());
        String responseBody = page.getWebResponse().getContentAsString();
        LOGGER.info("putCustomListsTest Response:" + responseBody);
        String expected = "6 CUSTOM LISTS ELEMENTS PROCESSED, 6 PRESENT";
        assertTrue(responseBody.contains(expected), "Expected: " + expected + ", but got: " + responseBody);
    }

    @Test(priority = 3)
    public void addIoCsTest() throws Exception {
        IoCRecord ioCRecord = IoCFactory.getIoCRecord("hosted", "blacklist", "myDocumentId", "feed2", "feed2", "seznam.cz", IoCSourceIdType.FQDN, "seznam.cz", null, "seznam.cz");
        assertTrue(cacheService.dropTheWholeCache(), "Dropping the whole cache failed.");
        assertTrue(cacheService.addToCache(ioCRecord), "Adding a new IoC to a presumably empty cache failed.");
    }

    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, priority = 4)
    @OperateOnDeployment("ear")
    @RunAsClient
    public void getStatsTest(@ArquillianResource URL context) throws Exception {
        WebClient webClient = new WebClient();
        WebRequest requestSettings = new WebRequest(new URL(context + "rest/stats"), HttpMethod.GET);
        requestSettings.setAdditionalHeader("Content-Type", "application/json");
        requestSettings.setAdditionalHeader("X-sinkit-token", TOKEN);
        Page page = webClient.getPage(requestSettings);
        assertEquals(200, page.getWebResponse().getStatusCode());
        String responseBody = page.getWebResponse().getContentAsString();
        LOGGER.info("getStatsTest Response:" + responseBody);
        String expected = "{\"rule\":4,\"ioc\":1}";
        assertTrue(responseBody.contains(expected), "Expected: " + expected + ". got: " + responseBody);
    }

    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, priority = 5)
    @OperateOnDeployment("ear")
    @RunAsClient
    public void getIoCsTest(@ArquillianResource URL context) throws Exception {
        WebClient webClient = new WebClient();
        WebRequest requestSettings = new WebRequest(new URL(context + "rest/blacklist/records"), HttpMethod.GET);
        requestSettings.setAdditionalHeader("Content-Type", "application/json");
        requestSettings.setAdditionalHeader("X-sinkit-token", TOKEN);
        Page page = webClient.getPage(requestSettings);
        assertEquals(200, page.getWebResponse().getStatusCode());
        String responseBody = page.getWebResponse().getContentAsString();
        LOGGER.info("getIoCsTest Response:" + responseBody);
        String expected = "[\"seznam.cz\"]";
        assertTrue(responseBody.contains(expected), "Expected " + expected + ", but got: " + responseBody);
    }

    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, priority = 6)
    @OperateOnDeployment("ear")
    @RunAsClient
    public void getIoCTest(@ArquillianResource URL context) throws Exception {
        WebClient webClient = new WebClient();
        WebRequest requestSettings = new WebRequest(new URL(context + "rest/blacklist/record/seznam.cz"), HttpMethod.GET);
        requestSettings.setAdditionalHeader("Content-Type", "application/json");
        requestSettings.setAdditionalHeader("X-sinkit-token", TOKEN);
        Page page = webClient.getPage(requestSettings);
        assertEquals(200, page.getWebResponse().getStatusCode());
        String responseBody = page.getWebResponse().getContentAsString();
        LOGGER.info("getIoCTest Response:" + responseBody);
        String expected = "\"black_listed_domain_or_i_p\":\"seznam.cz\"";
        assertTrue(responseBody.contains(expected), "IoC response should have contained " + expected + ", but got:" + responseBody);
        expected = "\"sources\":{\"feed2\":\"blacklist\"}";
        assertTrue(responseBody.contains(expected), "IoC should have contained " + expected + ", but got: " + responseBody);
    }

    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, priority = 7)
    @OperateOnDeployment("ear")
    @RunAsClient
    public void getSinkHoleTest(@ArquillianResource URL context) throws Exception {
        WebClient webClient = new WebClient();
        WebRequest requestSettings = new WebRequest(new URL(context + "rest/blacklist/dns/10.11.12.22/seznam.cz"), HttpMethod.GET);
        requestSettings.setAdditionalHeader("Content-Type", "application/json");
        requestSettings.setAdditionalHeader("X-sinkit-token", TOKEN);
        Page page = webClient.getPage(requestSettings);
        assertEquals(200, page.getWebResponse().getStatusCode());
        String responseBody = page.getWebResponse().getContentAsString();
        LOGGER.info("getSinkHoleTest Response:" + responseBody);
        String expected = "{\"sinkhole\":\"" + System.getenv("SINKIT_SINKHOLE_IP") + "\"}";
        assertTrue(responseBody.contains(expected), "Should have contained " + expected + ", but got: " + responseBody);
    }

    /**
     * Not test exactly, just cleaning old data in elastic
     *
     * @param context
     * @throws Exception
     */
    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, priority = 8)
    @OperateOnDeployment("ear")
    @RunAsClient
    public void cleanElasticTest(@ArquillianResource URL context) throws Exception {
        WebClient webClient = new WebClient();
        WebRequest requestSettings = new WebRequest(
                new URL("http://" + System.getenv("SINKIT_ELASTIC_HOST") + ":" + System.getenv("SINKIT_ELASTIC_PORT") +
                        "/" + ArchiveServiceEJB.ELASTIC_IOC_INDEX + "/"), HttpMethod.DELETE);
        Page page;
        try {
            page = webClient.getPage(requestSettings);
            assertEquals(200, page.getWebResponse().getStatusCode());
        } catch (FailingHttpStatusCodeException ex) {
            //NO-OP index does not exist yet, but it's ok
        }
    }

    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, priority = 9)
    @OperateOnDeployment("ear")
    @RunAsClient
    public void receiveIoCTest(@ArquillianResource URL context) throws Exception {
        WebClient webClient = new WebClient();
        WebRequest requestSettings = new WebRequest(new URL(context + "rest/blacklist/ioc/"), HttpMethod.POST);
        requestSettings.setAdditionalHeader("Content-Type", "application/json");
        requestSettings.setAdditionalHeader("X-sinkit-token", TOKEN);

        String feed = "integrationTest";
        String type = "phishing";
        String fqdn = "phishing.ru";
        requestSettings.setRequestBody(
                "{" +
                        "\"feed\":{" +
                        "\"name\":\"" + feed + "\"," +
                        "\"url\":\"http://www.greatfeed.com/feed.txt\"" +
                        "}," +
                        "\"classification\":{" +
                        "\"type\": \"" + type + "\"," +
                        "\"taxonomy\": \"Fraud\"" +
                        "}," +
                        "\"raw\":\"aHR0cDovL2luZm9ybWF0aW9uLnVwZGF0ZS5teWFjY291bnQtc2VjdXJlLmNvbS85ODI0YTYxOGRlNTlmYjE2MTlmNTUzNTgwYWFmZjcxMS9mMWI2YTE2OTc2MDRiNmI2M2IwODBmODQ2N2FiNGZiNS8=\"," +
                        "\"source\":{" +
                        "\"fqdn\":\"" + fqdn + "\"," +
                        "\"bgp_prefix\":\"some_prefix\"," +
                        "\"asn\":\"123456\"," +
                        "\"asn_name\":\"some_name\"," +
                        "\"geolocation\":{" +
                        "\"cc\":\"RU\"," +
                        "\"city\":\"City\"," +
                        "\"latitude\":\"85.12645\"," +
                        "\"longitude\":\"-12.9788\"" +
                        "}" +
                        "}," +
                        "\"time\":{" +
                        "\"observation\":\"" + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(Calendar.getInstance().getTime()) + "\"" +
                        "}," +
                        "\"protocol\":{" +
                        "\"application\":\"ssh\"" +
                        "}," +
                        "\"description\":{" +
                        "   \"text\":\"description\"" +
                        "}" +
                        "}"
        );
        Page page = webClient.getPage(requestSettings);
        assertEquals(200, page.getWebResponse().getStatusCode());
        String responseBody = page.getWebResponse().getContentAsString();
        LOGGER.info("Response:" + responseBody);
        String expected = "{\"feed\":{\"url\":\"http://www.greatfeed.com/feed.txt\",\"name\":\"" + feed + "\"},\"description\":{\"text\":\"description\"},\"classification\":{\"type\":\"" + type + "\",\"taxonomy\":\"Fraud\"},\"protocol\":{\"application\":\"ssh\"},\"raw\":\"aHR0cDovL2luZm9ybWF0aW9uLnVwZGF0ZS5teWFjY291bnQtc2VjdXJlLmNvbS85ODI0YTYxOGRlNTlmYjE2MTlmNTUzNTgwYWFmZjcxMS9mMWI2YTE2OTc2MDRiNmI2M2IwODBmODQ2N2FiNGZiNS8\\u003d\",\"source\":{\"id\":{\"value\":\"" + fqdn + "\",\"type\":\"fqdn\"},\"fqdn\":\"" + fqdn + "\",\"asn\":123456,\"asn_name\":\"some_name\",\"geolocation\":{\"cc\":\"RU\",\"city\":\"City\",\"latitude\":85.12645,\"longitude\":-12.9788},\"bgp_prefix\":\"some_prefix\"},\"time\":{\"observation\":\"";
        assertTrue(responseBody.contains(expected), "Should have contained " + expected + ", but got: " + responseBody);
    }

    @Test(priority = 10)
    public void iocInElasticTest() throws Exception {

        String feed = "integrationTest";
        String type = "phishing";
        String fqdn = "phishing.ru";

        IoCRecord ioc = archiveService.findActiveIoCRecordBySourceId(fqdn, type, feed);
        assertNotNull(ioc, "Excpecting IoC, but got null with fqdn: " + fqdn + ", type: " + type + ", feed: " + feed);
        assertEquals(ioc.getFeed().getName(), feed, "Expected feed.name: " + feed + ", but got: " + ioc.getFeed().getName());
        assertEquals(ioc.getSource().getId().getType(), IoCSourceIdType.FQDN, "Expected source.id.type: " + IoCSourceIdType.FQDN + ", but got: " + ioc.getSource().getId().getType());
        assertEquals(ioc.getSource().getId().getValue(), fqdn, "Expected source.id.value: " + fqdn + ", but got: " + ioc.getSource().getId().getValue());
        assertEquals(ioc.getClassification().getType(), type, "Expected classification.type: " + type + ", but got: " + ioc.getClassification().getType());
        assertNotNull(ioc.getSeen().getFirst(), "Expected seen.first but got null");
        assertNotNull(ioc.getSeen().getLast(), "Expected seen.last but got null");
        Calendar c = Calendar.getInstance();
        c.add(Calendar.HOUR, -coreService.getIocActiveHours());
        assertTrue(ioc.getSeen().getLast().after(c.getTime()), "Expected seen.last is not older than " + coreService.getIocActiveHours() + " hours, but got " + ioc.getSeen().getLast());
        assertTrue(ioc.isActive(), "Expected ioc to be active, but got active: false");
        assertNotNull(ioc.getTime().getObservation(), "Expecting time.observation, but got null");
    }

    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, priority = 11)
    @OperateOnDeployment("ear")
    @RunAsClient
    public void iocInCacheTest(@ArquillianResource URL context) throws Exception {
        WebClient webClient = new WebClient();
        WebRequest requestSettings = new WebRequest(new URL(context + "rest/blacklist/record/phishing.ru"), HttpMethod.GET);
        requestSettings.setAdditionalHeader("Content-Type", "application/json");
        requestSettings.setAdditionalHeader("X-sinkit-token", TOKEN);
        Page page = webClient.getPage(requestSettings);
        assertEquals(200, page.getWebResponse().getStatusCode());
        String responseBody = page.getWebResponse().getContentAsString();
        LOGGER.info("iocInCacheTest Response:" + responseBody);
        String expected = "\"black_listed_domain_or_i_p\":\"phishing.ru\"";
        assertTrue(responseBody.contains(expected), "IoC response should have contained " + expected + ", but got:" + responseBody);
        expected = "\"sources\":{\"integrationTest\":\"phishing\"}";
        assertTrue(responseBody.contains(expected), "IoC should have contained " + expected + ", but got: " + responseBody);
    }
}
