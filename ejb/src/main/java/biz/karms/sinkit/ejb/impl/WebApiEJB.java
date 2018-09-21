package biz.karms.sinkit.ejb.impl;

import biz.karms.sinkit.ejb.WebApi;
import biz.karms.sinkit.ejb.cache.annotations.SinkitCache;
import biz.karms.sinkit.ejb.cache.annotations.SinkitCacheName;
import biz.karms.sinkit.ejb.cache.pojo.BlacklistedRecord;
import biz.karms.sinkit.ejb.cache.pojo.CustomList;
import biz.karms.sinkit.ejb.cache.pojo.GSBRecord;
import biz.karms.sinkit.ejb.cache.pojo.Rule;
import biz.karms.sinkit.ejb.cache.pojo.WhitelistedRecord;
import biz.karms.sinkit.ejb.dto.AllDNSSettingDTO;
import biz.karms.sinkit.ejb.dto.CustomerCustomListDTO;
import biz.karms.sinkit.ejb.dto.FeedSettingCreateDTO;
import biz.karms.sinkit.ejb.util.CIDRUtils;
import biz.karms.sinkit.ejb.util.EndUserConfigurationValidator;
import biz.karms.sinkit.ejb.util.ResolverConfigurationValidator;
import biz.karms.sinkit.exception.EndUserConfigurationValidationException;
import biz.karms.sinkit.exception.ResolverConfigurationValidationException;
import biz.karms.sinkit.resolver.EndUserConfiguration;
import biz.karms.sinkit.resolver.ResolverConfiguration;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.validator.routines.DomainValidator;
import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.Search;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * @author Michal Karm Babacek
 */
@Stateless
public class WebApiEJB implements WebApi {

    @Inject
    private Logger log;

    @Inject
    @SinkitCache(SinkitCacheName.infinispan_blacklist)
    private RemoteCache<String, BlacklistedRecord> blacklistCache;

    @Inject
    @SinkitCache(SinkitCacheName.cache_manager_indexable)
    private RemoteCacheManager cacheManagerForIndexableCaches;

    @Inject
    @SinkitCache(SinkitCacheName.infinispan_whitelist)
    private RemoteCache<String, WhitelistedRecord> whitelistCache;

    @Inject
    @SinkitCache(SinkitCacheName.infinispan_gsb)
    private RemoteCache<String, GSBRecord> gsbCache;

    @Inject
    @SinkitCache(SinkitCacheName.resolver_configuration)
    private RemoteCache<Integer, ResolverConfiguration> resolverConfigurationCache;

    @Inject
    @SinkitCache(SinkitCacheName.end_user_configuration)
    private RemoteCache<String, EndUserConfiguration> endUserConfigurationCache;

    @Inject
    private ResolverConfigurationValidator resolverConfigurationValidator;

    @Inject
    private EndUserConfigurationValidator endUserConfigurationValidator;

    // Testing/playground purposes
    @Override
    public String sayHello(final String queryString) {
        final OperatingSystemMXBean osmxbean = ManagementFactory.getOperatingSystemMXBean();
        final MemoryMXBean memxbean = ManagementFactory.getMemoryMXBean();
        final double load = osmxbean.getSystemLoadAverage() / osmxbean.getAvailableProcessors();
        final MemoryUsage mamUsage = memxbean.getHeapMemoryUsage();
        final long maxMem = mamUsage.getMax();
        final double usedmem = mamUsage.getUsed();
        return String.join(",", "Hello there ", queryString, ". My CPU load is ", Double.toString(load), "my heap mem usage is ", Double.toString(usedmem / ((maxMem >= 0) ? maxMem : mamUsage.getCommitted())));
    }

    @Override
    public Map<String, Map<String, String>> getStats() {
        final Map<String, Map<String, String>> info = new HashMap<>();
        info.put("Rules", cacheManagerForIndexableCaches.getCache(SinkitCacheName.infinispan_rules.toString()).stats().getStatsMap());
        info.put("CustomLists", cacheManagerForIndexableCaches.getCache(SinkitCacheName.infinispan_custom_lists.toString()).stats().getStatsMap());
        info.put("Whitelist", whitelistCache.stats().getStatsMap());
        info.put("GSB", gsbCache.stats().getStatsMap());
        info.put("Blacklist", blacklistCache.stats().getStatsMap());
        info.put("Resolver configurations", resolverConfigurationCache.stats().getStatsMap());
        info.put("End User configurations", endUserConfigurationCache.stats().getStatsMap());
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
            log.log(Level.FINE, "Putting key [" + blacklistedRecord.getBlackListedDomainOrIP() + "]");
            blacklistCache.put(blacklistedRecord.getBlackListedDomainOrIP(), blacklistedRecord);
            // TODO: Is this O.K.? Maybe we should just return the same instance.
            return blacklistCache.get(DigestUtils.md5Hex(blacklistedRecord.getBlackListedDomainOrIP()));
        } catch (Exception e) {
            log.log(Level.SEVERE, "putBlacklistedRecord", e);
            // TODO: Proper Error codes.
            return null;
        }
    }

    @Override
    public BlacklistedRecord getBlacklistedRecord(final String key) {
        log.log(Level.FINE, "getting key [" + key + "]");
        return blacklistCache.get(DigestUtils.md5Hex(key));
    }

    @Override
    public Object[] getBlacklistedRecordKeys() {
        return blacklistCache.keySet().toArray();
    }

    @Override
    public String deleteBlacklistedRecord(final String key) {
        try {
            String response;
            final String hashedKey = DigestUtils.md5Hex(key);
            if (blacklistCache.containsKey(hashedKey)) {
                blacklistCache.remove(hashedKey);
                response = key + " DELETED";
            } else {
                response = key + " DOES NOT EXIST";
            }
            return response;
        } catch (Exception e) {
            log.log(Level.SEVERE, "deleteBlacklistedRecord", e);
            // TODO: Proper Error codes.
            return null;
        }
    }

    // TODO: List? Array? Map with additional data? Let's think this over.
    @Override
    public List<?> getRules(final String clientIPAddress) {
        try {
            final RemoteCache<String, Rule> ruleCache = cacheManagerForIndexableCaches.getCache(SinkitCacheName.infinispan_rules.toString());
            final ImmutablePair<String, String> startEndAddresses = CIDRUtils.getStartEndAddresses(clientIPAddress);
            final String clientIPAddressPaddedBigInt = startEndAddresses.getLeft();
            log.log(Level.FINE, "Getting key [" + clientIPAddress + "] which actually translates to BigInteger zero padded representation " + "[" + clientIPAddressPaddedBigInt + "]");
            // Let's try to hit it
            Rule rule = ruleCache.withFlags(Flag.SKIP_CACHE_LOAD).get(clientIPAddressPaddedBigInt);
            if (rule != null) {
                return Collections.singletonList(rule);
            }
            QueryFactory qf = Search.getQueryFactory(ruleCache);
            Query query = qf.from(Rule.class)
                    .having("startAddress").lte(clientIPAddressPaddedBigInt)
                    .and()
                    .having("endAddress").gte(clientIPAddressPaddedBigInt)
                    .toBuilder().build();
            return query.list();
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
            final RemoteCache<String, Rule> ruleCache = cacheManagerForIndexableCaches.getCache(SinkitCacheName.infinispan_rules.toString());
            log.log(Level.SEVERE, "getAllRules: This is a very expensive operation.");
            final QueryFactory qf = Search.getQueryFactory(ruleCache);
            final Query query = qf.from(Rule.class).build();
            // Hundreds of records...
            List<Rule> results = query.list();
            return results;
        } catch (Exception e) {
            log.log(Level.SEVERE, "getRules client address troubles", e);
            // TODO: Proper Error codes.
            return null;
        }
    }

    @Override
    public Set<String> getRuleKeys() {
        final RemoteCache<String, Rule> ruleCache = cacheManagerForIndexableCaches.getCache(SinkitCacheName.infinispan_rules.toString());
        return ruleCache.keySet();
    }

    @Override
    public String deleteRulesByCustomer(Integer customerId) {
        int counter = 0;
        //TODO: Shouldn't this be done by a one Infinispan DSL removal call?
        try {
            final RemoteCache<String, Rule> ruleCache = cacheManagerForIndexableCaches.getCache(SinkitCacheName.infinispan_rules.toString());
            final QueryFactory qf = Search.getQueryFactory(ruleCache);
            Query query = qf.from(Rule.class)
                    .having("customerId").eq(customerId)
                    .toBuilder().build();
            // TODO: Use CloseableIterator
            Iterator iterator = query.list().iterator();
            while (iterator.hasNext()) {
                ruleCache.remove(((Rule) iterator.next()).getStartAddress());
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
            final RemoteCache<String, Rule> ruleCache = cacheManagerForIndexableCaches.getCache(SinkitCacheName.infinispan_rules.toString());
            final ImmutablePair<String, String> startEndAddresses = CIDRUtils.getStartEndAddresses(cidrAddress);
            final String clientIPAddressPaddedBigInt = startEndAddresses.getLeft();
            log.log(Level.FINE, "Deleting key [" + cidrAddress + "] which actually translates to BigInteger zero padded representation " +
                    "[" + clientIPAddressPaddedBigInt + "]");
            String response;
            if (ruleCache.containsKey(clientIPAddressPaddedBigInt)) {
                ruleCache.remove(clientIPAddressPaddedBigInt);
                response = clientIPAddressPaddedBigInt + " DELETED";
            } else {
                response = clientIPAddressPaddedBigInt + " DOES NOT EXIST";
            }
            return response;
        } catch (Exception e) {
            log.log(Level.SEVERE, "deleteRule", e);
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
            final RemoteCache<String, Rule> ruleCache = cacheManagerForIndexableCaches.getCache(SinkitCacheName.infinispan_rules.toString());
            final QueryFactory qf = Search.getQueryFactory(ruleCache);
            final Query query = qf.from(Rule.class)
                    .having("customerId").eq(customerId)
                    .toBuilder().build();

            if (query != null && query.getResultSize() > 0) {
                // Remove all found rules
                query.list().forEach(rule -> ruleCache.remove(((Rule) rule).getStartAddress()));
            }

            for (String cidr : customerDNSSetting.keySet()) {
                final ImmutablePair<String, String> startEndAddresses = CIDRUtils.getStartEndAddresses(cidr);
                Rule rule = new Rule();
                rule.setCidrAddress(cidr);
                rule.setCustomerId(customerId);
                rule.setSources(customerDNSSetting.get(cidr));
                rule.setStartAddress(startEndAddresses.getLeft());
                rule.setEndAddress(startEndAddresses.getRight());
                log.log(Level.FINE, "Putting key [" + rule.getStartAddress() + "]");
                ruleCache.put(rule.getStartAddress(), rule);
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
        final RemoteCache<String, Rule> ruleCache = cacheManagerForIndexableCaches.getCache(SinkitCacheName.infinispan_rules.toString());
        for (AllDNSSettingDTO allDNSSettingDTO : allDNSSetting) {
            try {
                if (allDNSSettingDTO == null || allDNSSettingDTO.getDnsClient() == null || allDNSSettingDTO.getDnsClient().length() < 7) {
                    log.log(Level.SEVERE, "postAllDNSClientSettings: Got an invalid or null record. Can't process this.");
                    // TODO: Proper Error codes.
                    return null;
                }
                final ImmutablePair<String, String> startEndAddresses = CIDRUtils.getStartEndAddresses(allDNSSettingDTO.getDnsClient());
                final Rule rule = new Rule();
                rule.setCidrAddress(allDNSSettingDTO.getDnsClient());
                rule.setCustomerId(allDNSSettingDTO.getCustomerId());
                rule.setSources(allDNSSettingDTO.getSettings());
                rule.setStartAddress(startEndAddresses.getLeft());
                rule.setEndAddress(startEndAddresses.getRight());
                log.log(Level.FINE, "Putting key [" + rule.getStartAddress() + "]");
                ruleCache.put(rule.getStartAddress(), rule);
            } catch (Exception e) {
                log.log(Level.SEVERE, "postAllDNSClientSettings", e);
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
        final RemoteCache<String, CustomList> customListsCache = cacheManagerForIndexableCaches.getCache(SinkitCacheName.infinispan_custom_lists.toString());
        if (customerId == null || customerCustomLists == null) {
            log.log(Level.SEVERE, "putCustomLists: customerId and customerCustomLists cannot be null.");
            // TODO: Proper Error codes.
            return null;
        }
        log.log(Level.INFO, "putCustomLists: customerId: " + customerId + ", customerCustomLists.length: " + customerCustomLists.length);
        int customListsElementCounter = 0;
        // TODO: If customerCustomLists is empty - should we clear/delete all customerId's lists? Ask Rattus.
        // Yes, we should, see:
        // TODO: This is a very dangerous assumption: A single empty list causes not only the particular CIDR, but ALL customer's custom lists to disappear.
        //if (customerCustomLists.length == 0 || customerCustomLists[0].getLists().isEmpty()) {
        // We delete the whole setup before setting it up again. The biggest downside is that any any later error causes the setup to end up empty.
        final QueryFactory qf = Search.getQueryFactory(customListsCache);
        final Query query = qf.from(CustomList.class)
                .having("customerId").eq(customerId)
                .toBuilder().build();
        final List<CustomList> result = query.list();
        log.log(Level.FINE, "putCustomLists: customerId: " + customerId + " yielded " + result.size() + "results in search.");

        // TODO cleanup, unnecessarily clumsy
        final Set<String> toBeAdded = new HashSet<>();
        Stream.of(customerCustomLists).forEach(x -> toBeAdded.addAll(x.getLists().keySet()));
        // The point is to remove from cache what is not in the updated set
        result.stream().filter(x -> !toBeAdded.contains(((x.getFqdn() != null) ? x.getFqdn() : x.getListCidrAddress()))).forEach(r -> {
            final String key = r.getClientCidrAddress() + ((r.getFqdn() != null) ? r.getFqdn() : r.getListCidrAddress());
            log.log(Level.INFO, "putCustomLists: removing key " + key);
            customListsCache.remove(key);
        });

        //} else {
        final DomainValidator domainValidator = DomainValidator.getInstance();
        String dnsClientStartAddress;
        String dnsClientEndAddress;
        for (CustomerCustomListDTO customerCustomList : customerCustomLists) {
            // Let's calculate DNS Client address
            try {
                if (StringUtils.isEmpty(customerCustomList.getDnsClient())) {
                    log.log(Level.SEVERE, "putCustomLists: DNS client CIDR was null: Lists: " + customerCustomList.getLists().toString());
                    return "putCustomLists: DNS client CIDR was null: Lists: " + customerCustomList.getLists().toString();
                }
                final ImmutablePair<String, String> startEndAddresses = CIDRUtils.getStartEndAddresses(customerCustomList.getDnsClient());
                dnsClientStartAddress = startEndAddresses.getLeft();
                dnsClientEndAddress = startEndAddresses.getRight();
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
            }

            // Let's process list of Blacklisted / Whitelisted / Logged CIDRs and FQDNs.
            for (String fqdnOrCIDR : customerCustomList.getLists().keySet()) {
                // TODO: OMG, this should go to some CustomList factory.
                final CustomList customList = new CustomList();
                customList.setCustomerId(customerId);
                customList.setClientCidrAddress(customerCustomList.getDnsClient());
                customList.setClientStartAddress(dnsClientStartAddress);
                customList.setClientEndAddress(dnsClientEndAddress);
                final String blacklistWhitelistLog = customerCustomList.getLists().get(fqdnOrCIDR);
                if (!(blacklistWhitelistLog != null && ("B".equals(blacklistWhitelistLog) || "W".equals(blacklistWhitelistLog) || "L".equals(blacklistWhitelistLog)))) {
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
                        final ImmutablePair<String, String> startEndAddresses = CIDRUtils.getStartEndAddresses(fqdnOrCIDR);
                        final String startAddress = startEndAddresses.getLeft();
                        final String endAddress = startEndAddresses.getRight();
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
                    log.log(Level.INFO, "pcustomListsCacheutCustomLists: Putting key [" + key + "]. customListsElementCounter: " + customListsElementCounter);
                    // TODO: This is redundant now...
                    //if (customListsCache.replace(key, customList) == null) {
                    customListsCache.put(key, customList);
                    //}
                    customListsElementCounter++;
                } catch (Exception e) {
                    log.log(Level.SEVERE, "putCustomLists: customListsElementCounter: " + customListsElementCounter, e);
                    // TODO: Proper Error codes.
                    return null;
                }
            }
            //}
        }
        return customListsElementCounter + " CUSTOM LISTS ELEMENTS PROCESSED, " + customListsCache.size() + " PRESENT" + customListsCache.keySet().toString();
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
        Query query;
        int updated = 0;
        try {
            final RemoteCache<String, Rule> ruleCache = cacheManagerForIndexableCaches.getCache(SinkitCacheName.infinispan_rules.toString());
            final QueryFactory qf = Search.getQueryFactory(ruleCache);
            query = qf.from(Rule.class)
                    .having("sources.feedUid").eq(feedUid)
                    .toBuilder().build();

            if (query != null && query.getResultSize() > 0) {
                List<Rule> list = query.list();
                for (Rule rule : list) {
                    HashMap<String, String> cidrMode = feedSettings.get(rule.getCustomerId());
                    if (cidrMode != null && cidrMode.containsKey(rule.getCidrAddress())) {
                        //TODO This is certainly wrong and overengineered... Let's talk to Rattus.
                        rule.getSources().replace(feedUid, cidrMode.get(rule.getCidrAddress()));
                        try {
                            ruleCache.replace(rule.getStartAddress(), rule);
                            updated++;
                        } catch (Exception e) {
                            log.log(Level.SEVERE, "putFeedSettings", e);
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
        throw new NotImplementedException("We are sorry, this is not suported at the moment.");
    }

    @Override
    public ResolverConfiguration putResolverConfiguration(ResolverConfiguration configuration) throws ResolverConfigurationValidationException {
        resolverConfigurationValidator.validate(configuration);
        return resolverConfigurationCache.put(configuration.getResolverId(), configuration);
    }

    @Override
    public ResolverConfiguration getResolverConfiguration(int resolverId) {
        return resolverConfigurationCache.get(resolverId);
    }

    @Override
    public ResolverConfiguration deleteResolverConfiguration(int resolverId) {
        return resolverConfigurationCache.remove(resolverId);
    }

    @Override
    public List<ResolverConfiguration> getAllResolverConfigurations() {
        final RemoteCache<Integer, ResolverConfiguration> resolverConfigurationCache = cacheManagerForIndexableCaches.getCache(SinkitCacheName.resolver_configuration
                .name());
        final QueryFactory qf = Search.getQueryFactory(resolverConfigurationCache);
        final Query query = qf.from(ResolverConfiguration.class).build();
        return query.list();
    }

    @Override
    public EndUserConfiguration putEndUserConfiguration(EndUserConfiguration configuration) throws EndUserConfigurationValidationException {
        endUserConfigurationValidator.validate(configuration);
        return endUserConfigurationCache.put(configuration.getId(), configuration);
    }

    @Override
    public EndUserConfiguration getEndUserConfiguration(String id) {
        return endUserConfigurationCache.get(id);
    }

    @Override
    public EndUserConfiguration deleteEndUserConfiguration(String id) {
        return endUserConfigurationCache.remove(id);
    }

    @Override
    public List<EndUserConfiguration> getAllEndUserConfigurations() {
        final RemoteCache<String, EndUserConfiguration> endUserConfigurationRemoteCache = cacheManagerForIndexableCaches.getCache(SinkitCacheName.end_user_configuration
                .name());
        final QueryFactory qf = Search.getQueryFactory(endUserConfigurationRemoteCache);
        final Query query = qf.from(EndUserConfiguration.class).build();
        return query.list();
    }
}
