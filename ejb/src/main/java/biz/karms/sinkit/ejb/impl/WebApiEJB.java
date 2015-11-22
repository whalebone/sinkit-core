package biz.karms.sinkit.ejb.impl;

import biz.karms.sinkit.ejb.MyCacheManagerProvider;
import biz.karms.sinkit.ejb.WebApi;
import biz.karms.sinkit.ejb.cache.pojo.BlacklistedRecord;
import biz.karms.sinkit.ejb.cache.pojo.CustomList;
import biz.karms.sinkit.ejb.cache.pojo.Rule;
import biz.karms.sinkit.ejb.dto.AllDNSSettingDTO;
import biz.karms.sinkit.ejb.dto.CustomerCustomListDTO;
import biz.karms.sinkit.ejb.dto.FeedSettingCreateDTO;
import biz.karms.sinkit.ejb.util.CIDRUtils;
import com.google.common.collect.Lists;
import org.apache.commons.validator.routines.DomainValidator;
import org.infinispan.Cache;
import org.infinispan.query.Search;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Michal Karm Babacek
 */
@Stateless
//@TransactionManagement(TransactionManagementType.BEAN)
public class WebApiEJB implements WebApi {

    @Inject
    private Logger log;

    @Inject
    private MyCacheManagerProvider m;

    private Cache<String, BlacklistedRecord> blacklistCache = null;

    private Cache<String, Rule> ruleCache = null;

    private Cache<String, CustomList> customListsCache = null;

    //@Inject
    //private javax.transaction.UserTransaction utx;

    @PostConstruct
    public void setup() {
        blacklistCache = m.getCache("BLACKLIST_CACHE");
        ruleCache = m.getCache("RULES_CACHE");
        customListsCache = m.getCache("CUSTOM_LISTS_CACHE");
        if (blacklistCache == null || ruleCache == null || customListsCache == null) {
            throw new IllegalStateException("Both BLACKLIST_CACHE and RULES_CACHE and CUSTOM_LISTS_CACHE must not be null.");
        }
    }

    // Testing purposes
    @Override
    public String sayHello(final String queryString) {
        return "Hello there." + queryString;
    }

    @Override
    public Map<String, Integer> getStats() {
        Map<String, Integer> info = new HashMap<String, Integer>();
        info.put("ioc", blacklistCache.size());
        info.put("rule", ruleCache.size());
        return info;
    }

    @Override
    public BlacklistedRecord putBlacklistedRecord(final BlacklistedRecord blacklistedRecord) {
        if (blacklistedRecord == null || blacklistedRecord.getBlackListedDomainOrIP() == null) {
            log.log(Level.SEVERE, "putBlacklistedRecord: Got null record or IoC. Can't process this.");
            // TODO: Proper Error codes.
            return null;
        }
        try {
            //utx.begin();
            log.log(Level.FINE, "Putting key [" + blacklistedRecord.getBlackListedDomainOrIP() + "]");
            blacklistCache.put(blacklistedRecord.getBlackListedDomainOrIP(), blacklistedRecord);
            //utx.commit();
            // TODO: Is this O.K.? Maybe we should just return the same instance.
            return blacklistCache.get(blacklistedRecord.getBlackListedDomainOrIP());
        } catch (Exception e) {
            log.log(Level.SEVERE, "putBlacklistedRecord", e);
            /*try {
                if (utx.getStatus() != javax.transaction.Status.STATUS_NO_TRANSACTION) {
                    utx.rollback();
                }
            } catch (Exception e1) {
                log.log(Level.SEVERE, "putBlacklistedRecord", e1);
            }*/
            // TODO: Proper Error codes.
            return null;
        }
    }

    @Override
    public BlacklistedRecord getBlacklistedRecord(final String key) {
        log.log(Level.FINE, "getting key [" + key + "]");
        return blacklistCache.get(key);
    }

    @Override
    public Object[] getBlacklistedRecordKeys() {
        return blacklistCache.keySet().toArray();
    }

    @Override
    public String deleteBlacklistedRecord(final String key) {
        try {
            //utx.begin();
            String response;
            if (blacklistCache.containsKey(key)) {
                blacklistCache.remove(key);
                response = key + " DELETED";
            } else {
                response = key + " DOES NOT EXIST";
            }
            //utx.commit();
            return response;
        } catch (Exception e) {
            log.log(Level.SEVERE, "deleteBlacklistedRecord", e);
            /*try {
                if (utx.getStatus() != javax.transaction.Status.STATUS_NO_TRANSACTION) {
                    utx.rollback();
                }
            } catch (Exception e1) {
                log.log(Level.SEVERE, "deleteBlacklistedRecord", e1);
            }*/
            // TODO: Proper Error codes.
            return null;
        }
    }

    // TODO: List? Array? Map with additional data? Let's think this over.
    @Override
    public List<?> getRules(final String clientIPAddress) {
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
                List<Rule> wrapit = new ArrayList<>();
                wrapit.add(rule);
                return wrapit;
            }

            // Let's search subnets
/*
            SearchManager searchManager = org.infinispan.query.Search.getSearchManager(ruleCache);
            QueryBuilder queryBuilder = searchManager.buildQueryBuilderForClass(Rule.class).get();

            Query luceneQuery = queryBuilder
                    .bool()
                    .must(queryBuilder.range().onField("startAddress").below(clientIPAddressPaddedBigInt).createQuery())
                    .must(queryBuilder.range().onField("endAddress").above(clientIPAddressPaddedBigInt).createQuery())
                    .createQuery();

            CacheQuery query = searchManager.getQuery(luceneQuery, Rule.class);
            return query.list();
*/

            QueryFactory qf = Search.getQueryFactory(ruleCache);
            Query query = qf.from(Rule.class)
                    .having("startAddress").lte(clientIPAddressPaddedBigInt)
                    .and()
                    .having("endAddress").gte(clientIPAddressPaddedBigInt)
                    .toBuilder().build();
            List<Rule> list = query.list();
            return list;
        } catch (Exception e) {
            log.log(Level.SEVERE, "getRules client address troubles", e);
            // TODO: Proper Error codes.
            return null;
        }
    }

    // TODO: List? Array? Map with additional data? Let's think this over.
    @Override
    public List<?> getAllRules() {
        try {
            log.log(Level.SEVERE, "getAllRules: This is a very expensive operation.");
            return Lists.newArrayList(ruleCache.values());
        } catch (Exception e) {
            log.log(Level.SEVERE, "getRules client address troubles", e);
            // TODO: Proper Error codes.
            return null;
        }
    }

    @Override
    public Set<String> getRuleKeys() {
        return ruleCache.keySet();
    }

    @Override
    public String deleteRulesByCustomer(Integer customerId) {
        int counter = 0;
        //TODO: Shouldn't this be done by a one Infinispan DSL removal call?
        try {
            QueryFactory qf = Search.getQueryFactory(ruleCache);
            Query query = qf.from(Rule.class)
                    .having("customerId").eq(customerId)
                    .toBuilder().build();
            Iterator iterator = query.list().iterator();
            while (iterator.hasNext()) {
                ruleCache.removeAsync(((Rule) iterator.next()).getStartAddress());
                counter++;
            }
            return counter + " RULES DELETED FOR CUSTOMER ID: " + customerId;
        } catch (Exception e) {
            log.log(Level.SEVERE, "deleteRulesByCustomer", e);
            // TODO: Proper Error codes.
            return null;
        }
    }

    @Override
    public String deleteRule(final String cidrAddress) {
        try {
            //TODO: It is very wasteful to calculate the whole thing for just the one /32 or /128 masked client IP.
            CIDRUtils cidrUtils = new CIDRUtils(cidrAddress);
            String clientIPAddressPaddedBigInt = cidrUtils.getStartIPBigIntegerString();
            cidrUtils = null;
            log.log(Level.FINE, "Deleting key [" + cidrAddress + "] which actually translates to BigInteger zero padded representation " +
                    "[" + clientIPAddressPaddedBigInt + "]");
            //utx.begin();
            String response;
            if (ruleCache.containsKey(clientIPAddressPaddedBigInt)) {
                ruleCache.remove(clientIPAddressPaddedBigInt);
                response = clientIPAddressPaddedBigInt + " DELETED";
            } else {
                response = clientIPAddressPaddedBigInt + " DOES NOT EXIST";
            }
            //utx.commit();
            return response;
        } catch (Exception e) {
            log.log(Level.SEVERE, "deleteRule", e);
            /*try {
                if (utx.getStatus() != javax.transaction.Status.STATUS_NO_TRANSACTION) {
                    utx.rollback();
                }
            } catch (Exception e1) {
                log.log(Level.SEVERE, "deleteRule", e1);
            }*/
            // TODO: Proper Error codes.
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
    @Override
    public String putDNSClientSettings(final Integer customerId, final HashMap<String, HashMap<String, String>> customerDNSSetting) {
        try {
            /*.
            SearchManager searchManager = org.infinispan.query.Search.getSearchManager(ruleCache);
            QueryBuilder queryBuilder = searchManager.buildQueryBuilderForClass(Rule.class).get();
            Query luceneQuery = queryBuilder.keyword().onField("customerId").matching(customerId).createQuery();
            CacheQuery query = searchManager.getQuery(luceneQuery, Rule.class);
            */
            QueryFactory qf = Search.getQueryFactory(ruleCache);

            Query query = qf.from(Rule.class)
                    .having("customerId").eq(customerId)
                    .toBuilder().build();

            if (query != null && query.getResultSize() > 0) {
                List<Rule> list = query.list();
                for (Rule rule : list) {
                    //Rule rule = (Rule) itr.next();
                    if (customerDNSSetting.containsKey(rule.getCidrAddress())) {
                        rule.setSources(customerDNSSetting.get(rule.getCidrAddress()));
                        try {
                            //utx.begin();
                            ruleCache.replace(rule.getStartAddress(), rule);
                            //utx.commit();
                        } catch (Exception e) {
                            log.log(Level.SEVERE, "putDNSClientSettings", e);
                            /*try {
                                if (utx.getStatus() != javax.transaction.Status.STATUS_NO_TRANSACTION) {
                                    utx.rollback();
                                }
                            } catch (Exception e1) {
                                log.log(Level.SEVERE, "putDNSClientSettings", e1);
                                return null;
                            }*/
                            // TODO: Proper Error codes.
                            return null;
                        }
                    }
                }
            } else {
                for (String cidr : customerDNSSetting.keySet()) {
                    CIDRUtils cidrUtils = new CIDRUtils(cidr);
                    if (cidrUtils == null) {
                        log.log(Level.SEVERE, "putDNSClientSettings: We have failed to construct CIDRUtils instance.");
                        // TODO: Proper Error codes.
                        return null;
                    }
                    Rule rule = new Rule();
                    rule.setCidrAddress(cidr);
                    rule.setCustomerId(customerId);
                    rule.setSources(customerDNSSetting.get(cidr));
                    rule.setStartAddress(cidrUtils.getStartIPBigIntegerString());
                    rule.setEndAddress(cidrUtils.getEndIPBigIntegerString());
                    cidrUtils = null;
                    log.log(Level.FINE, "Putting key [" + rule.getStartAddress() + "]");
                    ruleCache.put(rule.getStartAddress(), rule);
                }
                log.log(Level.FINE, "putDNSClientSettings: customerId " + customerId + " does not exist, query result is either null or empty. We inserted it.");
                return customerId + " INSERTED";
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "putDNSClientSettings troubles", e);
            // TODO: Proper Error codes.
            return null;
        }
        return customerId + " SETTINGS UPDATED";
    }

    @Override
    public String postAllDNSClientSettings(final AllDNSSettingDTO[] allDNSSetting) {
        //TODO: Perhaps invert the flow: create 1 utx.begin();-utx.commit(); block and loop inside...
        for (AllDNSSettingDTO allDNSSettingDTO : allDNSSetting) {
            try {
                if (allDNSSettingDTO == null || allDNSSettingDTO.getDnsClient() == null || allDNSSettingDTO.getDnsClient().length() < 7) {
                    log.log(Level.SEVERE, "postAllDNSClientSettings: Got an invalid or null record. Can't process this.");
                    // TODO: Proper Error codes.
                    return null;
                }
                CIDRUtils cidrUtils = new CIDRUtils(allDNSSettingDTO.getDnsClient());
                if (cidrUtils == null) {
                    log.log(Level.SEVERE, "postAllDNSClientSettings: We have failed to construct CIDRUtils instance.");
                    // TODO: Proper Error codes.
                    return null;
                }
                Rule rule = new Rule();
                rule.setCidrAddress(allDNSSettingDTO.getDnsClient());
                rule.setCustomerId(allDNSSettingDTO.getCustomerId());
                rule.setSources(allDNSSettingDTO.getSettings());
                rule.setStartAddress(cidrUtils.getStartIPBigIntegerString());
                rule.setEndAddress(cidrUtils.getEndIPBigIntegerString());
                cidrUtils = null;
                //utx.begin();
                log.log(Level.FINE, "Putting key [" + rule.getStartAddress() + "]");
                ruleCache.put(rule.getStartAddress(), rule);
                //utx.commit();
            } catch (Exception e) {
                log.log(Level.SEVERE, "postAllDNSClientSettings", e);
                /*try {
                    if (utx.getStatus() != javax.transaction.Status.STATUS_NO_TRANSACTION) {
                        utx.rollback();
                    }
                } catch (Exception e1) {
                    log.log(Level.SEVERE, "postAllDNSClientSettings", e1);
                }*/
                // TODO: Proper Error codes.
                return null;
            }
        }
        return allDNSSetting.length + " RULES PROCESSED " + ruleCache.size() + " PRESENT";
    }

    /**
     * Creates/updates customer custom lists
     * <p>
     * TODO: This method is wayyy too long and complicated. Refactor CustomList creation out into some CustomList factory.
     * <p>
     * Key for the cache is a String constructed as the following concatenation:
     * "DNS client CIDR"+"<Listed CIDR | Listed FQDN>"  By Listed we mean a member of the list of White/Black/Log listed strings.
     * <p>
     * Note: Let's talk about the Key with Rattus and assert its length...<= 255? Should customerId be part of it? Should we use hashes?
     * Note: The same old problem with nested subnets: What if more Black/White lists span more nested subnets?
     *
     * @param customerId
     * @param customerCustomLists
     * @return
     */
    @Override
    public String putCustomLists(final Integer customerId, final CustomerCustomListDTO[] customerCustomLists) {
        if (customerId == null || customerCustomLists == null) {
            log.log(Level.SEVERE, "putCustomLists: customerId and customerCustomLists cannot be null.");
            // TODO: Proper Error codes.
            return null;
        }
        //TODO: If customerCustomLists is empty - should we clear/delete all customerId's lists? Ask Rattus.
        final DomainValidator domainValidator = DomainValidator.getInstance();
        String dnsClientStartAddress;
        String dnsClientEndAddress;
        int customListsElementCounter = 0;
        for (CustomerCustomListDTO customerCustomList : customerCustomLists) {
            // Let's calculate DNS Client address
            CIDRUtils cidrUtils;
            try {
                cidrUtils = new CIDRUtils(customerCustomList.getDnsClient());
                if (cidrUtils == null) {
                    log.log(Level.SEVERE, "putCustomLists: We have failed to construct CIDRUtils instance from " + customerCustomList.getDnsClient());
                    // TODO: Proper Error codes.
                    return null;
                }
                dnsClientStartAddress = cidrUtils.getStartIPBigIntegerString();
                dnsClientEndAddress = cidrUtils.getEndIPBigIntegerString();
                if (dnsClientStartAddress == null || dnsClientEndAddress == null) {
                    log.log(Level.SEVERE, "putCustomLists: dnsClientStartAddress or dnsClientEndAddress were null. This cannot happen.");
                    // TODO: Proper Error codes.
                    return null;
                }
            } catch (Exception e) {
                // TODO: More robust approach would be break();, but it could violate consistency...
                // Furthermore, with validation in Portal, this really shouldn't happen, hence return null;
                log.log(Level.SEVERE, "putCustomLists: Invalid CIDR " + customerCustomList.getDnsClient(), e);
                // TODO: Proper Error codes.
                return null;
            } finally {
                cidrUtils = null;
            }

            // Let's process list of Blacklisted / Whitelisted / Logged CIDRs and FQDNs.
            for (String fqdnOrCIDR : customerCustomList.getLists().keySet()) {
                // TODO: OMG, this should go to some CustomList factory.
                final CustomList customList = new CustomList();
                customList.setCustomerId(customerId);
                customList.setClientCidrAddress(customerCustomList.getDnsClient());
                customList.setClientStartAddress(dnsClientStartAddress);
                customList.setClientEndAddress(dnsClientEndAddress);
                String blacklistWhitelistLog = customerCustomList.getLists().get(fqdnOrCIDR);
                if (!(blacklistWhitelistLog != null && (blacklistWhitelistLog.equals("B") || blacklistWhitelistLog.equals("W") || blacklistWhitelistLog.equals("L")))) {
                    log.log(Level.SEVERE, "putCustomLists: Expected one of <B|W|L> but got: " + blacklistWhitelistLog + ". customListsElementCounter: " + customListsElementCounter);
                    // TODO: Proper Error codes.
                    return null;
                }
                customList.setWhiteBlackLog(blacklistWhitelistLog);
                if (domainValidator.isValid(fqdnOrCIDR)) {
                    //Let's assume it is a Domain name, not a CIDR formatted IP address or subnet
                    customList.setFqdn(fqdnOrCIDR);
                } else {
                    // In this case, we have to calculate CIDR subnet start and end address
                    try {
                        cidrUtils = new CIDRUtils(fqdnOrCIDR);
                        if (cidrUtils == null) {
                            log.log(Level.SEVERE, "putCustomLists: We have failed to construct CIDRUtils instance from " + fqdnOrCIDR + ". customListsElementCounter: " + customListsElementCounter);
                            // TODO: Proper Error codes.
                            return null;
                        }
                        final String startAddress = cidrUtils.getStartIPBigIntegerString();
                        final String endAddress = cidrUtils.getEndIPBigIntegerString();
                        if (startAddress == null || endAddress == null) {
                            log.log(Level.SEVERE, "putCustomLists: endAddress or startAddress were null for CIDR " + fqdnOrCIDR + ". This cannot happen. customListsElementCounter: " + customListsElementCounter);
                            // TODO: Proper Error codes.
                            return null;
                        }
                        customList.setListCidrAddress(fqdnOrCIDR);
                        customList.setListStartAddress(startAddress);
                        customList.setListEndAddress(endAddress);
                    } catch (Exception e) {
                        // TODO: More robust approach would be break();, but it could violate consistency...
                        // Furthermore, with validation in Portal, this really shouldn't happen, hence return null;
                        log.log(Level.SEVERE, "putCustomLists: Invalid CIDR " + customerCustomList.getDnsClient() + ", customListsElementCounter: " + customListsElementCounter, e);
                        // TODO: Proper Error codes.
                        return null;
                    } finally {
                        cidrUtils = null;
                    }
                }

                //At this point, we have a valid CustomList instance, let's process it with the cache.
                // Sanity check
                if ((customList.getFqdn() != null && customList.getListCidrAddress() != null) || (customList.getFqdn() == null && customList.getListCidrAddress() == null)) {
                    log.log(Level.SEVERE, "putCustomLists: Sanity violation, customList has exactly one of [FQDN,CIDR] set, not both, not none. customListsElementCounter: " + customListsElementCounter);
                    // TODO: Proper Error codes.
                    return null;
                }

                // TODO: Talk to Rattus. ClientCidrAddresses cannot be arbitrary and must be thoroughly validated in Portal. Should we hash this?
                final String key = customList.getClientCidrAddress() + ((customList.getFqdn() != null) ? customList.getFqdn() : customList.getListCidrAddress());

                try {
                    //utx.begin();
                    log.log(Level.FINE, "putCustomLists: Putting key [" + key + "]. customListsElementCounter: " + customListsElementCounter);
                    if (customListsCache.replace(key, customList) == null) {
                        customListsCache.put(key, customList);
                    }
                    //utx.commit();
                    customListsElementCounter++;
                } catch (Exception e) {
                    log.log(Level.SEVERE, "putCustomLists: customListsElementCounter: " + customListsElementCounter, e);
                    /*try {
                        if (utx.getStatus() != javax.transaction.Status.STATUS_NO_TRANSACTION) {
                            utx.rollback();
                        }
                    } catch (Exception e1) {
                        log.log(Level.SEVERE, "putCustomLists", e1);
                        return null; //finally?
                    }*/
                    // TODO: Proper Error codes.
                    return null;
                }
            }
        }
        return customListsElementCounter + " CUSTOM LISTS ELEMENTS PROCESSED, " + customListsCache.size() + " PRESENT";
    }

    /**
     * TODO: This is most likely wrong, let's talk to Rattus.
     * <p>
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
    @Override
    public String putFeedSettings(final String feedUid, final HashMap<Integer, HashMap<String, String>> feedSettings) {
        //CacheQuery query;
        Query query;
        int updated = 0;
        try {
            /*SearchManager searchManager = org.infinispan.query.Search.getSearchManager(ruleCache);
            QueryBuilder queryBuilder = searchManager.buildQueryBuilderForClass(Rule.class).get();
            Query luceneQuery = queryBuilder
                    .phrase()
                    .onField("sources")
                    .sentence(feedUid)
                    .createQuery();
                    */
            //.keyword() //TODO: This would need a new SettingsMapBridge and might not be faster anyway...
            //.onField("sources")
            //.matching(feedUid)
            //.createQuery();

            QueryFactory qf = Search.getQueryFactory(ruleCache);
            query = qf.from(Rule.class)
                    .having("sources").contains(feedUid)
                    .toBuilder().build();

            // query = searchManager.getQuery(luceneQuery, Rule.class);
            if (query != null && query.getResultSize() > 0) {
                //  Iterator itr = query.iterator();
                // while (itr.hasNext()) {
                //   Rule rule = (Rule) itr.next();
                List<Rule> list = query.list();
                for (Rule rule : list) {
                    HashMap<String, String> cidrMode = feedSettings.get(rule.getCustomerId());
                    if (cidrMode != null && cidrMode.containsKey(rule.getCidrAddress())) {
                        //TODO This is certainly wrong and overengineered... Let's talk to Rattus.
                        rule.getSources().replace(feedUid, cidrMode.get(rule.getCidrAddress()));
                        try {
                            //utx.begin();
                            ruleCache.replace(rule.getStartAddress(), rule);
                            //utx.commit();
                            updated++;
                        } catch (Exception e) {
                            log.log(Level.SEVERE, "putFeedSettings", e);
                            /*try {
                                if (utx.getStatus() != javax.transaction.Status.STATUS_NO_TRANSACTION) {
                                    utx.rollback();
                                }
                            } catch (Exception e1) {
                                log.log(Level.SEVERE, "putFeedSettings", e1);
                                return null; //finally?
                            }*/
                            // TODO: Proper Error codes.
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
            // TODO: Proper Error codes.
            return null;
        }
        return query.getResultSize() + " RULES FOUND " + updated + " UPDATED";

    }

    @Override
    public String postCreateFeedSettings(FeedSettingCreateDTO feedSettingCreate) {
        //TODO
        throw new NotImplementedException();
    }
}
