package biz.karms.sinkit.ejb.impl;

import biz.karms.sinkit.ejb.GSBService;
import biz.karms.sinkit.ejb.cache.annotations.SinkitCache;
import biz.karms.sinkit.ejb.cache.annotations.SinkitCacheName;
import biz.karms.sinkit.ejb.cache.pojo.GSBRecord;
import biz.karms.sinkit.ejb.gsb.GSBClient;
import biz.karms.sinkit.ejb.gsb.dto.FullHashLookupResponse;
import biz.karms.sinkit.ejb.gsb.util.GSBCachePOJOFactory;
import biz.karms.sinkit.ejb.util.GSBUrl;
import org.infinispan.Cache;
import org.infinispan.commons.util.concurrent.NotifyingFuture;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by tom on 11/27/15.
 *
 * @author Tomas Kozel
 */
@Stateless
public class GSBServiceEJB implements GSBService {

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
    public Set<String> lookup(String url) {
        if (url == null) {
            throw new IllegalArgumentException("lookup: URL must not be null, cannot perform lookup.");
        }
        GSBUrl gsbUrl = new GSBUrl(url); //canonicalization is done here

        String fullHashString = gsbUrl.getHashString(); // TODO
        String hashStringPrefix = gsbUrl.getHashStringPrefix(4); // TODO
        byte[] hashPrefix = gsbUrl.getHashPrefix(4);
        logger.log(Level.INFO, "lookup: lookup for hashPrefix " + hashStringPrefix);
        Set<String> matchedBlacklists = new HashSet<>();

        GSBRecord gsbRecord = gsbCache.get(hashStringPrefix);
        // if hash prefix is not in cash then URL is not blacklisted for sure
        if (gsbRecord == null) {
            logger.log(Level.INFO, "lookup: hashPrefix " + hashStringPrefix + " was not found in cache.");
            return matchedBlacklists; //return empty list
        }
        HashMap<String, HashSet<String>> fullHashes;
        if (Calendar.getInstance().before(gsbRecord.getFullHashesExpireAt())) {
            logger.log(Level.INFO, "lookup: Full hashes for prefix " + hashStringPrefix + " are valid.");
            fullHashes = gsbRecord.getFullHashes();
        } else {
            logger.log(Level.INFO, "lookup: Full hashes for prefix " + hashStringPrefix + " expired -> updating.");
            FullHashLookupResponse resposne = gsbClient.getFullHashes(hashPrefix);
            gsbRecord = GSBCachePOJOFactory.createFullHashes(resposne);
            gsbCache.putAsync(hashStringPrefix, gsbRecord);
            fullHashes = gsbRecord.getFullHashes();
        }

        // if fullHashes are empty then return empty matched blacklists
        if (fullHashes.isEmpty()) {
            logger.log(Level.INFO, "lookup: Valid full hashes for prefix " + hashStringPrefix + " are empty.");
            return matchedBlacklists;
        } else {
            for (String blacklist : fullHashes.keySet()) {
                Set<String> fullHashesOnBlacklist = fullHashes.get(blacklist);
                if (fullHashesOnBlacklist != null && fullHashesOnBlacklist.contains(fullHashString)) {
                    logger.log(Level.FINEST, "lookup: got hit for hash prefix " + hashStringPrefix + " and full hash " + fullHashString + ": " + blacklist);
                    matchedBlacklists.add(blacklist);
                }
            }
        }
        return matchedBlacklists;
    }

    @Override
    public boolean putHashPrefix(String hashPrefix) {
        if (hashPrefix == null) {
            logger.log(Level.SEVERE, "putHashPrefix: Got null hash prefix. Can't process this.");
            return false;
        }

        if (!gsbCache.containsKey(hashPrefix))  {

            Calendar fullHashesExpireAt = Calendar.getInstance();
            // set to past to enforce update full hashes when hit by lookup for the first time
            fullHashesExpireAt.add(Calendar.SECOND, -1);
            HashMap<String, HashSet<String>> fullHashes =  new HashMap<>();

            GSBRecord gsbRecord = new GSBRecord(hashPrefix, fullHashesExpireAt, fullHashes);
            gsbCache.putAsync(hashPrefix, gsbRecord);
            logger.log(Level.FINEST, "putHashPrefix: Hash prefix " + hashPrefix + " added into cache.");
        } else {
            //Should not have happened often
            logger.log(Level.INFO, "putHashPrefix: Hash prefix " + hashPrefix + " already contained in cache.");
        }

        return true;
    }

    @Override
    public boolean removeHashPrefix(String hashPrefix) {
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
            NotifyingFuture<Void> cleared = gsbCache.clearAsync();
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
        return gsbCache.size();
    }
}
