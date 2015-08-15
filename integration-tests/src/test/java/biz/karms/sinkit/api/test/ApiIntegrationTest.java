package biz.karms.sinkit.api.test;


import biz.karms.sinkit.api.test.util.IoCFactory;
import biz.karms.sinkit.ejb.ServiceEJB;
import biz.karms.sinkit.ioc.IoCRecord;
import biz.karms.sinkit.ioc.IoCSourceIdType;
import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.File;
import java.net.URL;
import java.util.logging.Logger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author Michal Karm Babacek
 */
public class ApiIntegrationTest extends Arquillian {

    private static final Logger LOGGER = Logger.getLogger(ApiIntegrationTest.class.getName());
    private static final String TOKEN = System.getenv("SINKIT_ACCESS_TOKEN");

    @Deployment(name = "ear", testable = true)
    public static Archive<?> createTestArchive() {
        EnterpriseArchive ear = ShrinkWrap.create(ZipImporter.class, "sinkit-ear.ear").importFrom(new File("../ear/target/sinkit-ear.ear")).as(EnterpriseArchive.class);
        ear.getAsType(JavaArchive.class, "sinkit-ejb.jar").addClass(ApiIntegrationTest.class).addClass(IoCFactory.class);
        return ear;
    }


    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, priority = 0)
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

    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, priority = 1)
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

    @Inject
    ServiceEJB serviceEJB;

    @Test(priority = 2)
    public void addIoCsTest() throws Exception {
        IoCRecord ioCRecord = IoCFactory.getIoCRecord("hosted", "blacklist", "myDocumentId", "feed2", "feed2", "seznam.cz", IoCSourceIdType.FQDN, "seznam.cz", null, "seznam.cz");
        assertTrue(serviceEJB.dropTheWholeCache(), "Dropping the whole cache failed.");
        assertTrue(serviceEJB.addToCache(ioCRecord), "Adding a new IoC to a presumably empty cache failed.");
    }

    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, priority = 3)
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

    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, priority = 4)
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

    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, priority = 5)
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

    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, priority = 6)
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
}

