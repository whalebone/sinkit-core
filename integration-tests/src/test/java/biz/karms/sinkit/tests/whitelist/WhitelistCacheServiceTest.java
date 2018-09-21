package biz.karms.sinkit.tests.whitelist;

import biz.karms.sinkit.ejb.ArchiveService;
import biz.karms.sinkit.ejb.BlacklistCacheService;
import biz.karms.sinkit.ejb.CoreService;
import biz.karms.sinkit.ejb.WebApi;
import biz.karms.sinkit.ejb.WhitelistCacheService;
import biz.karms.sinkit.ejb.cache.pojo.BlacklistedRecord;
import biz.karms.sinkit.ejb.cache.pojo.WhitelistedRecord;
import biz.karms.sinkit.ioc.IoCRecord;
import biz.karms.sinkit.ioc.IoCSourceIdType;
import biz.karms.sinkit.tests.util.IoCFactory;
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
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * Created by tkozel on 1/9/16.
 */
public class WhitelistCacheServiceTest extends Arquillian {

    private static final Logger LOGGER = Logger.getLogger(WhitelistCacheServiceTest.class.getName());
    private static final String TOKEN = System.getenv("SINKIT_ACCESS_TOKEN");
    private static final int VALID_HOURS = Integer.parseInt(System.getenv("SINKIT_WHITELIST_VALID_HOURS"));

    @EJB
    WhitelistCacheService whitelistService;

    @EJB
    CoreService coreService;

    @EJB
    WebApi webApi;

    @EJB
    ArchiveService archiveService;

    @EJB
    BlacklistCacheService blacklistCacheService;

    /**
     * put white list entry
     * get white list entry
     * assert its correct data
     */
    @Test(enabled = true, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, priority = 300)
    public void putAndGetTest() throws Exception {
        LOGGER.log(Level.INFO, "putAndGetTest");
        Calendar before = Calendar.getInstance();
        before.add(Calendar.SECOND, VALID_HOURS * 3600 - 1);
        assertTrue(coreService.processWhitelistIoCRecord(IoCFactory.getIoCForWhitelist("1.2.3.4", null, "whitelist", false)));
        assertTrue(coreService.processWhitelistIoCRecord(IoCFactory.getIoCForWhitelist(null, "whalebone.io", "whalebone", false)));
        Calendar after = Calendar.getInstance();
        after.add(Calendar.SECOND, VALID_HOURS * 3600 + 1);
        WhitelistedRecord whiteIP = whitelistService.get("1.2.3.4");
        WhitelistedRecord whiteFQDN = whitelistService.get("whalebone.io");
        assertNotNull(whiteIP);
        assertNotNull(whiteFQDN);
        assertEquals(whiteIP.getRawId(), "1.2.3.4");
        assertEquals(whiteFQDN.getRawId(), "whalebone.io");
        assertEquals(whiteIP.getSourceName(), "whitelist");
        assertEquals(whiteFQDN.getSourceName(), "whalebone");
        assertNotNull(whiteIP.getExpiresAt());
        assertNotNull(whiteFQDN.getExpiresAt());
        assertTrue(before.before(whiteIP.getExpiresAt()));
        assertTrue(before.before(whiteFQDN.getExpiresAt()));
        assertTrue(after.after(whiteIP.getExpiresAt()));
        assertTrue(after.after(whiteFQDN.getExpiresAt()));
    }

    @Test(enabled = true, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, priority = 301)
    public void putAndGetSubdomainTest() throws Exception {
        LOGGER.log(Level.INFO, "putAndGetSubdomainTest");
        assertTrue(coreService.processWhitelistIoCRecord(IoCFactory.getIoCForWhitelist(null, "subdomain.whalebone.cz", "subdomain", false)));
        WhitelistedRecord white = whitelistService.get("subdomain.whalebone.cz");
        WhitelistedRecord theSameWhite = whitelistService.get("whalebone.cz");
        assertNotNull(white);
        assertEquals(white.getRawId(), "subdomain.whalebone.cz");
        assertEquals(white.getSourceName(), "subdomain");
        assertNull(theSameWhite);
    }

    @Test(enabled = true, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, priority = 302)
    public void getNonExistingTest() throws Exception {
        LOGGER.log(Level.INFO, "getNonExistingTest");
        assertNull(whitelistService.get("not exist"));
    }

    /**
     * put whitelist entry
     * put whitelist entry having same fqdn and longer expiration
     * assert the first one has been replaced by the second one
     */
    @Test(enabled = true, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, priority = 303)
    public void putUpdateTest() throws Exception {
        LOGGER.log(Level.INFO, "putUpdateTest");
        Thread.sleep(1001); // get some time for whitelist to get old
        WhitelistedRecord oldWhite = whitelistService.get("whalebone.io");
        assertNotNull(oldWhite);
        assertEquals(oldWhite.getRawId(), "whalebone.io");
        assertEquals(oldWhite.getSourceName(), "whalebone");
        assertNotNull(oldWhite.getExpiresAt());
        Calendar before = Calendar.getInstance();
        before.add(Calendar.SECOND, VALID_HOURS * 3600 - 1);
        assertTrue(coreService.processWhitelistIoCRecord(IoCFactory.getIoCForWhitelist(null, "whalebone.io", "newWhalebone", false)));
        WhitelistedRecord white = whitelistService.get("whalebone.io");
        int counter = 10;
        while ((white == null || !"newWhalebone".equals(white.getSourceName())) && counter > 0) {
            Thread.sleep(100);
            white = whitelistService.get("whalebone.io");
            counter--;
        }
        Calendar after = Calendar.getInstance();
        after.add(Calendar.SECOND, VALID_HOURS * 3600 + 1);
        assertNotNull(white);
        assertEquals(white.getRawId(), "whalebone.io");
        assertEquals(white.getSourceName(), "newWhalebone");
        assertNotNull(white.getExpiresAt());
        assertTrue(before.before(white.getExpiresAt()));
        assertTrue(after.after(white.getExpiresAt()));
        assertNotEquals(oldWhite.getSourceName(), white.getSourceName());
        assertNotEquals(oldWhite.getExpiresAt(), white.getExpiresAt());
    }

    /**
     * put whitelist entry
     * put second whitelist entry having same fqdn and shorter expiration
     * assert the first one is not touched and the second is discarded
     */
    @Test(enabled = true, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, priority = 304)
    public void putNotUpdateTest() throws Exception {
        LOGGER.log(Level.INFO, "putNotUpdateTest");
        assertTrue(coreService.processWhitelistIoCRecord(IoCFactory.getIoCForWhitelist(null, "whalebone.net", "oldWhalebone", false)));
        coreService.setWhitelistValidSeconds(1);
        assertTrue(coreService.processWhitelistIoCRecord(IoCFactory.getIoCForWhitelist(null, "whalebone.net", "newWhalebone", false)));
        WhitelistedRecord white = whitelistService.get("whalebone.net");
        Calendar before = Calendar.getInstance();
        before.add(Calendar.SECOND, 10);
        assertEquals(white.getRawId(), "whalebone.net");
        assertEquals(white.getSourceName(), "oldWhalebone");
        assertNotNull(white.getExpiresAt());
        assertTrue(before.before(white.getExpiresAt()));
    }

    /**
     * put whitelist entry and wait until it expires and disappears from whitelist cache
     */
    @Test(enabled = true, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, priority = 305)
    public void expirationTest() throws Exception {
        LOGGER.log(Level.INFO, "expirationTest");
        coreService.setWhitelistValidSeconds(1);
        assertTrue(coreService.processWhitelistIoCRecord(IoCFactory.getIoCForWhitelist(null, "willExpire", "nothing", false)));
        assertNotNull(whitelistService.get("willExpire"));
        int counter = 0;
        WhitelistedRecord white = whitelistService.get("willExpire");
        while (white != null && counter < 10) {
            Thread.sleep(100);
            white = whitelistService.get("willExpire");
            counter++;
        }
        assertNull(white);
    }

    @Test(enabled = true, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, priority = 306)
    public void dropTheWholeCacheTest() throws Exception {
        LOGGER.log(Level.INFO, "dropTheWholeCacheTest");
        WhitelistedRecord whiteIP = whitelistService.get("1.2.3.4");
        assertNotNull(whiteIP);
        WhitelistedRecord whiteFQDNio = whitelistService.get("whalebone.io");
        assertNotNull(whiteFQDNio);
        WhitelistedRecord whiteFQDNcz = whitelistService.get("subdomain.whalebone.cz");
        assertNotNull(whiteFQDNcz);
        assertTrue(whitelistService.dropTheWholeCache());
        whiteIP = whitelistService.get("1.2.3.4");
        whiteFQDNio = whitelistService.get("whalebone.io");
        whiteFQDNcz = whitelistService.get("subdomain.whalebone.cz");
        int counter = 0;
        while ((whiteIP != null || whiteFQDNio != null || whiteFQDNcz != null) && counter < 10) {
            Thread.sleep(100);
            whiteIP = whitelistService.get("1.2.3.4");
            whiteFQDNio = whitelistService.get("whalebone.io");
            whiteFQDNcz = whitelistService.get("subdomain.whalebone.cz");
            counter++;
        }
        assertNull(whiteIP);
        assertNull(whiteFQDNio);
        assertNull(whiteFQDNcz);
        counter = 0;
        while (!whitelistService.isWhitelistEmpty() && counter < 10) {
            counter++;
        }
        assertTrue(whitelistService.isWhitelistEmpty());
    }

    /**
     * put 3 IoCs: phishing.cz, subdomain.phishing.cz, phishing.ru
     * put whitelist entry: phishing.cz
     * assert phishing.cz and subdomain.phishing.cz were whitelisted and removed from infinispan blacklist
     * assert phishing.ru is untouched in archive and in infinispan blacklist
     */
    @Test(enabled = true, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, priority = 307)
    public void existingIoCsAreWhitelistedTest() throws Exception {
        LOGGER.log(Level.INFO, "whitelistedIoCTest");
        coreService.setWhitelistValidSeconds(VALID_HOURS * 3600);
        IoCRecord whitelisted = IoCFactory.getIoCRecordAsRecieved("feed", "phishing", "phishing.cz", IoCSourceIdType.FQDN, Calendar.getInstance().getTime(), null);
        IoCRecord whitelistedSubdomain = IoCFactory.getIoCRecordAsRecieved("feed", "phishing", "subdomain.phishing.cz", IoCSourceIdType.FQDN, Calendar.getInstance().getTime(), null);
        IoCRecord notWhitelisted = IoCFactory.getIoCRecordAsRecieved("feed", "phishing", "phishing.ru", IoCSourceIdType.FQDN, Calendar.getInstance().getTime(), null);
        whitelisted = coreService.processIoCRecord(whitelisted);
        String whiteSourceId = whitelisted.getSource().getId().getValue();
        String whiteDocId = whitelisted.getDocumentId();
        whitelistedSubdomain = coreService.processIoCRecord(whitelistedSubdomain);
        String whiteSubSourceId = whitelistedSubdomain.getSource().getId().getValue();
        String whiteSubDocId = whitelistedSubdomain.getDocumentId();
        assertNotEquals(whiteDocId, whiteSubDocId);
        assertNotEquals(whiteSourceId, whiteSubSourceId);
        notWhitelisted = coreService.processIoCRecord(notWhitelisted);
        String notWhiteSourceId = notWhitelisted.getSource().getId().getValue();
        String notWhiteDocId = notWhitelisted.getDocumentId();
        BlacklistedRecord whiteBR = webApi.getBlacklistedRecord(whiteSourceId);
        BlacklistedRecord whiteSubBR = webApi.getBlacklistedRecord(whiteSubSourceId);
        BlacklistedRecord notWhiteBR = webApi.getBlacklistedRecord(notWhiteSourceId);
        whitelisted = archiveService.getIoCRecordById(whiteDocId);
        whitelistedSubdomain = archiveService.getIoCRecordById(whiteSubDocId);
        notWhitelisted = archiveService.getIoCRecordById(notWhiteDocId);
        int counter = 0;
        while ((whiteBR == null || whiteSubBR == null || notWhiteBR == null ||
                whitelisted == null || whitelistedSubdomain == null || notWhitelisted == null) && counter < 10) {
            Thread.sleep(100);
            whiteBR = webApi.getBlacklistedRecord(whiteSourceId);
            whiteSubBR = webApi.getBlacklistedRecord(whiteSubSourceId);
            notWhiteBR = webApi.getBlacklistedRecord(notWhiteSourceId);
            whitelisted = archiveService.getIoCRecordById(whiteDocId);
            whitelistedSubdomain = archiveService.getIoCRecordById(whiteSubDocId);
            notWhitelisted = archiveService.getIoCRecordById(notWhiteDocId);
            counter++;
        }
        assertNotNull(whiteBR);
        assertNotNull(whiteSubBR);
        assertNotNull(notWhiteBR);
        assertNotNull(whitelisted);
        assertNotNull(whitelistedSubdomain);
        assertNotNull(notWhitelisted);
        Calendar before = Calendar.getInstance();
        before.add(Calendar.SECOND, -1);
        assertTrue(coreService.processWhitelistIoCRecord(IoCFactory.getIoCForWhitelist(null, "phishing.cz", "white", false)));
        counter = 0;
        whitelisted = archiveService.getIoCRecordById(whitelisted.getDocumentId());
        assertNotNull(whitelisted);
        whitelistedSubdomain = archiveService.getIoCRecordById(whitelistedSubdomain.getDocumentId());
        assertNotNull(whitelistedSubdomain);
        whiteBR = webApi.getBlacklistedRecord(whitelisted.getSource().getId().getValue());
        whiteSubBR = webApi.getBlacklistedRecord(whitelistedSubdomain.getSource().getId().getValue());
        while ((whitelisted.getWhitelistName() == null || whiteBR != null ||
                whitelistedSubdomain.getWhitelistName() == null || whiteSubBR != null) && counter < 10) {
            Thread.sleep(100);
            whitelisted = archiveService.getIoCRecordById(whitelisted.getDocumentId());
            assertNotNull(whitelisted, "Something strange has happend. While waiting for the ioc to be set as whitelisted, it disappeared...");
            whitelistedSubdomain = archiveService.getIoCRecordById(whitelistedSubdomain.getDocumentId());
            assertNotNull(whitelistedSubdomain, "Something strange has happend. While waiting for the ioc to be set as whitelisted, it disappeared...");
            whiteBR = webApi.getBlacklistedRecord(whitelisted.getSource().getId().getValue());
            whiteSubBR = webApi.getBlacklistedRecord(whitelistedSubdomain.getSource().getId().getValue());
            counter++;
        }
        assertNotNull(whitelisted.getWhitelistName());
        assertNotNull(whitelistedSubdomain.getWhitelistName());
        assertNull(whiteBR);
        assertNull(whiteSubBR);
        Calendar after = Calendar.getInstance();
        after.add(Calendar.SECOND, 1);
        assertEquals(whitelisted.getWhitelistName(), "white");
        assertEquals(whitelistedSubdomain.getWhitelistName(), "white");
        assertNotNull(whitelisted.getTime().getWhitelisted());
        assertNotNull(whitelistedSubdomain.getTime().getWhitelisted());
        assertTrue(before.getTimeInMillis() < whitelisted.getTime().getWhitelisted().getTime());
        assertTrue(before.getTimeInMillis() < whitelistedSubdomain.getTime().getWhitelisted().getTime());
        assertTrue(after.getTimeInMillis() > whitelisted.getTime().getWhitelisted().getTime());
        assertTrue(after.getTimeInMillis() > whitelistedSubdomain.getTime().getWhitelisted().getTime());
        notWhitelisted = archiveService.getIoCRecordById(notWhitelisted.getDocumentId());
        assertNotNull(notWhitelisted);
        assertNull(notWhitelisted.getWhitelistName());
        assertNull(notWhitelisted.getTime().getWhitelisted());
    }

    //TODO: This tests is obsolete (we now store entries that are on whitelist in blacklistcache, but with parameter whitelisted

    /**
     * put whitelist entries for IP and FQDN
     * put iocs having same IP or FQDN -> should be whitelisted
     * put iocs having different IP or FQDN -> shouldn't be whitelisted
     * assert whitelist and blacklist entries
     */
    @Test(enabled = false, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, priority = 307)
    public void newIoCIsWhitelistedTest() throws Exception {
        LOGGER.log(Level.INFO, "newIoCIsWhitelistedTest");
        coreService.setWhitelistValidSeconds(VALID_HOURS * 3600);
        assertTrue(coreService.processWhitelistIoCRecord(IoCFactory.getIoCForWhitelist(null, "trusted.xoxo", "global", false)));
        assertTrue(coreService.processWhitelistIoCRecord(IoCFactory.getIoCForWhitelist("172.0.0.1", null, "local", false)));
        Calendar before = Calendar.getInstance();
        before.add(Calendar.SECOND, -1);
        IoCRecord trusted = coreService.processIoCRecord(
                IoCFactory.getIoCRecordAsRecieved("feed", "malware", "trusted.xoxo", IoCSourceIdType.FQDN, Calendar.getInstance().getTime(), null));
        assertNotNull(trusted);
        String trustedDocId = trusted.getDocumentId();

        IoCRecord veryTrusted = coreService.processIoCRecord(
                IoCFactory.getIoCRecordAsRecieved("feed", "malware", "very.trusted.xoxo", IoCSourceIdType.FQDN, Calendar.getInstance().getTime(), null));
        assertNotNull(veryTrusted);
        String veryTrustedDocId = veryTrusted.getDocumentId();

        IoCRecord whiteIP = coreService.processIoCRecord(
                IoCFactory.getIoCRecordAsRecieved("feed", "malware", "172.0.0.1", IoCSourceIdType.IP, Calendar.getInstance().getTime(), null));
        assertNotNull(whiteIP);
        String whiteIPDocId = whiteIP.getDocumentId();

        IoCRecord evil = coreService.processIoCRecord(
                IoCFactory.getIoCRecordAsRecieved("feed", "malware", "evil.domain.cz", IoCSourceIdType.FQDN, Calendar.getInstance().getTime(), null));
        assertNotNull(evil);
        String evilDocId = evil.getDocumentId();

        IoCRecord evilIP = coreService.processIoCRecord(
                IoCFactory.getIoCRecordAsRecieved("feed", "malware", "0.6.6.6", IoCSourceIdType.IP, Calendar.getInstance().getTime(), null));
        assertNotNull(evilIP);
        String evilIPDocId = evilIP.getDocumentId();

        trusted = archiveService.getIoCRecordById(trustedDocId);
        veryTrusted = archiveService.getIoCRecordById(veryTrustedDocId);
        whiteIP = archiveService.getIoCRecordById(whiteIPDocId);
        evil = archiveService.getIoCRecordById(evilDocId);
        evilIP = archiveService.getIoCRecordById(evilIPDocId);
        int counter = 0;
        while ((trusted == null || veryTrusted == null || whiteIP == null || evil == null || evilIP == null)
                && counter < 10) {
            Thread.sleep(100);
            trusted = archiveService.getIoCRecordById(trustedDocId);
            veryTrusted = archiveService.getIoCRecordById(veryTrustedDocId);
            whiteIP = archiveService.getIoCRecordById(whiteIPDocId);
            evil = archiveService.getIoCRecordById(evilDocId);
            evilIP = archiveService.getIoCRecordById(evilIPDocId);
            counter++;
        }
        assertNotNull(trusted);
        assertNotNull(veryTrusted);
        assertNotNull(whiteIP);
        assertNotNull(evil);
        assertNotNull(evilIP);
        Calendar after = Calendar.getInstance();
        after.add(Calendar.SECOND, +1);

        assertNotNull(trusted.getWhitelistName());
        assertEquals(trusted.getWhitelistName(), "global");
        assertNotNull(trusted.getTime().getWhitelisted());
        assertTrue(before.getTimeInMillis() < trusted.getTime().getWhitelisted().getTime(), before.getTimeInMillis() + " = " + trusted.getTime().getWhitelisted().getTime());
        assertTrue(after.getTimeInMillis() > trusted.getTime().getWhitelisted().getTime());

        assertNull(webApi.getBlacklistedRecord(trusted.getSource().getId().getValue()));

        assertNotNull(veryTrusted.getWhitelistName());
        assertEquals(veryTrusted.getWhitelistName(), "global");
        assertNotNull(veryTrusted.getTime().getWhitelisted());
        assertTrue(before.getTimeInMillis() < veryTrusted.getTime().getWhitelisted().getTime());
        assertTrue(after.getTimeInMillis() > veryTrusted.getTime().getWhitelisted().getTime());
        assertNull(webApi.getBlacklistedRecord(veryTrusted.getSource().getId().getValue()));

        assertNotNull(whiteIP.getWhitelistName());
        assertEquals(whiteIP.getWhitelistName(), "local");
        assertNotNull(whiteIP.getTime().getWhitelisted());
        assertTrue(before.getTimeInMillis() < whiteIP.getTime().getWhitelisted().getTime());
        assertTrue(after.getTimeInMillis() > whiteIP.getTime().getWhitelisted().getTime());
        assertNull(webApi.getBlacklistedRecord(whiteIP.getSource().getId().getValue()));

        assertNull(evil.getWhitelistName());
        assertNull(evil.getTime().getWhitelisted());
        assertNotNull(webApi.getBlacklistedRecord(evil.getSource().getId().getValue()));

        assertNull(evilIP.getWhitelistName());
        assertNull(evilIP.getTime().getWhitelisted());
        assertNotNull(webApi.getBlacklistedRecord(evilIP.getSource().getId().getValue()));
    }

    /**
     * put ioc FQDN that is supposed to be whitelisted
     * do lookup -> response must be sinkholed since whitelist entry's not been put yet
     * put whitelist FQDN entry
     * put whitelist IP entry
     * put ioc IP that is supposed to be whitelisted
     * do FQDN lookup -> response mustn't be sinkholed
     * do IP lookup -> response mustn't be sinkholed
     */
    @Test(enabled = true, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, priority = 308)
    @OperateOnDeployment("ear")
    @RunAsClient
    public void endToEndTest(@ArquillianResource URL context) throws Exception {
        WebClient webClient = new WebClient();

        // Feed config
        WebRequest requestSettingsFeed = new WebRequest(new URL(context + "rest/rules/all"), HttpMethod.POST);
        requestSettingsFeed.setAdditionalHeader("Content-Type", "application/json");
        requestSettingsFeed.setAdditionalHeader("X-sinkit-token", TOKEN);
        requestSettingsFeed.setRequestBody("[{\"dns_client\":\"254.1.1.1/32\",\"settings\":{\"some-intelmq-feed-to-sink\":\"S\", \"feed\":\"S\"},\"customer_id\":111,\"customer_name\":\"Whitelist User\"}]");
        Page pageFeed = webClient.getPage(requestSettingsFeed);
        assertEquals(HttpURLConnection.HTTP_OK, pageFeed.getWebResponse().getStatusCode());
        String responseBodyFeed = pageFeed.getWebResponse().getContentAsString();
        LOGGER.info("endToEnd Response:" + responseBodyFeed);
        assertTrue(responseBodyFeed.contains("1 RULES PROCESSED"));

        //add ioc fqdn
        // 2015-12-12T22:52:58+02:00
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        String observation = df.format(new Date());
        WebRequest requestSettingsIoC = new WebRequest(new URL(context + "rest/blacklist/ioc/"), HttpMethod.POST);
        requestSettingsIoC.setAdditionalHeader("Content-Type", "application/json");
        requestSettingsIoC.setAdditionalHeader("X-sinkit-token", TOKEN);
        requestSettingsIoC.setRequestBody("{\"feed\":{\"name\":\"some-intelmq-feed-to-sink\",\"url\":\"http://example.com/feed.txt\"},\"classification\":{\"type\": \"phishing\",\"taxonomy\": \"Fraud\"},\"raw\":\"aHwwwwfdfBmODQ2N244iNGZiNS8=\",\"source\":{\"fqdn\":\"trusted.domain.to.be.whitelisted.cz\",\"bgp_prefix\":\"some_prefix\",\"asn\":\"3355556\",\"asn_name\":\"any_name\",\"geolocation\":{\"cc\":\"RU\",\"city\":\"City\",\"latitude\":\"85.12645\",\"longitude\":\"-12.9788\"}},\"time\":{\"observation\":\"" + observation + "\"},\"protocol\":{\"application\":\"ssh\"},\"description\":{\"text\":\"description\"}}");
        Page pageIoC = webClient.getPage(requestSettingsIoC);
        assertEquals(HttpURLConnection.HTTP_OK, pageIoC.getWebResponse().getStatusCode());
        String responseBodyIoC = pageIoC.getWebResponse().getContentAsString();
        LOGGER.info("endToEndIoC Response:" + responseBodyIoC);
        assertTrue(responseBodyIoC.contains("\"fqdn\":\"trusted.domain.to.be.whitelisted.cz\""));

        //Checking that our entry is there
        TimeUnit.SECONDS.sleep(2);
        WebRequest requestSettings = new WebRequest(new URL(context + "rest/blacklist/record/trusted.domain.to.be.whitelisted.cz"), HttpMethod.GET);
        requestSettings.setAdditionalHeader("Content-Type", "application/json");
        requestSettings.setAdditionalHeader("X-sinkit-token", TOKEN);
        Page pageBlack = webClient.getPage(requestSettings);
        assertEquals(HttpURLConnection.HTTP_OK, pageBlack.getWebResponse().getStatusCode());
        String responseBody = pageBlack.getWebResponse().getContentAsString();
        LOGGER.info("endToEndBlacklist Response:" + responseBody);

        //dns query should be blacklisted

        WebRequest requestSettingsDNS = new WebRequest(new URL(context + "rest/blacklist/dns/254.1.1.1/trusted.domain.to.be.whitelisted.cz/trusted.domain.to.be.whitelisted.cz"), HttpMethod.GET);
        requestSettingsDNS.setAdditionalHeader("Content-Type", "application/json");
        requestSettingsDNS.setAdditionalHeader("X-sinkit-token", TOKEN);
        Page pageDNS = webClient.getPage(requestSettingsDNS);
        assertEquals(HttpURLConnection.HTTP_OK, pageDNS.getWebResponse().getStatusCode());
        String responseBodyDNS = pageDNS.getWebResponse().getContentAsString();
        LOGGER.info("endToEndDNS Response:" + responseBodyDNS);
        assertTrue(responseBodyDNS.contains("{\"sinkhole\":\"" + System.getenv("SINKIT_SINKHOLE_IP") + "\"}"),
                "Response body was " + responseBodyDNS + "Should have contained sinkhole IP");

        //add whitelist entry FQDN
        // 2015-12-12T22:52:58+02:00
        WebRequest requestSettingsWhite = new WebRequest(new URL(context + "rest/whitelist/ioc/"), HttpMethod.POST);
        requestSettingsWhite.setAdditionalHeader("Content-Type", "application/json");
        requestSettingsWhite.setAdditionalHeader("X-sinkit-token", TOKEN);
        requestSettingsWhite.setRequestBody("{\"feed\":{\"name\":\"end2endWhitelist\"},\"source\":{\"fqdn\":\"trusted.domain.to.be.whitelisted.cz\"}}");
        Page pageWhite = webClient.getPage(requestSettingsWhite);
        assertEquals(HttpURLConnection.HTTP_OK, pageWhite.getWebResponse().getStatusCode());
        String responseBodyWhite = pageWhite.getWebResponse().getContentAsString();
        LOGGER.info("endToEndWhite Response:" + responseBodyWhite);
        assertTrue(responseBodyIoC.contains("true"));

        //add whitelist entry IP
        requestSettingsWhite.setRequestBody("{\"feed\":{\"name\":\"end2endWhitelist\"},\"source\":{\"ip\":\"83.215.22.31\"}}");
        pageWhite = webClient.getPage(requestSettingsWhite);
        assertEquals(HttpURLConnection.HTTP_OK, pageWhite.getWebResponse().getStatusCode());
        responseBodyWhite = pageWhite.getWebResponse().getContentAsString();
        LOGGER.info("endToEndWhite Response:" + responseBodyWhite);
        assertTrue(responseBodyIoC.contains("true"));

        //add ioc IP
        requestSettingsIoC.setRequestBody("{\"feed\":{\"name\":\"some-intelmq-feed-to-sink\",\"url\":\"http://example.com/feed.txt\"},\"classification\":{\"type\": \"phishing\",\"taxonomy\": \"Fraud\"},\"raw\":\"aHwwwwfdfBmODQ2N244iNGZiNS8=\",\"source\":{\"ip\":\"83.215.22.31\",\"bgp_prefix\":\"some_prefix\",\"asn\":\"3355556\",\"asn_name\":\"any_name\",\"geolocation\":{\"cc\":\"RU\",\"city\":\"City\",\"latitude\":\"85.12645\",\"longitude\":\"-12.9788\"}},\"time\":{\"observation\":\"" + observation + "\"},\"protocol\":{\"application\":\"ssh\"},\"description\":{\"text\":\"description\"}}");
        pageIoC = webClient.getPage(requestSettingsIoC);
        assertEquals(HttpURLConnection.HTTP_OK, pageIoC.getWebResponse().getStatusCode());
        responseBodyIoC = pageIoC.getWebResponse().getContentAsString();
        LOGGER.info("endToEndIoC Response:" + responseBodyIoC);
        assertTrue(responseBodyIoC.contains("\"ip\":\"83.215.22.31\""));

        //dns query (FQDN) should be whitelisted (not sinkholed)
        pageDNS = webClient.getPage(requestSettingsDNS);
        assertEquals(HttpURLConnection.HTTP_OK, pageDNS.getWebResponse().getStatusCode());
        responseBodyDNS = pageDNS.getWebResponse().getContentAsString();
        LOGGER.info("endToEndDNSWhitelisted Response:" + responseBodyDNS);
        assertTrue(responseBodyDNS.contains("null"), "Expected to contain: null, got:" + responseBodyDNS);

        //dns query (IP) should be whitelisted (not sinkholed)
        requestSettingsDNS = new WebRequest(new URL(context + "rest/blacklist/dns/254.1.1.1/83.215.22.31/trusted.domain.to.be.whitelisted.cz"), HttpMethod.GET);
        requestSettingsDNS.setAdditionalHeader("Content-Type", "application/json");
        requestSettingsDNS.setAdditionalHeader("X-sinkit-token", TOKEN);
        pageDNS = webClient.getPage(requestSettingsDNS);
        assertEquals(HttpURLConnection.HTTP_OK, pageDNS.getWebResponse().getStatusCode());
        responseBodyDNS = pageDNS.getWebResponse().getContentAsString();
        LOGGER.info("endToEndDNSWhitelisted Response:" + responseBodyDNS);
        assertTrue(responseBodyDNS.contains("null"));

        // TODO uncomment when logic, that checks fqdn (last param of the request) after ip was not found on blacklist, is implemented
//        //dns query (IP) should be blacklisted because of evil.domain.cz is on blacklist
//        requestSettingsDNS = new WebRequest(new URL(context + "rest/blacklist/dns/254.1.1.1/83.215.22.31/evil.domain.cz"), HttpMethod.GET);
//        requestSettingsDNS.setAdditionalHeader("Content-Type", "application/json");
//        requestSettingsDNS.setAdditionalHeader("X-sinkit-token", TOKEN);
//        pageDNS = webClient.getPage(requestSettingsDNS);
//        assertEquals(HttpURLConnection.HTTP_OK, pageDNS.getWebResponse().getStatusCode());
//        responseBodyDNS = pageDNS.getWebResponse().getContentAsString();
//        LOGGER.info("endToEndDNSWhitelisted Response:" + responseBodyDNS);
//        assertTrue(responseBodyIoC.contains("\"ip\":\"83.215.22.31\""));
    }

    @Test(enabled = true, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, priority = 309)
    @OperateOnDeployment("ear")
    @RunAsClient
    public void testRESTApi(@ArquillianResource URL context) throws Exception {
        WebClient webClient = new WebClient();

        WebRequest request = new WebRequest(new URL(context + "rest/whitelist/record/trusted.domain.to.be.whitelisted.cz"), HttpMethod.GET);
        request.setAdditionalHeader("Content-Type", "application/json");
        request.setAdditionalHeader("X-sinkit-token", TOKEN);
        Page page = webClient.getPage(request);
        assertEquals(HttpURLConnection.HTTP_OK, page.getWebResponse().getStatusCode());
        String responseBody = page.getWebResponse().getContentAsString();
        LOGGER.info("RESTApi Response:" + responseBody);
        assertTrue(responseBody.contains("trusted.domain.to.be.whitelisted.cz"),
                "Response body should have contained trusted.domain.to.be.whitelisted.cz, got:" + responseBody);

        request = new WebRequest(new URL(context + "rest/whitelist/record/trusted.domain.to.be.whitelisted.cz"), HttpMethod.GET);
        request.setAdditionalHeader("Content-Type", "application/json");
        request.setAdditionalHeader("X-sinkit-token", TOKEN);
        page = webClient.getPage(request);
        assertEquals(HttpURLConnection.HTTP_OK, page.getWebResponse().getStatusCode());
        responseBody = page.getWebResponse().getContentAsString();
        LOGGER.info("RESTApi Response:" + responseBody);
        assertTrue(responseBody.contains("trusted.domain.to.be.whitelisted.cz"));

        request = new WebRequest(new URL(context + "rest/whitelist/record/trusted.domain.to.be.whitelisted.cz"), HttpMethod.DELETE);
        request.setAdditionalHeader("Content-Type", "application/json");
        request.setAdditionalHeader("X-sinkit-token", TOKEN);
        page = webClient.getPage(request);
        assertEquals(HttpURLConnection.HTTP_OK, page.getWebResponse().getStatusCode());
        responseBody = page.getWebResponse().getContentAsString();
        LOGGER.info("RESTApi Response:" + responseBody);
        assertTrue(responseBody.contains("true"));

        //TODO: What do we want to test here? Stats is now different
        /*
        request = new WebRequest(new URL(context + "rest/stats/"), HttpMethod.GET);
        request.setAdditionalHeader("Content-Type", "application/json");
        request.setAdditionalHeader("X-sinkit-token", TOKEN);
        page = webClient.getPage(request);
        assertEquals(HttpURLConnection.HTTP_OK, page.getWebResponse().getStatusCode());
        responseBody = page.getWebResponse().getContentAsString();
        LOGGER.info("RESTApi Response:" + responseBody);
        assertTrue(responseBody.contains("5"));
        */
    }
}
