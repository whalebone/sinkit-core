package biz.karms.sinkit.ejb;

import biz.karms.sinkit.ejb.util.CIDRUtils;
import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.query.CacheQuery;
import org.infinispan.query.SearchManager;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Michal Karm Babacek
 */
// TODO: Really?
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class ServiceEJB {

    @Inject
    private Logger log;

    @Inject
    private DefaultCacheManager m;

    private Cache<String, BlacklistedRecord> blacklistCache = null;

    private Cache<String, Rule> ruleCache = null;

    @Inject
    private javax.transaction.UserTransaction utx;

    @PostConstruct
    public void setup() {
        blacklistCache = m.getCache("BLACKLIST_CACHE");
        ruleCache = m.getCache("RULES_CACHE");
    }

    // Testing purposes
    public String sayHello(final String queryString) {
        return "Hello there." + queryString;
    }

    public Map<String, Integer> getStats() {
        Map<String, Integer> info = new HashMap<String, Integer>();
        info.put("Total documents", blacklistCache.size());
        return info;
    }

    public BlacklistedRecord putBlacklistedRecord(final BlacklistedRecord blacklistedRecord) {
        if (blacklistedRecord == null || blacklistedRecord.getBlackListedDomainOrIP() == null) {
            log.log(Level.SEVERE, "putBlacklistedRecord: Got null record or IoC. Can't process this.");
            return null;
        }
        try {
            utx.begin();
            log.log(Level.FINEST, "Putting key [" + blacklistedRecord.getBlackListedDomainOrIP() + "]");
            blacklistCache.put(blacklistedRecord.getBlackListedDomainOrIP(), blacklistedRecord);
            utx.commit();
            // TODO: Is this O.K.? Maybe we should just return the same instance.
            return blacklistCache.get(blacklistedRecord.getBlackListedDomainOrIP());
        } catch (Exception e) {
            log.log(Level.SEVERE, "putBlacklistedRecord", e);
            try {
                if (utx.getStatus() != javax.transaction.Status.STATUS_NO_TRANSACTION) {
                    utx.rollback();
                }
            } catch (Exception e1) {
                // TODO Hmm, make it better :-(
                e1.printStackTrace();
            }
            return null;
        }
    }

    public BlacklistedRecord getBlacklistedRecord(final String key) {
        log.log(Level.FINEST, "getting key [" + key + "]");
        return blacklistCache.get(key);
    }

    public Set<String> getBlacklistedRecordKeys() {
        return blacklistCache.keySet();
    }

    public String deleteBlacklistedRecord(final String key) {
        try {
            utx.begin();
            String response;
            if (blacklistCache.containsKey(key)) {
                blacklistCache.remove(key);
                response = key + " DELETED";
            } else {
                response = key + " DOES NOT EXIST";
            }
            utx.commit();
            return response;
        } catch (Exception e) {
            log.log(Level.SEVERE, "deleteBlacklistedRecord", e);
            try {
                if (utx.getStatus() != javax.transaction.Status.STATUS_NO_TRANSACTION) {
                    utx.rollback();
                }
            } catch (Exception e1) {
                // TODO Hmm, make it better :-(
                e1.printStackTrace();
            }
            return null;
        }
    }

    public Rule putRule(Rule rule) {
        try {
            if (rule == null || rule.getCidrAddress() == null || rule.getCidrAddress().length() < 7) {
                log.log(Level.SEVERE, "putRule: Got invalid or null 'rule' record. Can't process this.");
                return null;
            }
            // TODO: If we had something like RuleDTO without *Address attributes, we wouldn't need this check.
            if (rule.getStartAddress() != null || rule.getEndAddress() != null) {
                log.log(Level.SEVERE, "putRule: getStartAddress or getEndAddress ain't null. This is weird, we should be setting these exclusively here.");
                return null;
            }
            //TODO: It is very wasteful to calculate the whole thing for just the one /32 or /128 masked client IP.
            CIDRUtils cidrUtils = new CIDRUtils(rule.getCidrAddress());
            if (cidrUtils == null) {
                log.log(Level.SEVERE, "putRule: We have failed to construct CIDRUtils instance.");
                return null;
            }
            rule.setStartAddress(cidrUtils.getStartIPBigIntegerString());
            rule.setEndAddress(cidrUtils.getEndIPBigIntegerString());
            cidrUtils = null;
            utx.begin();
            log.log(Level.FINEST, "Putting key [" + rule.getStartAddress() + "]");
            ruleCache.put(rule.getStartAddress(), rule);
            utx.commit();
            // TODO: Is this O.K.? Maybe we should just return the same instance.
            return ruleCache.get(rule.getStartAddress());
        } catch (Exception e) {
            log.log(Level.SEVERE, "putRule", e);
            try {
                if (utx.getStatus() != javax.transaction.Status.STATUS_NO_TRANSACTION) {
                    utx.rollback();
                }
            } catch (Exception e1) {
                // TODO Hmm, make it better :-(
                e1.printStackTrace();
            }
            return null;
        }
    }

    // TODO: List? Array? Map with additional data? Let's think this over.
    public List<Object> getRules(final String clientIPAddress) {
        try {
            //TODO: It is very wasteful to calculate the whole thing for just the one /32 or /128 masked client IP.
            CIDRUtils cidrUtils = new CIDRUtils(clientIPAddress);
            final String clientIPAddressPaddedBigInt = cidrUtils.getStartIPBigIntegerString();
            cidrUtils = null;
            log.log(Level.FINEST, "Getting key [" + clientIPAddress + "] which actually translates to BigInteger zero padded representation " +
                    "[" + clientIPAddressPaddedBigInt + "]");
            // Let's try to hit it
            Rule rule = ruleCache.get(clientIPAddressPaddedBigInt);
            if (rule != null) {
                List wrapit = new ArrayList<>();
                wrapit.add(rule);
                return wrapit;
            }

            // Let's search subnets
            SearchManager searchManager = org.infinispan.query.Search.getSearchManager(ruleCache);
            QueryBuilder queryBuilder = searchManager.buildQueryBuilderForClass(Rule.class).get();

            Query luceneQuery = queryBuilder
                    .bool()
                    .must(queryBuilder.range().onField("startAddress").below(clientIPAddressPaddedBigInt).createQuery())
                    .must(queryBuilder.range().onField("endAddress").above(clientIPAddressPaddedBigInt).createQuery())
                    .createQuery();

            CacheQuery query = searchManager.getQuery(luceneQuery, Rule.class);
            return query.list();
        } catch (Exception e) {
            log.log(Level.SEVERE, "getRules client address troubles", e);
            return null;
        }
    }

    public Set<String> getRuleKeys() {
        return ruleCache.keySet();
    }

    public String deleteRule(final String cidrAddress) {
        try {
            //TODO: It is very wasteful to calculate the whole thing for just the one /32 or /128 masked client IP.
            CIDRUtils cidrUtils = new CIDRUtils(cidrAddress);
            String clientIPAddressPaddedBigInt = cidrUtils.getStartIPBigIntegerString();
            cidrUtils = null;
            log.log(Level.FINEST, "Deleting key [" + cidrAddress + "] which actually translates to BigInteger zero padded representation " +
                    "[" + clientIPAddressPaddedBigInt + "]");
            utx.begin();
            String response;
            if (ruleCache.containsKey(clientIPAddressPaddedBigInt)) {
                ruleCache.remove(clientIPAddressPaddedBigInt);
                response = clientIPAddressPaddedBigInt + " DELETED";
            } else {
                response = clientIPAddressPaddedBigInt + " DOES NOT EXIST";
            }
            utx.commit();
            return response;
        } catch (Exception e) {
            log.log(Level.SEVERE, "deleteRule", e);
            try {
                if (utx.getStatus() != javax.transaction.Status.STATUS_NO_TRANSACTION) {
                    utx.rollback();
                }
            } catch (Exception e1) {
                // TODO Hmm, make it better :-(
                e1.printStackTrace();
            }
            return null;
        }
    }


}
