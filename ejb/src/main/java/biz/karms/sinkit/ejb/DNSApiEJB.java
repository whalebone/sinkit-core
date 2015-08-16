package biz.karms.sinkit.ejb;

import biz.karms.sinkit.ejb.cache.pojo.BlacklistedRecord;
import biz.karms.sinkit.ejb.cache.pojo.CustomList;
import biz.karms.sinkit.ejb.cache.pojo.Rule;
import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Michal Karm Babacek
 */
@Stateless
public class DNSApiEJB {

    @Inject
    private Logger log;

    @Inject
    private DefaultCacheManager m;

    private Cache<String, BlacklistedRecord> blacklistCache = null;

    private Cache<String, Rule> ruleCache = null;

    private Cache<String, CustomList> customListsCache = null;

    @PostConstruct
    public void setup() {
        blacklistCache = m.getCache("BLACKLIST_CACHE");
        ruleCache = m.getCache("RULES_CACHE");
        customListsCache = m.getCache("CUSTOM_LISTS_CACHE");
        if (blacklistCache == null || ruleCache == null || customListsCache == null) {
            throw new IllegalStateException("Both BLACKLIST_CACHE and RULES_CACHE and CUSTOM_LISTS_CACHE must not be null.");
        }
    }

    public BlacklistedRecord getSinkHole(final String client, final String key) {

        // Lookup BlacklistedRecord from IoC cache (gives feeds)

        // Lookup Rules (gives customerId, feeds and their settings)

        // Lookup White lists (lookup FQDN or CIDR)

        // Lookup Black lists (lookup FQDN or CIDR)

        // Log Kozel

        // Return sinkhole or null

        log.log(Level.FINE, "getting key [" + key + "]");
        return blacklistCache.get(key);
    }
}
