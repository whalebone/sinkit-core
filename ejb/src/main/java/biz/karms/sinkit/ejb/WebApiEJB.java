package biz.karms.sinkit.ejb;

import biz.karms.sinkit.ejb.dto.AllDNSSettingDTO;
import biz.karms.sinkit.ejb.dto.CustomerCustomListDTO;
import biz.karms.sinkit.ejb.dto.FeedSettingCreateDTO;
import biz.karms.sinkit.ejb.util.CIDRUtils;
import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.query.CacheQuery;
import org.infinispan.query.SearchManager;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

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
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class WebApiEJB {

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
        if (blacklistCache == null || ruleCache == null) {
            throw new IllegalStateException("Both BLACKLIST_CACHE and RULES_CACHE must not be null.");
        }
    }

    // Testing purposes
    public String sayHello(final String queryString) {
        return "Hello there." + queryString;
    }

    public Map<String, Integer> getStats() {
        Map<String, Integer> info = new HashMap<String, Integer>();
        info.put("ioc", blacklistCache.size());
        info.put("rule", ruleCache.size());
        return info;
    }

    public BlacklistedRecord putBlacklistedRecord(final BlacklistedRecord blacklistedRecord) {
        if (blacklistedRecord == null || blacklistedRecord.getBlackListedDomainOrIP() == null) {
            log.log(Level.SEVERE, "putBlacklistedRecord: Got null record or IoC. Can't process this.");
            return null;
        }
        try {
            utx.begin();
            log.log(Level.FINE, "Putting key [" + blacklistedRecord.getBlackListedDomainOrIP() + "]");
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
                log.log(Level.SEVERE, "putBlacklistedRecord", e1);
            }
            return null;
        }
    }

    public BlacklistedRecord getBlacklistedRecord(final String key) {
        log.log(Level.FINE, "getting key [" + key + "]");
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
                log.log(Level.SEVERE, "deleteBlacklistedRecord", e1);
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
            log.log(Level.FINE, "Getting key [" + clientIPAddress + "] which actually translates to BigInteger zero padded representation " +
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
            log.log(Level.FINE, "Deleting key [" + cidrAddress + "] which actually translates to BigInteger zero padded representation " +
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
                log.log(Level.SEVERE, "deleteRule", e1);
            }
            return null;
        }
    }

    /**
     * - lookup all Rule instances with particular customerId
     * - in those results, find those with dnsClient matching dnsClient in customerDNSSetting
     * - in these matching instances, replace current FeedModeDTO settings with the new one
     *
     * @param customerId
     * @param customerDNSSetting Map K(dnsClient in CIDR) : V(HashMap<String, String>), where HashMap<String, String> stands for: "feedUID" : "<L|S|D>"
     * @return
     */
    public String putDNSClientSettings(final Integer customerId, final HashMap<String, HashMap<String, String>> customerDNSSetting) {
        try {
            SearchManager searchManager = org.infinispan.query.Search.getSearchManager(ruleCache);
            QueryBuilder queryBuilder = searchManager.buildQueryBuilderForClass(Rule.class).get();
            Query luceneQuery = queryBuilder.keyword().onField("customerId").matching(customerId).createQuery();
            CacheQuery query = searchManager.getQuery(luceneQuery, Rule.class);
            if (query != null && query.list().size() > 0) {
                Iterator itr = query.iterator();
                while (itr.hasNext()) {
                    Rule rule = (Rule) itr.next();
                    if (customerDNSSetting.containsKey(rule.getCidrAddress())) {
                        rule.setSources(customerDNSSetting.get(rule.getCidrAddress()));
                        try {
                            utx.begin();
                            ruleCache.replace(rule.getStartAddress(), rule);
                            utx.commit();
                        } catch (Exception e) {
                            log.log(Level.SEVERE, "putDNSClientSettings", e);
                            try {
                                if (utx.getStatus() != javax.transaction.Status.STATUS_NO_TRANSACTION) {
                                    utx.rollback();
                                }
                            } catch (Exception e1) {
                                log.log(Level.SEVERE, "putDNSClientSettings", e1);
                                return null;
                            }
                            return null;
                        }
                    }
                }
            } else {
                log.log(Level.SEVERE, "putDNSClientSettings: customerId " + customerId + " does not exist, query result is either null or empty.");
                return customerId + " DOES NOT EXIST";
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "putDNSClientSettings troubles", e);
            return null;
        }
        return customerId + " SETTINGS UPDATED";
    }

    public String postAllDNSClientSettings(final AllDNSSettingDTO[] allDNSSetting) {
        //TODO: Perhaps invert the flow: create 1 utx.begin();-utx.commit(); block and loop inside...
        for (AllDNSSettingDTO allDNSSettingDTO : allDNSSetting) {
            try {
                if (allDNSSettingDTO == null || allDNSSettingDTO.getDnsClient() == null || allDNSSettingDTO.getDnsClient().length() < 7) {
                    log.log(Level.SEVERE, "postAllDNSClientSettings: Got an invalid or null record. Can't process this.");
                    return null;
                }
                //TODO: It is very wasteful to calculate the whole thing for just the one /32 or /128 masked client IP.
                CIDRUtils cidrUtils = new CIDRUtils(allDNSSettingDTO.getDnsClient());
                if (cidrUtils == null) {
                    log.log(Level.SEVERE, "postAllDNSClientSettings: We have failed to construct CIDRUtils instance.");
                    return null;
                }
                Rule rule = new Rule();
                rule.setCidrAddress(allDNSSettingDTO.getDnsClient());
                rule.setCustomerId(allDNSSettingDTO.getCustomerId());
                rule.setSources(allDNSSettingDTO.getSettings());
                rule.setStartAddress(cidrUtils.getStartIPBigIntegerString());
                rule.setEndAddress(cidrUtils.getEndIPBigIntegerString());
                cidrUtils = null;
                utx.begin();
                log.log(Level.FINE, "Putting key [" + rule.getStartAddress() + "]");
                ruleCache.put(rule.getStartAddress(), rule);
                utx.commit();
            } catch (Exception e) {
                log.log(Level.SEVERE, "postAllDNSClientSettings", e);
                try {
                    if (utx.getStatus() != javax.transaction.Status.STATUS_NO_TRANSACTION) {
                        utx.rollback();
                    }
                } catch (Exception e1) {
                    log.log(Level.SEVERE, "postAllDNSClientSettings", e1);
                }
                return null;
            }
        }
        return allDNSSetting.length + " RULES PROCESSED " + ruleCache.size() + " PRESENT";
    }

    public String putCustomLists(final Integer customerId, final CustomerCustomListDTO[] customerCustomLists) {
        //TODO
        throw new NotImplementedException();
    }

    /**
     * TODO: This is most likely wrong, let's talk to Rattus.
     *
     * # Example
     * ## Add some rules
     * ```
     * curl -H "Content-Type: application/json" -H "X-sinkit-token: ${SINKIT_ACCESS_TOKEN}" -X POST -d '[{"dns_client":"10.10.10.10/32","settings":{"feed-3":"D","feed2":"S","test-feed1":"L"},"customer_id":2,"customer_name":"yadayada-2"},{"dns_client":"10.10.10.11/32","settings":{"feed-3":"D","feed2":"S","test-feed1":"L"},"customer_id":2,"customer_name":"yadayada-2"},{"dns_client":"10.11.12.0/24","settings":{"test-feed1":"L","feed2":"S","feed-3":"D"},"customer_id":1,"customer_name":"test-yadayada"},{"dns_client":"10.11.30.30/32","settings":{"test-feed1":"L","feed2":"S","feed-3":"D"},"customer_id":1,"customer_name":"test-yadayada"}]' http://localhost:8080/sinkit/rest/rules/all
     * "4 RULES PROCESSED 4 PRESENT"
     * ```
     * ## Take a look at one of them, note: ```"feed-3":"D"```
     * ```
     * curl -H "Accept: application/json"  -H "X-sinkit-token: ${SINKIT_ACCESS_TOKEN}" -X GET http://localhost:8080/sinkit/rest/rules/10.10.10.10
     * [{"start_address":"0000000000000000000000000000000168430090","end_address":"0000000000000000000000000000000168430090","cidr_address":"10.10.10.10/32","customer_id":2,"sources":{"feed-3":"D","feed2":"S","test-feed1":"L"}}]
     * ```
     * ## Set ```"feed-3":"S"``` for a particular customer (customer_id 2) for his CIDR:
     * ```
     * curl -H "Content-Type: application/json" -H "X-sinkit-token: ${SINKIT_ACCESS_TOKEN}" -X PUT -d '{"2":{"10.10.10.10/32":"S"}}' http://localhost:8080/sinkit/rest/feed/feed-3
     * "4 RULES FOUND 1 UPDATED"
     * ```
     * ## Note ```"feed-3":"S"```
     * ```
     * curl -H "Accept: application/json"  -H "X-sinkit-token: ${SINKIT_ACCESS_TOKEN}" -X GET http://localhost:8080/sinkit/rest/rules/10.10.10.10
     * [{"start_address":"0000000000000000000000000000000168430090","end_address":"0000000000000000000000000000000168430090","cidr_address":"10.10.10.10/32","customer_id":2,"sources":{"feed-3":"S","feed2":"S","test-feed1":"L"}}]
     * ```
     * ## Note other ```feed-3``` settings remained intact:
     * ```
     * curl -H "Accept: application/json"  -H "X-sinkit-token: ${SINKIT_ACCESS_TOKEN}" -X GET http://localhost:8080/sinkit/rest/rules/10.11.12.22
     * [{"start_address":"0000000000000000000000000000000168496128","end_address":"0000000000000000000000000000000168496383","cidr_address":"10.11.12.0/24","customer_id":1,"sources":{"feed2":"S","feed-3":"D","test-feed1":"L"}}]
     * ```
     *
     * @param feedUid
     * @param feedSettings
     * @return null or result message
     */
    public String putFeedSettings(final String feedUid, final HashMap<Integer, HashMap<String, String>> feedSettings) {
        CacheQuery query;
        int updated = 0;
        try {
            SearchManager searchManager = org.infinispan.query.Search.getSearchManager(ruleCache);
            QueryBuilder queryBuilder = searchManager.buildQueryBuilderForClass(Rule.class).get();
            Query luceneQuery = queryBuilder
                    .phrase()
                    .onField("sources")
                    .sentence(feedUid)
                    .createQuery();
            //.keyword() //TODO: This would need a new SettingsMapBridge and might not be faster anyway...
            //.onField("sources")
            //.matching(feedUid)
            //.createQuery();
            query = searchManager.getQuery(luceneQuery, Rule.class);
            if (query != null && query.list().size() > 0) {
                Iterator itr = query.iterator();
                while (itr.hasNext()) {
                    Rule rule = (Rule) itr.next();
                    HashMap<String, String> cidrMode = feedSettings.get(rule.getCustomerId());
                    if (cidrMode != null && cidrMode.containsKey(rule.getCidrAddress())) {
                        //TODO This is certainly wrong and overengineered... Let's talk to Rattus.
                        rule.getSources().replace(feedUid, cidrMode.get(rule.getCidrAddress()));
                        try {
                            utx.begin();
                            ruleCache.replace(rule.getStartAddress(), rule);
                            utx.commit();
                            updated++;
                        } catch (Exception e) {
                            log.log(Level.SEVERE, "putFeedSettings", e);
                            try {
                                if (utx.getStatus() != javax.transaction.Status.STATUS_NO_TRANSACTION) {
                                    utx.rollback();
                                }
                            } catch (Exception e1) {
                                log.log(Level.SEVERE, "putFeedSettings", e1);
                                return null;
                            }
                            return null;
                        }
                    }
                }
            } else {
                log.log(Level.SEVERE, "putFeedSettings: feedUid " + feedUid + " did not return any results from Rules cache.");
                return feedUid + " HAS NO RESULTS";
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "putFeedSettings troubles", e);
            return null;
        }
        return query.list().size() + " RULES FOUND " + updated + " UPDATED";

    }

    public String postCreateFeedSettings(FeedSettingCreateDTO feedSettingCreate) {
        //TODO
        throw new NotImplementedException();
    }
}
