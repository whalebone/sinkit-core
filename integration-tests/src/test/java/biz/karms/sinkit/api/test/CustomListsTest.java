package biz.karms.sinkit.api.test;


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
import org.testng.annotations.Test;

import java.io.File;
import java.net.URL;
import java.util.logging.Logger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class CustomListsTest extends Arquillian {

    private static final Logger LOGGER = Logger.getLogger(CustomListsTest.class.getName());
    private static final String TOKEN = System.getenv("SINKIT_ACCESS_TOKEN");

    @Deployment(name = "ear", testable = false)
    public static Archive<?> createTestArchive() {
        return ShrinkWrap.create(ZipImporter.class, "sinkit-ear.ear").importFrom(new File("../ear/target/sinkit-ear.ear")).as(EnterpriseArchive.class);
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
        assertTrue(responseBody.contains("4 RULES PROCESSED 4 PRESENT"));
    }

    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, priority = 1)
    @OperateOnDeployment("ear")
    @RunAsClient
    public void putCustomListsTest(@ArquillianResource URL context) throws Exception {
        WebClient webClient = new WebClient();
        WebRequest requestSettings = new WebRequest(new URL(context + "rest/rules/all"), HttpMethod.PUT);
        requestSettings.setAdditionalHeader("Content-Type", "application/json");
        requestSettings.setAdditionalHeader("X-sinkit-token", TOKEN);
        requestSettings.setRequestBody(
                "[{\"dns_client\":\"10.10.10.10/32\"," +
                  "\"lists\":{\"seznam.cz\":\"W\"," +
                             "\"google.com\":\"B\"," +
                             "\"example.com\":\"L\"}" +
                 "},"+
                 "{\"dns_client\":\"fe80::3ea9:f4ff:fe81:c450/64\"," +
                  "\"lists\":{\"seznam.cz\":\"L\"," +
                             "\"google.com\":\"W\"," +
                             "\"example.com\":\"W\"}" +
                 "}]'\n");
        Page page = webClient.getPage(requestSettings);
        assertEquals(200, page.getWebResponse().getStatusCode());
        String responseBody = page.getWebResponse().getContentAsString();
        LOGGER.info("Response:" + responseBody);
        //assertTrue(responseBody.contains("4 RULES PROCESSED 4 PRESENT"));
    }
}

