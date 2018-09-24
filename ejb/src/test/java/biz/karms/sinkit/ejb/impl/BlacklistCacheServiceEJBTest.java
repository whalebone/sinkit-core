package biz.karms.sinkit.ejb.impl;

import biz.karms.sinkit.ejb.cache.pojo.BlacklistedRecord;

import biz.karms.sinkit.ioc.IoCRecord;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.eq;

/**
 * @author Krystof Kolar
 */
@RunWith(MockitoJUnitRunner.class)
public class BlacklistCacheServiceEJBTest {

    IoCRecord ioc1;
    IoCRecord ioc2;
    String key;
    GsonBuilder gsonBuilder;
    InputStream blacklistedRecord1InputStream;
    InputStream blacklistedRecord2InputStream;

    @Mock
    private Logger log;

    @Mock
     RemoteCache<String, BlacklistedRecord> blacklistCache;

    @InjectMocks
    private BlacklistCacheServiceEJB blacklistCacheService;

    @Before
    public void setUp () throws Exception{
        //ioc1, ioc2 and their corresponding key (both have the same)
        //will be used throughout
        final InputStream ioc1InputStream = BlacklistCacheServiceEJBTest.class.getClassLoader()
                .getResourceAsStream("ioc1.json");
       ioc1 = new GsonBuilder().setDateFormat(IoCRecord.DATE_FORMAT)
                .create().fromJson(new InputStreamReader(ioc1InputStream),
                        IoCRecord.class);
        key = DigestUtils.md5Hex(ioc1.getSource().getId().getValue());

        final InputStream ioc2InputStream = BlacklistCacheServiceEJBTest.class.getClassLoader()
                .getResourceAsStream("ioc2.json");
        ioc2 = new GsonBuilder().setDateFormat(IoCRecord.DATE_FORMAT)
                .create().fromJson(new InputStreamReader(ioc2InputStream),
                        IoCRecord.class);

        //use streams here, so that blacklistedRecord1 and blacklistedRecord2 get properly loaded in each test
        blacklistedRecord1InputStream = BlacklistCacheServiceEJBTest.class.getClassLoader()
                .getResourceAsStream("blacklisted_record1.json");
        blacklistedRecord2InputStream = BlacklistCacheServiceEJBTest.class.getClassLoader()
                .getResourceAsStream("blacklisted_record2.json");

        //needed to read BlacklistedRecord jsons
        JsonDeserializer<Calendar> calendarJsonDeserializer = (json, typeOfT, context) -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Calendar c = Calendar.getInstance();
            try {
                Date date = sdf.parse(json.getAsJsonPrimitive().getAsString());
                c.setTime(date);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return c;
        };
        JsonDeserializer<Pair> pairJsonDeserializer = (json, typeOfT, context) -> {
            Set<Map.Entry<String, JsonElement>> entrySet = json.getAsJsonObject().entrySet();
            List<Pair<String, String>> list = entrySet.stream().map(entry -> new ImmutablePair<>(entry.getKey(), entry.getValue().getAsString())).collect(
                    Collectors.toList());

            return !list.isEmpty() ? list.get(0) : null;
        };

        gsonBuilder = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.IDENTITY).setDateFormat("yyyy-MM-dd")
                .registerTypeAdapter(Calendar.class, calendarJsonDeserializer)
                .registerTypeAdapter(ImmutablePair.class, pairJsonDeserializer);

    }

    /**
     * Tests removal of a whole cache entry (even when it contains multiple feeds)
     * @throws Exception
     */
    @Test
    public void removeWholeObjectFromCachePresentTest() throws Exception {
        //prepare
        when(blacklistCache.containsKey(key)).thenReturn(true);

        //call tested method
        assertTrue(blacklistCacheService.removeWholeObjectFromCache(ioc1));

        //verify
        verify(blacklistCache).containsKey(key);
        verify(blacklistCache).remove(key);
        verifyNoMoreInteractions(blacklistCache);
    }

    /**
     * Tests method removeWholeObjectFromCache on the case when the object is not in the cache.
     * There should be no other interactions with the cache than checking whether the object is present
     * @throws Exception
     */
    @Test
    public void removeWholeObjectFromCacheAbsentTest() throws Exception {
        //prepare
        when(blacklistCache.containsKey(key)).thenReturn(false);

        //call tested method
        assertTrue(blacklistCacheService.removeWholeObjectFromCache(ioc1));

        //verify
        verify(blacklistCache).containsKey(key);
        verifyNoMoreInteractions(blacklistCache);
    }

    /**
     *This tests the case when there is 1 feed in the cached entry. Removing it means removing the whole entry
     * @throws Exception
     */
    @Test
    public void removeFromCache1FeedTest() throws Exception{
        //prepare
        final BlacklistedRecord blacklistedRecord1 = gsonBuilder
                .create()
                .fromJson(new InputStreamReader(blacklistedRecord1InputStream), BlacklistedRecord.class);
        when(blacklistCache.containsKey(key)).thenReturn(true);
        when(blacklistCache.withFlags(Flag.SKIP_CACHE_LOAD)).thenReturn(blacklistCache);
        when(blacklistCache.get(key)).thenReturn(blacklistedRecord1);

        //call
        assertTrue(blacklistCacheService.removeFromCache(ioc1));

        //verify
        verify(blacklistCache).containsKey(key);
        verify(blacklistCache).withFlags(Flag.SKIP_CACHE_LOAD);
        verify(blacklistCache).get(key);
        //the whalebone.io entry will get removed from cache, because it only containes one feed, feed1
        verify(blacklistCache).remove(key);
        verifyNoMoreInteractions(blacklistCache);
    }

   /**
    *This tests the case when there are 2 feeds in the cached record, and one of them gets removed
    * @throws Exception
    */
    @Test
    public void removeFromCache2FeedsTest() throws Exception{
        //prepare
        final BlacklistedRecord blacklistedRecord2= gsonBuilder
                .create()
                .fromJson(new InputStreamReader(blacklistedRecord2InputStream), BlacklistedRecord.class);
        when(blacklistCache.containsKey(key)).thenReturn(true);
        when(blacklistCache.withFlags(Flag.SKIP_CACHE_LOAD)).thenReturn(blacklistCache);
        when(blacklistCache.get(key)).thenReturn(blacklistedRecord2);

        //call, ioc1 has feed1, only feed2 should remain in blacklistedRecord
        assertTrue(blacklistCacheService.removeFromCache(ioc1));

        //verify
        verify(blacklistCache).containsKey(key);
        verify(blacklistCache).withFlags(Flag.SKIP_CACHE_LOAD);
        verify(blacklistCache).get(key);
        verify(blacklistCache).replace(key,blacklistedRecord2);
        //one feed gets removed, one remains
        assertEquals(Collections.singleton("feed2"),
                blacklistedRecord2.getAccuracy().keySet());
        assertEquals(Collections.singleton("feed2"),
                blacklistedRecord2.getSources().keySet());
        //this shouldn't change
        assertEquals(key,blacklistedRecord2.getBlackListedDomainOrIP());
        verifyNoMoreInteractions(blacklistCache);
    }

    /**
    *ioc2 from feed2 gets added to the entry that is based on the ioc from feed1 (blacklistedRecord1)
     * @throws Exception
     */
    @Test
    public void addToCacheContainsKeyTest() throws Exception {
        //prepare
        final BlacklistedRecord blacklistedRecord1 = gsonBuilder
                .create()
                .fromJson(new InputStreamReader(blacklistedRecord1InputStream), BlacklistedRecord.class);
        when(blacklistCache.containsKey(key)).thenReturn(true);
        when(blacklistCache.withFlags(Flag.SKIP_CACHE_LOAD)).thenReturn(blacklistCache);
        when(blacklistCache.get(key)).thenReturn(blacklistedRecord1);

        //call
        assertTrue(blacklistCacheService.addToCache(ioc2));

        //verify
        verify(blacklistCache).containsKey(key);
        verify(blacklistCache).withFlags(Flag.SKIP_CACHE_LOAD);
        verify(blacklistCache).get(key);
        verify(blacklistCache).replace(key, blacklistedRecord1);
        //one feed gets added, we have 2 feed entries in total
        assertEquals(new HashSet<>(Arrays.asList("feed1", "feed2")),
                blacklistedRecord1.getAccuracy().keySet() );
        assertEquals(new HashSet<>(Arrays.asList("feed1", "feed2")),
                blacklistedRecord1.getSources().keySet() );
        assertEquals(key,blacklistedRecord1.getBlackListedDomainOrIP());
        verifyNoMoreInteractions(blacklistCache);
    }

    /**
    *ioc1 gets added to empty cache
     * @throws Exception
     */
    @Test
    public void addToCacheNoKeyTest() throws Exception {
        //prepare
        ArgumentCaptor<BlacklistedRecord> argumentCaptor = ArgumentCaptor.forClass(BlacklistedRecord.class);
        when(blacklistCache.containsKey(key)).thenReturn(false);

        //call
        assertTrue(blacklistCacheService.addToCache(ioc1));

        //verify that something gets inserted
        verify(blacklistCache).containsKey(key);
        verify(blacklistCache).put(eq(key) , argumentCaptor.capture());
        //verify that what gets inserted is the correct thing
        assertEquals(key, argumentCaptor.getValue().getBlackListedDomainOrIP());
        assertEquals(ioc1.getAccuracy(),
                argumentCaptor.getValue().getAccuracy().get("feed1"));
        assertEquals(ioc1.getDocumentId(),
                argumentCaptor.getValue().getSources().get("feed1").getValue());
        assertEquals(ioc1.getClassification().getType(),
                argumentCaptor.getValue().getSources().get("feed1").getKey());
        verifyNoMoreInteractions(blacklistCache);
    }

    /**
    *ioc1 gets added to cache when its corresponding blacklist entry is already there but with a lower feed accuracy (blacklisted_record1 has feed1.accuracy set to 10)
     *  Accuracy will get updated with the new value(here 34)
     * @throws Exception
     */
    @Test
    public void addToCacheAccuracyUpdateTest() throws Exception {
        //prepare
        final BlacklistedRecord blacklistedRecord1 = gsonBuilder.create()
                .fromJson(new InputStreamReader(blacklistedRecord1InputStream), BlacklistedRecord.class);
        blacklistedRecord1.getAccuracy().get("feed1").put("feed",10);
        assertEquals(new Integer(10), blacklistedRecord1.getAccuracy().get("feed1").get("feed"));

        when(blacklistCache.containsKey(key)).thenReturn(true);
        when(blacklistCache.withFlags(Flag.SKIP_CACHE_LOAD)).thenReturn(blacklistCache);
        when(blacklistCache.get(key)).thenReturn(blacklistedRecord1);

        //call
        assertTrue(blacklistCacheService.addToCache(ioc1));

        //verify
        verify(blacklistCache).containsKey(key);
        verify(blacklistCache).withFlags(Flag.SKIP_CACHE_LOAD);
        verify(blacklistCache).get(key);
        verify(blacklistCache).replace(key, blacklistedRecord1);
        //accuracy gets updated to 34 (ioc1's accuracy)
        assertEquals(new Integer(34), blacklistedRecord1.getAccuracy().get("feed1").get("feed"));
        assertEquals(key,blacklistedRecord1.getBlackListedDomainOrIP());
        verifyNoMoreInteractions(blacklistCache);
    }
}
