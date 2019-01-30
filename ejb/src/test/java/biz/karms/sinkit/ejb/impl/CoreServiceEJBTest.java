package biz.karms.sinkit.ejb.impl;

import biz.karms.sinkit.ejb.ArchiveService;
import biz.karms.sinkit.ejb.BlacklistCacheService;
import biz.karms.sinkit.ejb.WhitelistCacheService;
import biz.karms.sinkit.ejb.cache.pojo.WhitelistedRecord;
import biz.karms.sinkit.ioc.IoCAccuCheckerMetadata;
import biz.karms.sinkit.ioc.IoCAccuCheckerReport;
import biz.karms.sinkit.exception.TooOldIoCException;

import biz.karms.sinkit.ioc.IoCClassification;
import biz.karms.sinkit.ioc.IoCFeed;
import biz.karms.sinkit.ioc.IoCRecord;
import biz.karms.sinkit.ioc.IoCSource;
import biz.karms.sinkit.ioc.IoCSourceId;
import biz.karms.sinkit.ioc.IoCSourceIdType;
import biz.karms.sinkit.ioc.IoCTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Date;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Tomas Kozel
 */
@RunWith(MockitoJUnitRunner.class)
public class CoreServiceEJBTest {

    @Mock
    private Logger log;

    @Mock
    private WhitelistCacheService whitelistCacheService;

    @Mock
    private ArchiveService archiveService;

    @Mock
    private BlacklistCacheService blacklistCacheService;

    @InjectMocks
    private CoreServiceEJB coreService;

    @Before
    public void setUp () throws Exception {
        Field hours = CoreServiceEJB.class.getDeclaredField("iocActiveHours");
        hours.setAccessible(true);
        hours.set(coreService,24);
    }

    /**
     * Tests the update of accuracy with a report from AccuChecker
     *
     * @throws Exception
     */
    @Test
    public void updateWithAccuCheckerReportTest() throws Exception {

        //prepare
        IoCRecord ioc1 = getIoCForWhitelist(null, "mal.com", "OneFeed", true);
        ioc1.setDocumentId("1");
        //give this ioc a feed accuracy
        HashMap<String, Integer> feed_accuracy_1 = new HashMap<>();
        feed_accuracy_1.put("feed", 80);
        ioc1.setAccuracy(feed_accuracy_1);

        //2nd ioc, feed accuracy setup
        IoCRecord ioc2 = getIoCForWhitelist(null, "mal.com", "SomeOtherFeed", true);
        ioc2.setDocumentId("2");
        HashMap<String, Integer> feed_accuracy_2 = new HashMap<>();
        feed_accuracy_2.put("feed", 50);
        ioc2.setAccuracy(feed_accuracy_2);

        //accuchecker report setup
        IoCRecord report = getIoCForWhitelist(null, "mal.com", null, true);
        HashMap<String, Integer> accuracy = new HashMap<>();
        accuracy.put("SomeAccuracyProvider", 20);
        report.setAccuracy(accuracy);
        HashMap<String, IoCAccuCheckerMetadata> metadata = new HashMap<>();
        IoCAccuCheckerMetadata meta = new IoCAccuCheckerMetadata();
        meta.setContent("SomeAccuracyProvider has no metadata");
        meta.setTimestamp(null);

        metadata.put("SomeAccuracyProvider", meta);
        report.setMetadata(metadata);
        IoCAccuCheckerReport accu_report = new IoCAccuCheckerReport(report);
        List<IoCRecord> iocs = new ArrayList<IoCRecord>();
        iocs.add(ioc1);
        iocs.add(ioc2);
        when(archiveService.getMatchingActiveEntries("source.id.value", "mal.com")).thenReturn(iocs);
        when(archiveService.archiveReceivedIoCRecord(ioc1)).thenReturn(true);
        when(archiveService.archiveReceivedIoCRecord(ioc2)).thenReturn(true);
        when(blacklistCacheService.addToCache(iocs.get(0))).thenReturn(true);
        when(blacklistCacheService.addToCache(iocs.get(1))).thenReturn(true);

        //call tested method
        assertTrue(coreService.updateWithAccuCheckerReport(accu_report));

        //verify
        verify(archiveService).getMatchingActiveEntries("source.id.value", "mal.com");
        verify(archiveService).archiveReceivedIoCRecord(ioc1);
        verify(archiveService).archiveReceivedIoCRecord(ioc2);
        verify(blacklistCacheService).addToCache(iocs.get(0));
        verify(blacklistCacheService).addToCache(iocs.get(1));
        assertEquals(new Integer(80), iocs.get(0).getAccuracy().get("feed"));
        assertEquals(new Integer(20), iocs.get(0).getAccuracy().get("SomeAccuracyProvider"));
        assertEquals(new Integer(50), iocs.get(1).getAccuracy().get("feed"));
        assertEquals(new Integer(20), iocs.get(1).getAccuracy().get("SomeAccuracyProvider"));
        verifyNoMoreInteractions(archiveService);
        verifyNoMoreInteractions(blacklistCacheService);
    }

    /**
     * Tests the processIoCRecord method for the case when the ioc sent to it is not on whitelist and is active.
     * @throws Exception
     */
    @Test
    public void processNotOnWhitelistActiveTest() throws Exception{
        //prepare
        IoCRecord ioc = getIoCRecord(null, "gooddomain.malware.com","SomeFeed", 1, "malware");
        // IoC is newer than oldest we allow 1<24
        when(whitelistCacheService.get("malware.com")).thenReturn(null);
        when(whitelistCacheService.get("gooddomain.malware.com")).thenReturn(null);

        //call tested method
        coreService.processIoCRecord(ioc);

        //verify
        verify(whitelistCacheService).get("malware.com");
        verify(whitelistCacheService).get("gooddomain.malware.com");
        assertEquals(null,ioc.getWhitelistName());
        verify(archiveService).archiveReceivedIoCRecord(ioc);
        verify(blacklistCacheService).addToCache(ioc);
        verifyNoMoreInteractions(archiveService);
        verifyNoMoreInteractions(blacklistCacheService);
        assertNotNull(ioc.getSeen().getFirst());
        assertNotNull(ioc.getSeen().getLast());
        assertNotNull(ioc.getTime().getReceivedByCore());
        assertEquals(ioc.getSource().getId().getValue(),
                "gooddomain.malware.com");
        assertTrue(ioc.getActive());
    }

    /**
     * Tests processIoCRecord method for the case when the entry sent to it is on a whitelist
     * @throws Exception
     */
    @Test
    public void processOnWhitelistActiveTest() throws Exception{
        //preparation
        Calendar expiresAt = Calendar.getInstance();
        expiresAt.add(Calendar.DAY_OF_MONTH, 1);
        IoCRecord ioc= getIoCRecord(null, "notmalware.somewebhosting.com",
                "SomeFeed", 1, "malware");
        // IoC is newer than oldest we allow 1<24
        //somewebhosting.com can host malware
        when(whitelistCacheService.get("somewebhosting.com")).thenReturn(null);
        //however, notmalware.somewebhosting.com is whitelisted
        WhitelistedRecord white = createWhite("notmalware.somewebhosting.com" ,
                "mywhitelist", expiresAt, true);
        when(whitelistCacheService.get("notmalware.somewebhosting.com")).thenReturn(white);

        //run tested method
        coreService.processIoCRecord(ioc);

        //verify
        verify(whitelistCacheService).get("somewebhosting.com");
        verify(whitelistCacheService).get("notmalware.somewebhosting.com");
        verify(archiveService).archiveReceivedIoCRecord(ioc);
        verify(blacklistCacheService).addToCache(ioc);
        verifyNoMoreInteractions(archiveService);
        verifyNoMoreInteractions(blacklistCacheService);
        assertEquals("mywhitelist",ioc.getWhitelistName());
        assertNotNull(ioc.getSeen().getFirst());
        assertNotNull(ioc.getSeen().getLast());
        assertNotNull(ioc.getTime().getReceivedByCore());
        assertNotNull(ioc.getTime().getWhitelisted());
        assertTrue(ioc.getActive());
        assertEquals(ioc.getSource().getId().getValue(),
                "notmalware.somewebhosting.com");
    }

    @Test (expected = TooOldIoCException.class)
    public void processNotActiveTest() throws Exception{
        IoCRecord ioc= getIoCRecord(null, "somedomain.malware.com","SomeFeed", 52, "malware");
        // IoC is older than oldest we allow 52>24
        coreService.processIoCRecord(ioc);
    }

    @Test
    public void     processExistingCompletedNoUpdateTest() throws Exception {
        Calendar expiresAt = Calendar.getInstance();
        expiresAt.add(Calendar.DAY_OF_MONTH, 1);
        WhitelistedRecord white = createWhite("whalebone.io", "whalebone", expiresAt, true);
        IoCRecord ioc = getIoCForWhitelist(null, "whalebone.io", "whalebone", true);
        when(whitelistCacheService.get("whalebone.io")).thenReturn(white);

        assertTrue(coreService.processWhitelistIoCRecord(ioc));

        verify(whitelistCacheService).get("whalebone.io");
        verifyNoMoreInteractions(whitelistCacheService);
        verifyZeroInteractions(archiveService);
        verifyZeroInteractions(blacklistCacheService);
    }

    @Test
    public void processExistingNotCompletedNoUpdateTest() throws Exception {
        Calendar expiresAt = Calendar.getInstance();
        expiresAt.add(Calendar.DAY_OF_MONTH, 1);
        WhitelistedRecord white = createWhite("whalebone.io", "whalebone", expiresAt, false);
        IoCRecord ioc = getIoCForWhitelist(null, "whalebone.io", "whalebone", true);
        IoCRecord iocToBeWhite1 = getIoCForWhitelist(null, "whalebone.io", "someFeed", true);
        IoCRecord iocToBeWhite2 = getIoCForWhitelist(null, "greate.whaelobne.io", "someOtherFeed", true);
        ArrayList<IoCRecord> toBeWhiteListed = new ArrayList<>();
        toBeWhiteListed.add(iocToBeWhite1);
        toBeWhiteListed.add(iocToBeWhite2);

        when(whitelistCacheService.get("whalebone.io")).thenReturn(white);
        when(archiveService.findIoCsForWhitelisting("whalebone.io")).thenReturn(toBeWhiteListed);
        when(blacklistCacheService.removeWholeObjectFromCache(any(IoCRecord.class))).thenReturn(true);
        when(whitelistCacheService.setCompleted(white)).thenReturn(white);

        assertTrue(coreService.processWhitelistIoCRecord(ioc));

        verify(whitelistCacheService).get("whalebone.io");
        verify(archiveService).findIoCsForWhitelisting("whalebone.io");
        verify(archiveService).setRecordWhitelisted(iocToBeWhite1, "whalebone");
        verify(archiveService).setRecordWhitelisted(iocToBeWhite2, "whalebone");
        verify(blacklistCacheService).removeWholeObjectFromCache(iocToBeWhite1);
        verify(blacklistCacheService).removeWholeObjectFromCache(iocToBeWhite2);
        verify(whitelistCacheService).setCompleted(white);
        verifyNoMoreInteractions(whitelistCacheService);
        verifyNoMoreInteractions(blacklistCacheService);
        verifyNoMoreInteractions(archiveService);
    }

    @Test
    public void processExistingCompletedUpdateTest() throws Exception {
        coreService.setWhitelistValidSeconds(1 * 60 * 60); // 1 hour
        Calendar expiresAt = Calendar.getInstance();
        expiresAt.add(Calendar.MINUTE, 30);
        WhitelistedRecord white = createWhite("whalebone.io", "whalebone", expiresAt, true);
        Calendar expiresAt2 = Calendar.getInstance();
        expiresAt2.add(Calendar.HOUR, 1);
        WhitelistedRecord white2 = createWhite("whalebone.io", "whalebone2", expiresAt2, true);
        IoCRecord ioc = getIoCForWhitelist(null, "whalebone.io", "whalebone2", true);
        when(whitelistCacheService.get("whalebone.io")).thenReturn(white);
        when(whitelistCacheService.put(any(IoCRecord.class), eq(true))).thenReturn(white2);

        assertTrue(coreService.processWhitelistIoCRecord(ioc));

        verify(whitelistCacheService).get("whalebone.io");
        verify(whitelistCacheService).put(ioc, true);
        verifyNoMoreInteractions(whitelistCacheService);
        verifyZeroInteractions(blacklistCacheService);
        verifyZeroInteractions(archiveService);
    }

    @Test
    public void processExistingNotCompletedUpdateTest() throws Exception {
        coreService.setWhitelistValidSeconds(1 * 60 * 60); // 1 hour
        Calendar expiresAt = Calendar.getInstance();
        expiresAt.add(Calendar.MINUTE, 30);
        WhitelistedRecord white = createWhite("whalebone.io", "whalebone", expiresAt, false);
        Calendar expiresAt2 = Calendar.getInstance();
        expiresAt2.add(Calendar.HOUR, 1);
        WhitelistedRecord white2 = createWhite("whalebone.io", "whalebone2", expiresAt2, true);
        IoCRecord ioc = getIoCForWhitelist(null, "whalebone.io", "whalebone2", true);
        IoCRecord iocToBeWhite1 = getIoCForWhitelist(null, "whalebone.io", "someFeed", true);
        IoCRecord iocToBeWhite2 = getIoCForWhitelist(null, "greate.whaelobne.io", "someOtherFeed", true);
        ArrayList<IoCRecord> toBeWhiteListed = new ArrayList<>();
        toBeWhiteListed.add(iocToBeWhite1);
        toBeWhiteListed.add(iocToBeWhite2);

        when(whitelistCacheService.get("whalebone.io")).thenReturn(white);
        when(whitelistCacheService.put(any(IoCRecord.class), eq(false))).thenReturn(white2);
        when(archiveService.findIoCsForWhitelisting("whalebone.io")).thenReturn(toBeWhiteListed);
        when(blacklistCacheService.removeWholeObjectFromCache(any(IoCRecord.class))).thenReturn(true);
        when(whitelistCacheService.setCompleted(white2)).thenReturn(white2);

        assertTrue(coreService.processWhitelistIoCRecord(ioc));

        verify(whitelistCacheService).get("whalebone.io");
        verify(whitelistCacheService).put(ioc, false);
        verify(archiveService).findIoCsForWhitelisting("whalebone.io");
        verify(archiveService).setRecordWhitelisted(iocToBeWhite1, "whalebone2");
        verify(archiveService).setRecordWhitelisted(iocToBeWhite2, "whalebone2");
        verify(blacklistCacheService).removeWholeObjectFromCache(iocToBeWhite1);
        verify(blacklistCacheService).removeWholeObjectFromCache(iocToBeWhite2);
        verify(whitelistCacheService).setCompleted(white2);
        verifyNoMoreInteractions(whitelistCacheService);
        verifyNoMoreInteractions(blacklistCacheService);
        verifyNoMoreInteractions(archiveService);
    }

    @Test
    public void processNotExistingTest() throws Exception {
        coreService.setWhitelistValidSeconds(1 * 60 * 60); // 1 hour
        Calendar expiresAt = Calendar.getInstance();
        expiresAt.add(Calendar.HOUR, 1);
        WhitelistedRecord white = createWhite("whalebone.io", "whalebone", expiresAt, false);
        IoCRecord ioc = getIoCForWhitelist(null, "whalebone.io", "whalebone", true);
        IoCRecord iocToBeWhite1 = getIoCForWhitelist(null, "whalebone.io", "someFeed", true);
        IoCRecord iocToBeWhite2 = getIoCForWhitelist(null, "greate.whaelobne.io", "someOtherFeed", true);
        ArrayList<IoCRecord> toBeWhiteListed = new ArrayList<>();
        toBeWhiteListed.add(iocToBeWhite1);
        toBeWhiteListed.add(iocToBeWhite2);

        when(whitelistCacheService.get("whalebone.io")).thenReturn(null);
        when(whitelistCacheService.put(any(IoCRecord.class), eq(false))).thenReturn(white);
        when(archiveService.findIoCsForWhitelisting("whalebone.io")).thenReturn(toBeWhiteListed);
        when(blacklistCacheService.removeWholeObjectFromCache(any(IoCRecord.class))).thenReturn(true);
        when(whitelistCacheService.setCompleted(white)).thenReturn(white);

        assertTrue(coreService.processWhitelistIoCRecord(ioc));

        verify(whitelistCacheService).get("whalebone.io");
        verify(whitelistCacheService).put(ioc, false);
        verify(archiveService).findIoCsForWhitelisting("whalebone.io");
        verify(archiveService).setRecordWhitelisted(iocToBeWhite1, "whalebone");
        verify(archiveService).setRecordWhitelisted(iocToBeWhite2, "whalebone");
        verify(blacklistCacheService).removeWholeObjectFromCache(iocToBeWhite1);
        verify(blacklistCacheService).removeWholeObjectFromCache(iocToBeWhite2);
        verify(whitelistCacheService).setCompleted(white);
        verifyNoMoreInteractions(whitelistCacheService);
        verifyNoMoreInteractions(blacklistCacheService);
        verifyNoMoreInteractions(archiveService);
    }

    private WhitelistedRecord createWhite(String rawId, String sourceName, Calendar expiresAt, boolean completed) {
        WhitelistedRecord white = new WhitelistedRecord();
        white.setCompleted(completed);
        white.setRawId(rawId);
        white.setExpiresAt(expiresAt);
        white.setSourceName(sourceName);
        return white;
    }

    public IoCRecord getIoCForWhitelist(String ip, String fqdn, String sourceName, boolean withId) {
        IoCRecord ioc = new IoCRecord();
        ioc.setSource(new IoCSource());
        ioc.getSource().setIp(ip);
        ioc.getSource().setFqdn(fqdn);
        ioc.setFeed(new IoCFeed());
        ioc.getFeed().setName(sourceName);
        if (withId) {
            ioc.getSource().setId(new IoCSourceId());
            if (fqdn != null) {
                ioc.getSource().getId().setValue(fqdn);
                ioc.getSource().getId().setType(IoCSourceIdType.FQDN);
            } else if (ip != null) {
                ioc.getSource().getId().setValue(ip);
                ioc.getSource().getId().setType(IoCSourceIdType.IP);
            }
        }
        return ioc;
    }

    /**
     * creates IoCRecord observed ageHours in the past
     * @param ip
     * @param fqdn
     * @param sourceName feed name
     * @param ageHours sets time.observation to current
     * @return
     */
    public final IoCRecord getIoCRecord(String ip, String fqdn, String sourceName, int ageHours, String classificationType) {
        final IoCRecord ioc = new IoCRecord();

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR,-ageHours);
        Date observation = cal.getTime();
        IoCTime time = new IoCTime();
        ioc.setTime(time);
        ioc.getTime().setObservation(observation);

        IoCClassification classification = new IoCClassification();
        classification.setType(classificationType);
        ioc.setClassification(classification);

        ioc.setSource(new IoCSource());
        ioc.getSource().setIp(ip);
        ioc.getSource().setFqdn(fqdn);

        ioc.setFeed(new IoCFeed());
        ioc.getFeed().setName(sourceName);

        return ioc;
    }
}
