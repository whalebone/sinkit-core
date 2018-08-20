package biz.karms.sinkit.ejb.impl;

import biz.karms.sinkit.ejb.WhitelistCacheService;
import biz.karms.sinkit.ejb.cache.annotations.SinkitCache;
import biz.karms.sinkit.ejb.cache.annotations.SinkitCacheName;
import biz.karms.sinkit.ejb.cache.pojo.WhitelistedRecord;
import biz.karms.sinkit.ejb.util.WhitelistUtils;
import biz.karms.sinkit.ioc.IoCRecord;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;

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
    @SinkitCache(SinkitCacheName.infinispan_whitelist)
    private RemoteCache<String, WhitelistedRecord> whitelistCache;

    @Override
    public WhitelistedRecord put(final IoCRecord iocRecord, final boolean completed) {
        if (!isIoCValidForPut(iocRecord)) {
            return null;
        }

        final WhitelistedRecord white = WhitelistUtils.createWhitelistedRecord(iocRecord, completed);
        final String key = DigestUtils.md5Hex(white.getRawId());
        log.log(Level.FINE, "Whitelist key: " + key + ", ttl in s: " + iocRecord.getSource().getTtl());
        try {
            if (!whitelistCache.containsKey(key)) {
                whitelistCache.put(key, white, iocRecord.getSource().getTtl(), TimeUnit.SECONDS);
            } else {
                whitelistCache.replace(key, white, iocRecord.getSource().getTtl(), TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "put", e);
            return null;
        }
        return white;
    }

    @Override
    public WhitelistedRecord get(final String id) {
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
        whitelistCache.withFlags(Flag.SKIP_CACHE_LOAD).replace(key, partialWhite, ttl, TimeUnit.MILLISECONDS);
        return whitelistCache.withFlags(Flag.SKIP_CACHE_LOAD).get(key);
    }

    @Override
    public boolean remove(final String id) {
        log.log(Level.FINE, "remove: removing whitelist entry manually, key: " + id);
        final String key = DigestUtils.md5Hex(id);
        if (!whitelistCache.containsKey(key)) {
            log.log(Level.FINE, "remove: entry not found, key: " + id);
            return false;
        }
        whitelistCache.remove(key);
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
        if (iocRecord.getSource().getTtl() == null) {
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
