package biz.karms.sinkit.ejb.impl;

import biz.karms.sinkit.ejb.GSBService;
import biz.karms.sinkit.ejb.cache.annotations.SinkitCache;
import biz.karms.sinkit.ejb.cache.annotations.SinkitCacheName;
import biz.karms.sinkit.ejb.cache.pojo.GSBRecord;
import biz.karms.sinkit.ejb.gsb.GSBClient;
import biz.karms.sinkit.ejb.gsb.dto.FullHashLookupResponse;
import biz.karms.sinkit.ejb.gsb.util.GSBCachePOJOFactory;
import biz.karms.sinkit.ejb.gsb.util.GSBUtils;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.infinispan.Cache;
import org.infinispan.commons.util.concurrent.NotifyingFuture;
import org.infinispan.context.Flag;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by tom on 11/27/15.
 *
 * @author Tomas Kozel
 */
@Stateless
public class GSBServiceEJB implements GSBService {

    private static final int PREFIX_LENGTH = 4;

    @Inject
    private Logger logger;

    @Inject
    @SinkitCache(SinkitCacheName.GSB_CACHE)
    private Cache<String, GSBRecord> gsbCache;

    @EJB
    private GSBClient gsbClient;

    @PostConstruct
    public void setup() {
        if (gsbClient == null || gsbCache == null) {
            throw new IllegalStateException("GSB cache and client must not be null.");
        }
    }

    @Override
    public Set<String> lookup(final String ipOrFQDN) {
        if (ipOrFQDN == null) {
            throw new IllegalArgumentException("lookup: URL must not be null, cannot perform lookup.");
        }
        // canonicalization and prefix/suffix variants for lookup is done here
        final List<String> lookupVariants = GSBUtils.getLookupVariants(ipOrFQDN);
        // try to lookup for each variant until first match is found
        Set<String> gsbBlacklists;
        for (String lookupVariant : lookupVariants) {
            gsbBlacklists = lookupSingleVariant(lookupVariant);
            if (gsbBlacklists != null && !gsbBlacklists.isEmpty()) {
                return gsbBlacklists;
            }
        }
        return null;
    }

    private Set<String> lookupSingleVariant(final String lookupVariant) {
        final byte[] hash = DigestUtils.sha256(lookupVariant);
        final byte[] hashPrefix = ArrayUtils.subarray(hash, 0, PREFIX_LENGTH);
        final String fullHashString = Hex.encodeHexString(hash);
        final String hashStringPrefix = fullHashString.substring(0, PREFIX_LENGTH * 2);

        GSBRecord gsbRecord = gsbCache.get(hashStringPrefix);
        // if hash prefix is not in the cache then URL is not blacklisted for sure
        if (gsbRecord == null) {
            return null;
        } else {
            logger.log(Level.INFO, "lookup: hashPrefix " + hashStringPrefix + " was found in cache. It was made off: " + fullHashString + " which is lookupVariant: " + lookupVariant);
        }

        final HashMap<String, HashSet<String>> fullHashes;
        if (Calendar.getInstance().before(gsbRecord.getFullHashesExpireAt())) {
            logger.log(Level.FINE, "lookup: Full hashes for prefix " + hashStringPrefix + " are valid.");
            fullHashes = gsbRecord.getFullHashes();
        } else {
            logger.log(Level.FINE, "lookup: Full hashes for prefix " + hashStringPrefix + " expired -> updating.");
            FullHashLookupResponse resposne = gsbClient.getFullHashes(hashPrefix);
            gsbRecord = GSBCachePOJOFactory.createFullHashes(resposne);
            gsbCache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES).putAsync(hashStringPrefix, gsbRecord);
            fullHashes = gsbRecord.getFullHashes();
        }

        // if fullHashes are empty then return null, i.e. no matched blacklists
        if (MapUtils.isEmpty(fullHashes)) {
            logger.log(Level.FINE, "lookup: Valid full hashes for prefix " + hashStringPrefix + " are empty.");
            return null;
        } else {
            final Set<String> matchedBlacklists = new HashSet<>();
            for (String blacklist : fullHashes.keySet()) {
                final Set<String> fullHashesOnBlacklist = fullHashes.get(blacklist);
                if (fullHashesOnBlacklist != null && fullHashesOnBlacklist.contains(fullHashString)) {
                    logger.log(Level.FINEST, "lookup: got hit for hash prefix " + hashStringPrefix + " and full hash " + fullHashString + ": " + blacklist);
                    matchedBlacklists.add(blacklist);
                }
            }
            return matchedBlacklists;
        }
    }

    @Override
    public boolean putHashPrefix(final String hashPrefix) {
        if (hashPrefix == null) {
            logger.log(Level.SEVERE, "putHashPrefix: Got null hash prefix. Can't process this.");
            return false;
        }

        if (!gsbCache.containsKey(hashPrefix)) {
            final Calendar fullHashesExpireAt = Calendar.getInstance();
            // set to past to enforce update full hashes when hit by lookup for the first time
            fullHashesExpireAt.add(Calendar.SECOND, -1);
            HashMap<String, HashSet<String>> fullHashes = new HashMap<>();

            final GSBRecord gsbRecord = new GSBRecord(hashPrefix, fullHashesExpireAt, fullHashes);
            gsbCache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES).putAsync(hashPrefix, gsbRecord);
            logger.log(Level.FINEST, "putHashPrefix: Hash prefix " + hashPrefix + " added into cache.");
        } else {
            //Should not have happened often
            logger.log(Level.FINE, "putHashPrefix: Hash prefix " + hashPrefix + " already contained in cache.");
        }

        return true;
    }

    @Override
    public boolean removeHashPrefix(final String hashPrefix) {
        if (hashPrefix == null) {
            logger.log(Level.SEVERE, "removeHashPrefix: Got null hash prefix. Can't process this.");
            return false;
        }
        gsbCache.removeAsync(hashPrefix);
        return true;
    }


//    @Override
//    public boolean putFullHashes(String hashPrefix, int validSeconds, HashMap<String, HashSet<String>> fullHashes) {
//        if (hashPrefix == null || fullHashes == null) {
//            logger.log(Level.SEVERE, "putFullHashes: Hash prefix nor full hashes container cannot be null.");
//            return false;
//        }
//
//        if (!gsbCache.containsKey(hashPrefix)) {
//            logger.log(Level.SEVERE, "putFullHashes: Hash prefix " + hashPrefix + " is not contained in cache. Must not proceed.");
//            return false;
//        }
//        Calendar fullHashesExpireAt = Calendar.getInstance();
//        fullHashesExpireAt.add(Calendar.SECOND, validSeconds);
//        GSBRecord gsbRecord = new GSBRecord(hashPrefix, fullHashesExpireAt, fullHashes);
//        gsbCache.replaceAsync(hashPrefix, gsbRecord);
//        return true;
//    }

    @Override
    public boolean dropTheWholeCache(boolean async) {
        try {
            NotifyingFuture<Void> cleared = gsbCache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES).clearAsync();
            if (!async) {
                cleared.get();
            }
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.log(Level.SEVERE, "dropTheWholeCache: Clearing cache went wrong.");
            return false;
        }
    }

    @Override
    public int getStats() {
        logger.log(Level.SEVERE, "Dangerous call to .size(), could OOM.");
        return gsbCache.size();
    }
}
