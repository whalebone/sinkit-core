package biz.karms.sinkit.ejb;

import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;

import javax.annotation.PostConstruct;
import javax.ejb.DependsOn;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
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

    @PostConstruct
    public void setup() {
        blacklistCache = m.getCache("BLACKLIST_CACHE");
        ruleCache = m.getCache("RULES_CACHE");
        if (blacklistCache == null || ruleCache == null) {
            throw new IllegalStateException("Both BLACKLIST_CACHE and RULES_CACHE must not be null.");
        }
    }

    public BlacklistedRecord getSinkHole(final String client, final String key) {
        log.log(Level.FINEST, "getting key [" + key + "]");
        return blacklistCache.get(key);
    }
}
