package biz.karms.sinkit.ejb;

import biz.karms.sinkit.ejb.util.CIDRUtils;
import com.google.gson.GsonBuilder;
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
import java.util.HashMap;
import java.util.Map;
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

    public static final String ERR_MSG = "Error, please, check your input.";

    // Testing purposes
    public String sayHello(String queryString) {
        return new GsonBuilder().create().toJson("Hello there." + queryString);
    }

    public String getStats() {
        Map<String, Integer> info = new HashMap<String, Integer>();
        info.put("Total documents", blacklistCache.size());
        return new GsonBuilder().create().toJson(info);
    }

    public String putBlacklistedRecord(String json) {
        try {
            log.info("Received JSON [" + json + "]");
            BlacklistedRecord blacklistedRecord = new GsonBuilder().create().fromJson(json, BlacklistedRecord.class);
            utx.begin();
            log.info("Putting key [" + blacklistedRecord.getBlackListedDomainOrIP() + "]");
            blacklistCache.put(blacklistedRecord.getBlackListedDomainOrIP(), blacklistedRecord);
            utx.commit();
            return new GsonBuilder().create().toJson(blacklistedRecord);
        } catch (Exception e) {
            e.printStackTrace();
            log.log(Level.SEVERE, "putBlacklistedRecord", e);
            try {
                if (utx.getStatus() != javax.transaction.Status.STATUS_NO_TRANSACTION) {
                    utx.rollback();
                }
            } catch (Exception e1) {
                // TODO Hmm, make it better :-(
                e1.printStackTrace();
            }
            return new GsonBuilder().create().toJson(ERR_MSG);
        }
    }

    public String getBlacklistedRecord(String key) {
        log.info("getting key [" + key + "]");
        BlacklistedRecord blacklistedRecord = blacklistCache.get(key);
        return new GsonBuilder().create().toJson(blacklistedRecord);
    }

    public String getBlacklistedRecordKeys() {
        return new GsonBuilder().create().toJson(blacklistCache.keySet());
    }

    public String deleteBlacklistedRecord(String key) {
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
            return new GsonBuilder().create().toJson(response);
        } catch (Exception e) {
            e.printStackTrace();
            log.log(Level.SEVERE, "deleteBlacklistedRecord", e);
            try {
                if (utx.getStatus() != javax.transaction.Status.STATUS_NO_TRANSACTION) {
                    utx.rollback();
                }
            } catch (Exception e1) {
                // TODO Hmm, make it better :-(
                e1.printStackTrace();
            }
            return new GsonBuilder().create().toJson(ERR_MSG);
        }
    }

    public String putRule(String json) {
        try {
            log.info("Received JSON [" + json + "]");
            Rule rule = new GsonBuilder().create().fromJson(json, Rule.class);
            // TODO: Move to factory or transformer
            if (rule == null) {
                throw new IllegalArgumentException("We have failed to construct class Rule from JSON: " + json);
            }

            //TODO: It is very wasteful to calculate the whole thing for just the one /32 or /128 masked client IP.
            CIDRUtils cidrUtils = new CIDRUtils(rule.getCidrAddress());
            if (cidrUtils == null) {
                throw new IllegalArgumentException("We have failed to construct CIDRUtils instance.");
            }
            rule.setStartAddress(cidrUtils.getStartIPBigIntegerString());
            rule.setEndAddress(cidrUtils.getEndIPBigIntegerString());
            cidrUtils = null;

            utx.begin();
            log.finest("Putting key [" + rule.getStartAddress() + "]");
            ruleCache.put(rule.getStartAddress(), rule);
            utx.commit();
            return new GsonBuilder().create().toJson(rule);
        } catch (Exception e) {
            e.printStackTrace();
            log.log(Level.SEVERE, "putRule", e);
            try {
                if (utx.getStatus() != javax.transaction.Status.STATUS_NO_TRANSACTION) {
                    utx.rollback();
                }
            } catch (Exception e1) {
                // TODO Hmm, make it better :-(
                e1.printStackTrace();
            }
            return new GsonBuilder().create().toJson(ERR_MSG);
        }
    }

    public String getRules(String clientIPAddress) {
        try {
            //TODO: It is very wasteful to calculate the whole thing for just the one /32 or /128 masked client IP.
            CIDRUtils cidrUtils = new CIDRUtils(clientIPAddress);
            String clientIPAddressPaddedBigInt = cidrUtils.getStartIPBigIntegerString();
            cidrUtils = null;
            log.finest("Getting key [" + clientIPAddress + "] which actually translates to BigInteger zero padded representation " +
                    "[" + clientIPAddressPaddedBigInt + "]");

            // Let's try to hit it
            Rule rule = ruleCache.get(clientIPAddressPaddedBigInt);
            if (rule != null) {
                // TODO refactor JSON stuff into sinkit-rest project. This is evil and error prone.
                return new GsonBuilder().create().toJson(new Rule[]{rule});
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
            // TODO refactor JSON stuff into sinkit-rest project. This is evil and error prone.
            return new GsonBuilder().create().toJson(query.list());
        } catch (Exception e) {
            log.log(Level.SEVERE, "getRules client address troubles", e);
            return new GsonBuilder().create().toJson(ERR_MSG);
        }
    }

    public String getRuleKeys() {
        return new GsonBuilder().create().toJson(ruleCache.keySet());
    }

    public String deleteRule(String clientIPAddress) {
        try {
            //TODO: It is very wasteful to calculate the whole thing for just the one /32 or /128 masked client IP.
            CIDRUtils cidrUtils = new CIDRUtils(clientIPAddress);
            String clientIPAddressPaddedBigInt = cidrUtils.getStartIPBigIntegerString();
            cidrUtils = null;
            log.finest("Deleting key [" + clientIPAddress + "] which actually translates to BigInteger zero padded representation " +
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
            return new GsonBuilder().create().toJson(response);
        } catch (Exception e) {
            e.printStackTrace();
            log.log(Level.SEVERE, "deleteRule", e);
            try {
                if (utx.getStatus() != javax.transaction.Status.STATUS_NO_TRANSACTION) {
                    utx.rollback();
                }
            } catch (Exception e1) {
                // TODO Hmm, make it better :-(
                e1.printStackTrace();
            }
            return new GsonBuilder().create().toJson(ERR_MSG);
        }
    }
}
