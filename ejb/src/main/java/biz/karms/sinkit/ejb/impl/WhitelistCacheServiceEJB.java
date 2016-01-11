package biz.karms.sinkit.ejb.impl;

import biz.karms.sinkit.ejb.WhitelistCacheService;
import biz.karms.sinkit.ejb.cache.annotations.SinkitCache;
import biz.karms.sinkit.ejb.cache.annotations.SinkitCacheName;
import biz.karms.sinkit.ejb.cache.pojo.WhitelistedRecord;
import biz.karms.sinkit.ejb.util.WhitelistUtils;
import biz.karms.sinkit.ioc.IoCRecord;
import biz.karms.sinkit.ioc.IoCSourceIdType;
import org.apache.commons.lang3.StringUtils;
import org.infinispan.Cache;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by tkozel on 1/9/16.
 */
@Stateless
public class WhitelistCacheServiceEJB implements WhitelistCacheService {

    @Inject
    private Logger log;

    @Inject
    @SinkitCache(SinkitCacheName.WHITELIST_CACHE)
    private Cache<String, WhitelistedRecord> whitelistCache;

    @Override
    public boolean put(final IoCRecord iocRecord) {
        if (!isIoCValidForPut(iocRecord)) {
            return false;
        }

        WhitelistedRecord white = WhitelistUtils.createWhitelistedRecord(iocRecord);
        String key = WhitelistUtils.computeHashedId(white.getRawId());
        try {
            if (!whitelistCache.containsKey(key)) {
                whitelistCache.put(key, white, iocRecord.getSource().getTTL(), TimeUnit.SECONDS);
            } else {
                whitelistCache.replace(key, white, iocRecord.getSource().getTTL(), TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "put", e);
            return false;
        }
        return true;
    }

    @Override
    public WhitelistedRecord get(final IoCRecord iocRecord) {
        if(iocRecord == null || iocRecord.getSource() == null || iocRecord.getSource().getId() == null ||
                StringUtils.isBlank(iocRecord.getSource().getId().getValue())) {
            log.log(Level.SEVERE, "add: Cannot search whitelist. Object ioc or ioc.source.id.value is null or blank");
            return null;
        }

        String key;
        if (iocRecord.getSource().getId().getType() == IoCSourceIdType.FQDN) {
            key = WhitelistUtils.computeHashedId(WhitelistUtils.stripSubdomains(iocRecord.getSource().getId().getValue()));
        } else {
            key = WhitelistUtils.computeHashedId(iocRecord.getSource().getId().getValue());
        }
        if (!whitelistCache.containsKey(key)) {
            return null;
        }

        return whitelistCache.get(key);
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

    private boolean isIoCValidForPut(IoCRecord iocRecord) {
        if(iocRecord == null || iocRecord.getSource() == null || iocRecord.getSource().getId() == null ||
                StringUtils.isBlank(iocRecord.getSource().getId().getValue())) {
            log.log(Level.SEVERE, "put: Cannot put entry to whitelist - ioc or ioc.source.id.value is null or blank");
            return false;
        }
        if(iocRecord.getSource().getTTL() == null) {
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
