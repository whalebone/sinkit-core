package biz.karms.sinkit.ejb.impl;

import biz.karms.sinkit.ejb.WhitelistCacheService;
import biz.karms.sinkit.ejb.cache.annotations.SinkitCache;
import biz.karms.sinkit.ejb.cache.annotations.SinkitCacheName;
import biz.karms.sinkit.ejb.cache.pojo.WhitelistedRecord;
import biz.karms.sinkit.ejb.util.WhitelistUtils;
import biz.karms.sinkit.ioc.IoCRecord;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.infinispan.Cache;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Tomas Kozel
 */
@Stateless
public class WhitelistCacheServiceEJB implements WhitelistCacheService {

    @Inject
    private Logger log;

    @Inject
    @SinkitCache(SinkitCacheName.WHITELIST_CACHE)
    private Cache<String, WhitelistedRecord> whitelistCache;

    @Override
    public WhitelistedRecord put(final IoCRecord iocRecord, final boolean completed) {
        if (!isIoCValidForPut(iocRecord)) {
            return null;
        }

        final WhitelistedRecord white = WhitelistUtils.createWhitelistedRecord(iocRecord, completed);
        final String key = DigestUtils.md5Hex(white.getRawId());
        log.log(Level.INFO, "Whitelist key: " + key + ", ttl in s: " + iocRecord.getSource().getTTL());
        try {
            if (!whitelistCache.containsKey(key)) {
                whitelistCache.putAsync(key, white, iocRecord.getSource().getTTL(), TimeUnit.SECONDS);
            } else {
                whitelistCache.replaceAsync(key, white, iocRecord.getSource().getTTL(), TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "put", e);
            return null;
        }
        return white;
    }

    @Override
    public WhitelistedRecord get(final String id) {
//        if(iocRecord == null || iocRecord.getSource() == null || iocRecord.getSource().getId() == null ||
//                StringUtils.isBlank(iocRecord.getSource().getId().getValue())) {
//            log.log(Level.SEVERE, "add: Cannot search whitelist. Object ioc or ioc.source.id.value is null or blank");
//            return null;
//        }
        if (id == null) {
            log.log(Level.SEVERE, "get: Cannot search whitelist. Id is null.");
            return null;
        }
        return whitelistCache.getOrDefault(DigestUtils.md5Hex(id), null);
    }

    @Override
    public boolean dropTheWholeCache() {
        log.log(Level.SEVERE, "dropTheWholeCache: We are dropping the cache. This has severe operational implications.");
        try {
            whitelistCache.clearAsync();
        } catch (Exception e) {
            log.log(Level.SEVERE, "dropTheWholeCache", e);
            return false;
        }
        return true;
    }

    @Override
    public WhitelistedRecord setCompleted(final WhitelistedRecord partialWhite) {
        if (partialWhite == null || partialWhite.getRawId() == null || StringUtils.isBlank(partialWhite.getSourceName()) || partialWhite.getExpiresAt() == null) {
            log.log(Level.SEVERE, "put: Cannot set whiltelist entry as completed - missing mandatory fields");
            return null;
        }
        final String key = DigestUtils.md5Hex(partialWhite.getRawId());
        if (!whitelistCache.containsKey(key)) {
            return null;
        }
        final long ttl = (partialWhite.getExpiresAt().getTimeInMillis() - Calendar.getInstance().getTimeInMillis());
        if (ttl < 0) {
            return null;
        }
        partialWhite.setCompleted(true);
        //TODO: Do we need it to be acurate? That woudl require changes in MyCacheManagerProvider
        whitelistCache.replaceAsync(key, partialWhite, ttl, TimeUnit.MILLISECONDS);
        return whitelistCache.get(key);
    }

    @Override
    public boolean remove(final String id) {
        log.log(Level.INFO, "remove: removing whitelist entry manually, key: " + id);
        final String key = DigestUtils.md5Hex(id);
        if (!whitelistCache.containsKey(key)) {
            log.log(Level.INFO, "remove: entry not found, key: " + id);
            return false;
        }
        whitelistCache.removeAsync(key);
        return true;
    }

    @Override
    public boolean isWhitelistEmpty() {
        return whitelistCache.isEmpty();
    }

    private boolean isIoCValidForPut(final IoCRecord iocRecord) {
        if (iocRecord == null || iocRecord.getSource() == null || iocRecord.getSource().getId() == null || StringUtils.isBlank(iocRecord.getSource().getId().getValue())) {
            log.log(Level.SEVERE, "put: Cannot put entry to whitelist - ioc or ioc.source.id.value is null or blank");
            return false;
        }
        if (iocRecord.getSource().getTTL() == null) {
            log.log(Level.SEVERE, "put: Cannot put entry to whitelist - ioc.source.ttl is null");
            return false;
        }
        if (iocRecord.getFeed() == null || StringUtils.isBlank(iocRecord.getFeed().getName())) {
            log.log(Level.SEVERE, "put: Cannot put entry to whitelist - ioc.feed.name is null or blank");
            return false;
        }
        return true;
    }
}
