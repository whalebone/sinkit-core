package biz.karms.sinkit.ejb.impl;

import biz.karms.sinkit.ejb.ArchiveService;
import biz.karms.sinkit.ejb.BlacklistCacheService;
import biz.karms.sinkit.ejb.WhitelistCacheService;
import biz.karms.sinkit.ejb.cache.pojo.WhitelistedRecord;
import biz.karms.sinkit.ioc.IoCFeed;
import biz.karms.sinkit.ioc.IoCRecord;
import biz.karms.sinkit.ioc.IoCSource;
import biz.karms.sinkit.ioc.IoCSourceId;
import biz.karms.sinkit.ioc.IoCSourceIdType;

import java.util.ArrayList;
import java.util.Calendar;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.junit.Assert.assertEquals;
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
    private WhitelistCacheService whitelistCacheService;

    @Mock
    private ArchiveService archiveService;

    @Mock
    private BlacklistCacheService blacklistCacheService;

    @InjectMocks
    private CoreServiceEJB coreService;

    /**
     * Tests the update of accuracy with a report from AccuChecker
     * @throws Exception
     */
    @Test
    public void updateWithAccuCheckerReportTest() throws Exception {

        //prepare
        IoCRecord ioc1 = getIoCForWhitelist(null, "mal.com", "OneFeed", true);
        ioc1.setDocumentId("1");
        //give this ioc a feed accuracy
        HashMap<String,Integer> feed_accuracy_1 = new HashMap<>();
        feed_accuracy_1.put("feed", 80);
        ioc1.setAccuracy(feed_accuracy_1);

        //2nd ioc, feed accuracy setup
        IoCRecord ioc2 = getIoCForWhitelist(null, "mal.com", "SomeOtherFeed", true);
        ioc2.setDocumentId("2");
        HashMap<String,Integer> feed_accuracy_2 = new HashMap<>();
        feed_accuracy_2.put("feed", 50);
        ioc2.setAccuracy(feed_accuracy_2);

        //accuchecker report setup
        IoCRecord report = getIoCForWhitelist(null, "mal.com",null,true);
        HashMap<String,Integer> accuracy = new HashMap<>();
        accuracy.put("SomeAccuracyProvider", 20);
        report.setAccuracy(accuracy);
        HashMap<String,String> metadata = new HashMap<>();
        metadata.put("SomeAccuracyProvider","SomeAccuracyProvider has no metadata");
        report.setMetadata(metadata);
        IoCAccuCheckerReport accu_report = new IoCAccuCheckerReport(report);
        List<IoCRecord> iocs = new ArrayList<IoCRecord>();
        iocs.add(ioc1);
        iocs.add(ioc2);
        when( archiveService.getMatchingEntries("source.id.value", "mal.com")).thenReturn(iocs);
        when(archiveService.setReportToIoCRecord(accu_report,"1")).thenReturn(true);
        when(archiveService.setReportToIoCRecord(accu_report,"2")).thenReturn(true);
        when(blacklistCacheService.addToCache(iocs.get(0))).thenReturn(true);
        when(blacklistCacheService.addToCache(iocs.get(1))).thenReturn(true);

        //call tested method
        assertTrue(coreService.updateWithAccuCheckerReport(accu_report));

        //verify
        verify(archiveService).getMatchingEntries("source.id.value","mal.com");
        verify(archiveService).setReportToIoCRecord(accu_report,"1");
        verify(archiveService).setReportToIoCRecord(accu_report,"2");
        verify(blacklistCacheService).addToCache(iocs.get(0));
        verify(blacklistCacheService).addToCache(iocs.get(1));
        assertEquals(new Integer(80),iocs.get(0).getAccuracy().get("feed"));
        assertEquals(new Integer(20),iocs.get(0).getAccuracy().get("SomeAccuracyProvider"));
        assertEquals(new Integer(50),iocs.get(1).getAccuracy().get("feed"));
        assertEquals(new Integer(20),iocs.get(1).getAccuracy().get("SomeAccuracyProvider"));
        verifyNoMoreInteractions(archiveService);
        verifyNoMoreInteractions(blacklistCacheService);
    }

    @Test
    public void processExistingCompletedNoUpdateTest() throws Exception {
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

    @Test
    public void test() {
        System.out.println(System.getProperty("java.io.tmpdir"));
        System.out.println(Math.ceil((1001 - 1000) / 1000.00));
    }
}
