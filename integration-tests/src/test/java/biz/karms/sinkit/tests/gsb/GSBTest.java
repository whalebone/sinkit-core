package biz.karms.sinkit.tests.gsb;

import biz.karms.sinkit.ejb.GSBService;
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
import java.util.Set;
import java.util.logging.Logger;

import static org.testng.Assert.*;

/**
 * Created by tom on 11/28/15.
 *
 * @author Tomas Kozel
 */
public class GSBTest extends Arquillian {

    private static final Logger LOGGER = Logger.getLogger(GSBTest.class.getName());
    private static final String TOKEN = System.getenv("SINKIT_ACCESS_TOKEN");

    @EJB
    private GSBService gsbService;

    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, priority = 200)
    @OperateOnDeployment("ear")
    @RunAsClient
    public void clearGSBCacheTest(@ArquillianResource URL context) throws Exception {
        WebClient webClient = new WebClient();
        WebRequest requestSettings = new WebRequest(new URL(context + "rest/gsb"), HttpMethod.DELETE);
        requestSettings.setAdditionalHeader("Content-Type", "application/json");
        requestSettings.setAdditionalHeader("X-sinkit-token", TOKEN);
        Page page = webClient.getPage(requestSettings);
        assertEquals(200, page.getWebResponse().getStatusCode());
        String responseBody = page.getWebResponse().getContentAsString();
        LOGGER.info("clear gsb cache Response: " + responseBody);
        String expected = "true";
        assertTrue(responseBody.contains(expected), "Should have contained " + expected + ", but got: " + responseBody);
    }

    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, priority = 201)
    @OperateOnDeployment("ear")
    @RunAsClient
    public void putHashPrefixTest(@ArquillianResource URL context) throws Exception {
        WebClient webClient = new WebClient();
        WebRequest requestSettings = new WebRequest(new URL(context + "rest/gsb/aabbccdd"), HttpMethod.PUT);
        requestSettings.setAdditionalHeader("Content-Type", "application/json");
        requestSettings.setAdditionalHeader("X-sinkit-token", TOKEN);
        Page page = webClient.getPage(requestSettings);
        assertEquals(200, page.getWebResponse().getStatusCode());
        String responseBody = page.getWebResponse().getContentAsString();
        LOGGER.info("hashPrefixTest Response:" + responseBody);
        String expected = "true";
        assertTrue(responseBody.contains(expected), "Should have contained " + expected + ", but got: " + responseBody);
    }

    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, priority = 202)
    @OperateOnDeployment("ear")
    @RunAsClient
    public void getStatsTest(@ArquillianResource URL context) throws Exception {
        WebClient webClient = new WebClient();
        WebRequest requestSettings = new WebRequest(new URL(context + "rest/gsb/stats"), HttpMethod.GET);
        requestSettings.setAdditionalHeader("Content-Type", "application/json");
        requestSettings.setAdditionalHeader("X-sinkit-token", TOKEN);
        Page page = webClient.getPage(requestSettings);
        assertEquals(200, page.getWebResponse().getStatusCode());
        String responseBody = page.getWebResponse().getContentAsString();
        LOGGER.info("getStatsTest Response:" + responseBody);
        String expected = "{\"gsbRecords\":1}";
        assertTrue(responseBody.contains(expected), "Should have contained " + expected + ", but got: " + responseBody);
    }

    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, priority = 203)
    @OperateOnDeployment("ear")
    @RunAsClient
    public void removeHashPrefixTest(@ArquillianResource URL context) throws Exception {
        WebClient webClient = new WebClient();
        WebRequest requestSettings = new WebRequest(new URL(context + "rest/gsb/aabbccdd"), HttpMethod.DELETE);
        requestSettings.setAdditionalHeader("Content-Type", "application/json");
        requestSettings.setAdditionalHeader("X-sinkit-token", TOKEN);
        Page page = webClient.getPage(requestSettings);
        assertEquals(200, page.getWebResponse().getStatusCode());
        String responseBody = page.getWebResponse().getContentAsString();
        LOGGER.info("removeHashPrefixTest Response:" + responseBody);
        String expected = "true";
        assertTrue(responseBody.contains(expected), "Should have contained " + expected + ", but got: " + responseBody);
        Thread.sleep(1000);
        requestSettings = new WebRequest(new URL(context + "rest/gsb/stats"), HttpMethod.GET);
        requestSettings.setAdditionalHeader("Content-Type", "application/json");
        requestSettings.setAdditionalHeader("X-sinkit-token", TOKEN);
        page = webClient.getPage(requestSettings);
        assertEquals(200, page.getWebResponse().getStatusCode());
        responseBody = page.getWebResponse().getContentAsString();
        LOGGER.info("removeHashPrefixTest Response:" + responseBody);
        expected = "{\"gsbRecords\":0}";
        assertTrue(responseBody.contains(expected), "Should have contained " + expected + ", but got: " + responseBody);
    }


    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, priority = 204)
    @OperateOnDeployment("ear")
    @RunAsClient
    public void lookupTest(@ArquillianResource URL context) throws Exception {
        WebClient webClient = new WebClient();
        WebRequest requestSettings = new WebRequest(new URL(context + "rest/gsb/cf4b367e"), HttpMethod.PUT);
        requestSettings.setAdditionalHeader("Content-Type", "application/json");
        requestSettings.setAdditionalHeader("X-sinkit-token", TOKEN);
        Page page = webClient.getPage(requestSettings);
        assertEquals(200, page.getWebResponse().getStatusCode());
        String responseBody = page.getWebResponse().getContentAsString();
        LOGGER.info("removeHashPrefixTest Response:" + responseBody);
        String expected = "true";
        assertTrue(responseBody.contains(expected), "Should have contained " + expected + ", but got: " + responseBody);

        requestSettings = new WebRequest(new URL(context + "rest/gsb/lookup/http%3A%2F%2Fgoogle.com%0A"), HttpMethod.GET);
        requestSettings.setAdditionalHeader("Content-Type", "application/json");
        requestSettings.setAdditionalHeader("X-sinkit-token", TOKEN);
        page = webClient.getPage(requestSettings);
        assertEquals(200, page.getWebResponse().getStatusCode());
        responseBody = page.getWebResponse().getContentAsString();
        LOGGER.info("removeHashPrefixTest Response:" + responseBody);
        expected = "[\"goog-malware-shavar\"]";
        assertTrue(responseBody.contains(expected), "Should have contained " + expected + ", but got: " + responseBody);
    }

    //@Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, priority = 205)
    //@OperateOnDeployment("ear")
    //@RunAsClient
    public void clearGSBCacheTest2(@ArquillianResource URL context) throws Exception {
        WebClient webClient = new WebClient();
        WebRequest requestSettings = new WebRequest(new URL(context + "rest/gsb"), HttpMethod.DELETE);
        requestSettings.setAdditionalHeader("Content-Type", "application/json");
        requestSettings.setAdditionalHeader("X-sinkit-token", TOKEN);
        Page page = webClient.getPage(requestSettings);
        assertEquals(200, page.getWebResponse().getStatusCode());
        String responseBody = page.getWebResponse().getContentAsString();
        LOGGER.info("clear gsb cache Response: " + responseBody);
        String expected = "true";
        assertTrue(responseBody.contains(expected), "Should have contained " + expected + ", but got: " + responseBody);

        requestSettings = new WebRequest(new URL(context + "rest/gsb/stats"), HttpMethod.GET);
        requestSettings.setAdditionalHeader("Content-Type", "application/json");
        requestSettings.setAdditionalHeader("X-sinkit-token", TOKEN);
        page = webClient.getPage(requestSettings);
        assertEquals(200, page.getWebResponse().getStatusCode());
        responseBody = page.getWebResponse().getContentAsString();
        LOGGER.info("getStatsTest Response:" + responseBody);
        expected = "{\"gsbRecords\":0}";
        assertTrue(responseBody.contains(expected), "Should have contained " + expected + ", but got: " + responseBody);
    }
}
