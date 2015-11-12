package biz.karms.sinkit.tests.api;


import biz.karms.sinkit.ejb.ArchiveService;
import biz.karms.sinkit.ejb.CacheService;
import biz.karms.sinkit.ejb.CoreService;
import biz.karms.sinkit.ejb.impl.ArchiveServiceEJB;
import biz.karms.sinkit.ioc.IoCRecord;
import biz.karms.sinkit.ioc.IoCSourceIdType;
import biz.karms.sinkit.tests.util.IoCFactory;
import com.gargoylesoftware.htmlunit.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.testng.annotations.Test;

import javax.ejb.EJB;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Logger;

import static org.testng.Assert.*;

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
        String expected = "\"feed\":{\"url\":\"http://www.greatfeed.com/feed.txt\",\"name\":\"" + feed + "\"},\"description\":{\"text\":\"description\"},\"classification\":{\"type\":\"" + type + "\",\"taxonomy\":\"Fraud\"},\"protocol\":{\"application\":\"ssh\"},\"raw\":\"aHR0cDovL2luZm9ybWF0aW9uLnVwZGF0ZS5teWFjY291bnQtc2VjdXJlLmNvbS85ODI0YTYxOGRlNTlmYjE2MTlmNTUzNTgwYWFmZjcxMS9mMWI2YTE2OTc2MDRiNmI2M2IwODBmODQ2N2FiNGZiNS8\\u003d\",\"source\":{\"id\":{\"value\":\"" + fqdn + "\",\"type\":\"fqdn\"},\"fqdn\":\"" + fqdn + "\",\"asn\":123456,\"asn_name\":\"some_name\",\"geolocation\":{\"cc\":\"RU\",\"city\":\"City\",\"latitude\":85.12645,\"longitude\":-12.9788},\"bgp_prefix\":\"some_prefix\"},\"time\":{\"observation\":\"";
        assertTrue(responseBody.contains(expected), "Should have contained " + expected + ", but got: " + responseBody);
    }

    @Test(priority = 10)
    public void iocInElasticTest() throws Exception {

        String feed = "integrationTest";
        String type = "phishing";
        String fqdn = "phishing.ru";
        String iocId = "d056ec334e3c046f0d7fdde6f3d02c8b";  // id hash from values above

        IoCRecord ioc = archiveService.getIoCRecordById(iocId);
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

    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, priority = 20)
    @OperateOnDeployment("ear")
    @RunAsClient
    public void endToEndTest(@ArquillianResource URL context) throws Exception {

        WebClient webClient = new WebClient();

        // Feed config
        WebRequest requestSettingsFeed = new WebRequest(new URL(context + "rest/rules/all"), HttpMethod.POST);
        requestSettingsFeed.setAdditionalHeader("Content-Type", "application/json");
        requestSettingsFeed.setAdditionalHeader("X-sinkit-token", TOKEN);
        requestSettingsFeed.setRequestBody("[{\"dns_client\":\"94.0.0.0/1\",\"settings\":{\"some-intelmq-feed-to-sink\":\"S\",\"some-feed-to-log\":\"L\"},\"customer_id\":666,\"customer_name\":\"Some Name\"}]");
        Page pageFeed = webClient.getPage(requestSettingsFeed);
        assertEquals(200, pageFeed.getWebResponse().getStatusCode());
        String responseBodyFeed = pageFeed.getWebResponse().getContentAsString();
        LOGGER.info("endToEnd Response:" + responseBodyFeed);
        assertTrue(responseBodyFeed.contains("1 RULES PROCESSED"));

        //add ioc
        // 2015-12-12T22:52:58+02:00
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        String observation = df.format(new Date());
        WebRequest requestSettingsIoC = new WebRequest(new URL(context + "rest/blacklist/ioc/"), HttpMethod.POST);
        requestSettingsIoC.setAdditionalHeader("Content-Type", "application/json");
        requestSettingsIoC.setAdditionalHeader("X-sinkit-token", TOKEN);
        requestSettingsIoC.setRequestBody("{\"feed\":{\"name\":\"some-intelmq-feed-to-sink\",\"url\":\"http://example.com/feed.txt\"},\"classification\":{\"type\": \"phishing\",\"taxonomy\": \"Fraud\"},\"raw\":\"aHwwwwfdfBmODQ2N244iNGZiNS8=\",\"source\":{\"fqdn\":\"evil-domain-that-is-to-be-listed.cz\",\"bgp_prefix\":\"some_prefix\",\"asn\":\"3355556\",\"asn_name\":\"any_name\",\"geolocation\":{\"cc\":\"RU\",\"city\":\"City\",\"latitude\":\"85.12645\",\"longitude\":\"-12.9788\"}},\"time\":{\"observation\":\"" + observation + "\"},\"protocol\":{\"application\":\"ssh\"},\"description\":{\"text\":\"description\"}}");
        Page pageIoC = webClient.getPage(requestSettingsIoC);
        assertEquals(200, pageIoC.getWebResponse().getStatusCode());
        String responseBodyIoC = pageIoC.getWebResponse().getContentAsString();
        LOGGER.info("endToEndIoC Response:" + responseBodyIoC);
        assertTrue(responseBodyIoC.contains("\"document_id\":\"2350e3c4042fbb9678b7b94269e91e7b\""));
        assertTrue(responseBodyIoC.contains("\"feed\":{\"url\":\"http://example.com/feed.txt\",\"name\":\"some-intelmq-feed-to-sink\"},\"description\":{\"text\":\"description\"},\"classification\":{\"type\":\"phishing\",\"taxonomy\":\"Fraud\"},\"protocol\":{\"application\":\"ssh\"},\"raw\":\"aHwwwwfdfBmODQ2N244iNGZiNS8\\u003d\",\"source\":{\"id\":{\"value\":\"evil-domain-that-is-to-be-listed.cz\",\"type\":\"fqdn\"},\"fqdn\":\"evil-domain-that-is-to-be-listed.cz\",\"asn\":3355556,\"asn_name\":\"any_name\",\"geolocation\":{\"cc\":\"RU\",\"city\":\"City\",\"latitude\":85.12645,\"longitude\":-12.9788},\"bgp_prefix\":\"some_prefix\"}"));

        //dns query
        WebRequest requestSettingsDNS = new WebRequest(new URL(context + "rest/blacklist/dns/94.0.0.1/evil-domain-that-is-to-be-listed.cz"), HttpMethod.GET);
        requestSettingsDNS.setAdditionalHeader("Content-Type", "application/json");
        requestSettingsDNS.setAdditionalHeader("X-sinkit-token", TOKEN);
        Page pageDNS = webClient.getPage(requestSettingsDNS);
        assertEquals(200, pageDNS.getWebResponse().getStatusCode());
        String responseBodyDNS = pageDNS.getWebResponse().getContentAsString();
        LOGGER.info("endToEndDNS Response:" + responseBodyDNS);
        assertTrue(responseBodyDNS.contains("{\"sinkhole\":\"" + System.getenv("SINKIT_SINKHOLE_IP") + "\"}"));

        String index = IoCFactory.getLogIndex();

        WebRequest requestSettingsLog = new WebRequest(new URL(
                "http://" + System.getenv("SINKIT_ELASTIC_HOST") + ":" + System.getenv("SINKIT_ELASTIC_PORT") + "/" +
                        index + "/" + ArchiveServiceEJB.ELASTIC_LOG_TYPE + "/_search"
        ), HttpMethod.POST);
        requestSettingsLog.setAdditionalHeader("Content-Type", "application/json");
        requestSettingsLog.setAdditionalHeader("X-sinkit-token", TOKEN);
        requestSettingsLog.setRequestBody("{\n" +
                        "   \"query\" : {\n" +
                        "       \"filtered\" : {\n" +
                        "           \"query\" : {\n" +
                        "               \"query_string\" : {\n" +
                        "                   \"query\": \"action : \\\"block\\\" AND " +
                        "                       client : \\\"666\\\" AND " +
                        "                       request.ip : \\\"94.0.0.1\\\" AND " +
//depracted                        "                       request.raw : \\\"requestRaw\\\" AND " +
                        "                       reason.fqdn : \\\"evil-domain-that-is-to-be-listed.cz\\\"\"\n" +
//not used in this case                        "                       reason.ip : \\\"10.1.1.3\\\"\"\n" +
                        "               }\n" +
                        "           }\n" +
                        "       }\n" +
                        "   }\n" +
                        "}\n"
        );
        Page pageLog = webClient.getPage(requestSettingsLog);
        assertEquals(200, pageLog.getWebResponse().getStatusCode());
        String responseBodyLog = pageLog.getWebResponse().getContentAsString();
        LOGGER.info("Response:" + responseBodyLog);
        JsonParser jsonParser = new JsonParser();
        JsonArray hits = jsonParser.parse(responseBodyLog).getAsJsonObject()
                .get("hits").getAsJsonObject()
                .get("hits").getAsJsonArray();
        assertTrue(hits.size() == 1);

        JsonObject logRecord = hits.get(0).getAsJsonObject().get("_source").getAsJsonObject();
        assertEquals(logRecord.get("action").getAsString(), "block", "Expected LogRecord.action: block, but got: " + logRecord.get("action").getAsString());
        assertEquals(logRecord.get("client").getAsString(), "666", "Expected LogRecord.client: 666, but got: " + logRecord.get("client").getAsString());
        assertEquals(logRecord.get("request").getAsJsonObject().get("ip").getAsString(), "94.0.0.1", "Expected LogRecord.request.ip: 94.0.0.1, but got: " + logRecord.get("request").getAsJsonObject().get("ip").getAsString());
        assertNull(logRecord.get("request").getAsJsonObject().get("raw")/*, "Expected  LogRecord.request.raw to be null, but got: " + logRecord.get("request").getAsJsonObject().get("raw").getAsString()*/);
        assertEquals(logRecord.get("reason").getAsJsonObject().get("fqdn").getAsString(), "evil-domain-that-is-to-be-listed.cz","Expected LogRecord.reason.fqdn: evil-domain-that-is-to-be-listed.cz, but got: " + logRecord.get("reason").getAsJsonObject().get("fqdn").getAsString());
        assertNull(logRecord.get("reason").getAsJsonObject().get("ip")/*, "Expected LogRecord.reason.ip to be null, but got: " + logRecord.get("reason").getAsJsonObject().get("ip").getAsString()*/);
        assertNotNull(logRecord.get("logged").getAsString(), "Expected LogRecord.logged not to be null, but got null");
        assertEquals(logRecord.get("virus_total_request").getAsJsonObject().get("status").getAsString(), "waiting", "Expected LogRecord.virus_total_request.status: waiting, but got: " + logRecord.get("virus_total_request").getAsJsonObject().get("status").getAsString());
        JsonArray matchedIocs = logRecord.get("matched_iocs").getAsJsonArray();
        assertTrue(matchedIocs.size() == 1, "Expected size of LogRecord.matched_iocs array: 1, but got: " + matchedIocs.size());
        JsonObject matchedIoc1 = matchedIocs.get(0).getAsJsonObject();
        assertNotNull(matchedIoc1.get("unique_ref"));
        assertEquals(matchedIoc1.get("feed").getAsJsonObject().get("url").getAsString(), "http://example.com/feed.txt");
        assertEquals(matchedIoc1.get("feed").getAsJsonObject().get("name").getAsString(), "some-intelmq-feed-to-sink");
        assertEquals(matchedIoc1.get("description").getAsJsonObject().get("text").getAsString(), "description");
        assertEquals(matchedIoc1.get("classification").getAsJsonObject().get("type").getAsString(), "phishing");
        assertEquals(matchedIoc1.get("classification").getAsJsonObject().get("taxonomy").getAsString(), "Fraud");
        assertEquals(matchedIoc1.get("protocol").getAsJsonObject().get("application").getAsString(), "ssh");
        assertEquals(matchedIoc1.get("source").getAsJsonObject().get("id").getAsJsonObject().get("value").getAsString(), "evil-domain-that-is-to-be-listed.cz");
        assertEquals(matchedIoc1.get("source").getAsJsonObject().get("id").getAsJsonObject().get("type").getAsString(), "fqdn");
        assertEquals(matchedIoc1.get("source").getAsJsonObject().get("fqdn").getAsString(), "evil-domain-that-is-to-be-listed.cz");
        assertEquals(matchedIoc1.get("source").getAsJsonObject().get("asn").getAsInt(), 3355556);
        assertEquals(matchedIoc1.get("source").getAsJsonObject().get("asn_name").getAsString(), "any_name");
        assertEquals(matchedIoc1.get("source").getAsJsonObject().get("geolocation").getAsJsonObject().get("cc").getAsString(), "RU");
        assertEquals(matchedIoc1.get("source").getAsJsonObject().get("geolocation").getAsJsonObject().get("city").getAsString(), "City");
        assertEquals(matchedIoc1.get("source").getAsJsonObject().get("geolocation").getAsJsonObject().get("latitude").getAsDouble(), 85.12645);
        assertEquals(matchedIoc1.get("source").getAsJsonObject().get("geolocation").getAsJsonObject().get("longitude").getAsDouble(), -12.9788);
        assertEquals(matchedIoc1.get("source").getAsJsonObject().get("bgp_prefix").getAsString(), "some_prefix");
        assertNotNull(matchedIoc1.get("time").getAsJsonObject().get("observation"));
        assertNotNull(matchedIoc1.get("time").getAsJsonObject().get("received_by_core"));
        assertNotNull(matchedIoc1.get("seen").getAsJsonObject().get("first"));
        assertNotNull(matchedIoc1.get("seen").getAsJsonObject().get("last"));
        assertTrue(matchedIoc1.get("active").getAsBoolean());
    }
}
