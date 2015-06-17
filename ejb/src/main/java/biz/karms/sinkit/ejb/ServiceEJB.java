package biz.karms.sinkit.ejb;

import biz.karms.sinkit.ejb.util.BigIntegerTransformable;
import biz.karms.sinkit.ejb.util.CIDRUtils;
import com.google.gson.GsonBuilder;
import org.apache.lucene.search.NumericRangeQuery;
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
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
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

    private Cache<BigIntegerTransformable, Rule> ruleCache = null;

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
            // TODO: Move to factory
            rule.setCidrAddress(rule.getCidrAddress());
            if (rule == null || rule.getStartAddress() == null) {
                throw new IllegalArgumentException("We have failed to construct class Rule from JSON: " + json);
            }
            utx.begin();
            log.info("Putting key [" + rule.getStartAddress() + "]");
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
        CIDRUtils cidrUtils;
        try {
            //TODO: It is very wasteful to calculate the whole thing for just the one /32 or /128 masked client IP.
            cidrUtils = new CIDRUtils(clientIPAddress);
        } catch (Exception e) {
            log.log(Level.SEVERE, "getRules client address troubles", e);
            return new GsonBuilder().create().toJson(ERR_MSG);
        }
        log.info("getting key [" + clientIPAddress + "] which actually translates to BigIntegerTransformable [" + cidrUtils.getStartIp() + "]");
        if (cidrUtils != null) {



            cidrUtils.getStartIp().toString()


                SearchManager searchManager = org.infinispan.query.Search.getSearchManager(ruleCache);
            org.apache.lucene.search.Query pageQueryRange = NumericRangeQuery.




            QueryBuilder queryBuilder = searchManager.buildQueryBuilderForClass(Rule.class).get();
                org.apache.lucene.search.Query luceneQuery = queryBuilder.phrase()
                        .onField("queryTerms")
                        .andField("content")
                        .andField("source")
                        .andField("social")
                        .sentence(querySentence)
                        .createQuery();
                CacheQuery query = searchManager.getQuery(luceneQuery, Rule.class);
                log.log(Level.INFO, "Searching for terms [" + querySentence + "], max_results [" + maxResults + "]");
                List<Object> list = query.maxResults(maxResults).list();
                // TODO Is it wise to jet Gson processLists?
                return new GsonBuilder().create().toJson(list);




            info.put("API added users to process", searchManager.getQuery(pageQueryRange, UserNode.class).getResultSize());


            return new GsonBuilder().create().toJson(ruleCache.get(cidrUtils.getStartIp()));
        }
        cidrUtils = null;
        return new GsonBuilder().create().toJson(ERR_MSG);
    }

    public String getRuleKeys() {
        return new GsonBuilder().create().toJson(ruleCache.keySet());
    }

    public String deleteRule(String clientIPAddress) {
        CIDRUtils cidrUtils;
        try {
            //TODO: It is very wasteful to calculate the whole thing for just the one /32 or /128 masked client IP.
            cidrUtils = new CIDRUtils(clientIPAddress);
        } catch (UnknownHostException e) {
            log.log(Level.SEVERE, "getRules client address troubles", e);
            return new GsonBuilder().create().toJson(ERR_MSG);
        }
        try {
            utx.begin();
            String response;
            if (ruleCache.containsKey(cidrUtils.getStartIp())) {
                ruleCache.remove(cidrUtils.getStartIp());
                response = cidrUtils.getStartIp() + " DELETED";
            } else {
                response = cidrUtils.getStartIp() + " DOES NOT EXIST";
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
}
