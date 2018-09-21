package biz.karms.sinkit.ejb.impl;

import biz.karms.sinkit.ejb.CoreService;
import biz.karms.sinkit.ejb.DNSApi;
import biz.karms.sinkit.ejb.ThreatType;
import biz.karms.sinkit.ejb.WebApi;
import biz.karms.sinkit.ejb.cache.annotations.SinkitCache;
import biz.karms.sinkit.ejb.cache.annotations.SinkitCacheName;
import biz.karms.sinkit.ejb.cache.pojo.BlacklistedRecord;
import biz.karms.sinkit.ejb.cache.pojo.CustomList;
import biz.karms.sinkit.ejb.cache.pojo.Rule;
import biz.karms.sinkit.ejb.dto.Sinkhole;
import biz.karms.sinkit.ejb.util.CIDRUtils;
import biz.karms.sinkit.ejb.util.IPorFQDNValidator;
import biz.karms.sinkit.ejb.util.WhitelistUtils;
import biz.karms.sinkit.eventlog.EventLogAction;
import biz.karms.sinkit.exception.ArchiveException;
import com.google.gson.Gson;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.Search;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Michal Karm Babacek
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DNSApiEJB implements DNSApi {

    @Inject
    private Logger log;

    @EJB
    private CoreService coreService;

    @EJB
    private WebApi webApi;

    @EJB
    private DNSApiLoggingEJB dnsApiLoggingEJB;

    @Inject
    @SinkitCache(SinkitCacheName.infinispan_blacklist)
    private RemoteCache<String, BlacklistedRecord> blacklistCache;

    @Inject
    @SinkitCache(SinkitCacheName.cache_manager_indexable)
    private RemoteCacheManager cacheManagerForIndexableCaches;

    @Inject
    @SinkitCache(SinkitCacheName.rules_local_cache)
    private BasicCache<String, List<Rule>> ruleLocalCache;

    @Inject
    @SinkitCache(SinkitCacheName.custom_lists_local_cache)
    private BasicCache<String, List<CustomList>> customListsLocalCache;

    private static final String IPV6SINKHOLE = System.getenv("SINKIT_SINKHOLE_IPV6");
    private static final String IPV4SINKHOLE = System.getenv("SINKIT_SINKHOLE_IP");
    public static final String CUSTOM_LIST_FEED_NAME = (System.getenv().containsKey("SINKIT_CUSTOM_LIST_FEED_NAME")) ? System.getenv("SINKIT_CUSTOM_LIST_FEED_NAME") : "custom-list";
    private static final Map<String, Set<ImmutablePair<String, String>>> customListfeedTypeMap = new HashMap<String, Set<ImmutablePair<String, String>>>() {
        {
            put(CUSTOM_LIST_FEED_NAME, new HashSet<ImmutablePair<String, String>>() {
                {
                    add(new ImmutablePair<>(CUSTOM_LIST_FEED_NAME, null));
                }
            });
        }
    };
    private static final boolean DNS_REQUEST_LOGGING_ENABLED = Boolean.parseBoolean((System.getenv().containsKey("SINKIT_DNS_REQUEST_LOGGING_ENABLED")) ? System.getenv("SINKIT_DNS_REQUEST_LOGGING_ENABLED") : "true");

    /**
     * There are 3 ways to find the result, in ascending order by their cost:
     * 1. local cache of already found results based on clientIPAddressPaddedBigInt
     * 2. getting key clientIPAddressPaddedBigInt from the cache of Rules
     * 3. lookup in the cache of Rules based on subnets
     * <p>
     * TODO: List? Array? Map with additional data? Let's think this over.
     * TODO: Replace/factor out duplicated code in .getRules out of webApiEJB
     *
     * @param clientIPAddressPaddedBigInt
     * @return list of rules
     */
    private List<Rule> rulesLookup(final String clientIPAddressPaddedBigInt, final RemoteCache<String, Rule> ruleCache) {
        try {
            log.log(Level.FINE, "Getting key BigInteger zero padded representation " + clientIPAddressPaddedBigInt);
            // Let's search subnets
            final String keyInCache = DigestUtils.md5Hex(clientIPAddressPaddedBigInt + clientIPAddressPaddedBigInt);
            log.log(Level.FINE, "keyInCache: " + keyInCache + ", from: " + (clientIPAddressPaddedBigInt + clientIPAddressPaddedBigInt));

            final List<Rule> cached = ruleLocalCache.get(keyInCache);
            if (cached != null) {
                return cached;
            } else {
                // Let's try to hit it
                final Rule rule = ruleCache.withFlags(Flag.SKIP_CACHE_LOAD).get(clientIPAddressPaddedBigInt);
                if (rule != null) {
                    return Collections.singletonList(rule);
                }
                final QueryFactory qf = Search.getQueryFactory(ruleCache);
                final Query query = qf.from(Rule.class)
                        .having("startAddress").lte(clientIPAddressPaddedBigInt)
                        .and()
                        .having("endAddress").gte(clientIPAddressPaddedBigInt)
                        .toBuilder().build();
                if (query != null) {
                    final List<Rule> result = query.list();
                    ruleLocalCache.put(keyInCache, result);
                    return result;

                }
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "getRules client address troubles", e);
            return null;
        }
    }

    /**
     * There are 2 ways to find the result, in ascending order by their cost:
     * 1. local cache of already found results based on customerId
     * 2. lookup in the cache of Rules based on customerId
     * <p>
     * TODO: List? Array? Map with additional data? Let's think this over.
     * TODO: Replace/factor out duplicated code in .getRules out of webApiEJB
     *
     * @param customerId customer ID passed on from Resolver
     * @return list of rules
     */
    private List<Rule> rulesLookup(final Integer customerId, final RemoteCache<String, Rule> ruleCache) {
        try {
            log.log(Level.FINE, "Getting key for customerId " + customerId);
            /* TODO: Could we have a collision between MD5 hash of customer (client) ID and an MD5 hash sum of clientIPAddressPaddedBigInt + clientIPAddressPaddedBigInt ? */
            final String keyInCache = DigestUtils.md5Hex(customerId.toString());
            log.log(Level.FINE, "keyInCache: " + keyInCache + ", from: " + customerId);

            final List<Rule> cached = ruleLocalCache.get(keyInCache);
            if (cached != null) {
                return cached;
            } else {
                final QueryFactory qf = Search.getQueryFactory(ruleCache);
                final Query query = qf.from(Rule.class)
                        .having("customerId").eq(customerId)
                        .toBuilder().build();
                if (query != null) {
                    final List<Rule> result = query.list();
                    ruleLocalCache.put(keyInCache, result);
                    return result;

                }
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "getRules client customerId troubles", e);
            return null;
        }
    }

    private List<CustomList> customListsLookup(final Integer customerId, final IPorFQDNValidator.DECISION isFQDN, final String fqdnOrIp) {
        final QueryFactory qf = Search.getQueryFactory(cacheManagerForIndexableCaches.getCache(SinkitCacheName.infinispan_custom_lists.toString()));
        Query query;
        if (isFQDN == IPorFQDNValidator.DECISION.FQDN) {

            final String keyInCache = DigestUtils.md5Hex(customerId + fqdnOrIp);
            log.log(Level.FINE, "keyInCache: " + keyInCache + ", from: " + (customerId + fqdnOrIp));

            final List<CustomList> cached = customListsLocalCache.get(keyInCache);
            if (cached != null) {
                return cached;
            } else {
                query = qf.from(CustomList.class)
                        .having("customerId").eq(customerId)
                        .and()
                        .having("fqdn").like("%" + fqdnOrIp + "%")
                        .toBuilder().build();
                if (query != null) {
                    final List<CustomList> result = query.list();
                    customListsLocalCache.put(keyInCache, result);
                    return result;
                }
                return null;
            }
        } else if (isFQDN == IPorFQDNValidator.DECISION.IP) {
            final String clientIPAddressPaddedBigInt;
            try {
                clientIPAddressPaddedBigInt = CIDRUtils.getStartEndAddresses(fqdnOrIp).getLeft();
            } catch (UnknownHostException e) {
                log.log(Level.FINE, "customListsLookup: " + fqdnOrIp + " in not a valid IP address nor a valid FQDN.");
                return null;
            }

            final String keyInCache = DigestUtils.md5Hex(customerId + clientIPAddressPaddedBigInt + clientIPAddressPaddedBigInt);
            log.log(Level.FINE, "keyInCache: " + keyInCache + ", from: " + (customerId + clientIPAddressPaddedBigInt + clientIPAddressPaddedBigInt));

            final List<CustomList> cached = customListsLocalCache.get(keyInCache);
            if (cached != null) {
                return cached;
            } else {
                query = qf.from(CustomList.class)
                        .having("customerId").eq(customerId)
                        .and()
                        .having("listStartAddress").lte(clientIPAddressPaddedBigInt)
                        .and()
                        .having("listEndAddress").gte(clientIPAddressPaddedBigInt)
                        .toBuilder().build();
                if (query != null) {
                    final List<CustomList> result = query.list();
                    customListsLocalCache.put(keyInCache, result);
                    return result;
                }
                return null;
            }
        } else {
            log.log(Level.SEVERE, "Unexpected data, should have been IPorFQDNValidator.DECISION.IP or IPorFQDNValidator.DECISION.FQDN");
            return null;
        }
    }

    private CustomList retrieveOneCustomList(final Integer customerId, final IPorFQDNValidator.DECISION isFQDN, final String fqdnOrIp) {
        //TODO logic about B|W lists
        //TODO logic about *something.foo.com being less important then bar.something.foo.com
        //TODO This is just a stupid dummy/mock
        final List<CustomList> customLists = customListsLookup(customerId, isFQDN, fqdnOrIp);
        return (CollectionUtils.isEmpty(customLists)) ? null : customLists.get(0);
    }

    /**
     * Sinkhole, to be called by DNS client.
     *
     * @param clientIPAddress        - DNS server IP
     * @param fqdnOrIpRaw            - FQDN DNS is trying to resolve or resolved IP (v6 or v4)
     * @param fqdn                   - FQDN original DNS query
     * @param customerIdFromResolver - customerId, if resolver knows it
     * @return null if there is an error and/or there is no reason to sinkhole or Sinkhole instance on positive hit
     */
    @Override
    public Sinkhole getSinkHole(final String clientIPAddress, final String fqdnOrIpRaw, final String fqdn, final Integer customerIdFromResolver) {

        // To determine whether key is a FQDN or an IP address
        final ImmutablePair<IPorFQDNValidator.DECISION, String> fqdnOrIpProcessed = IPorFQDNValidator.decide(fqdnOrIpRaw);
        final IPorFQDNValidator.DECISION isFQDN = fqdnOrIpProcessed.getLeft();
        final String fqdnOrIp = fqdnOrIpProcessed.getRight();

        if (isFQDN == IPorFQDNValidator.DECISION.GARBAGE) {
            //TODO we might have to revisit this. @See IPorFQDNValidator
            return null;
        }

        // Used for benchmarking
        long start;

        final List<Rule> rules;

        final RemoteCache<String, Rule> rulesCache = cacheManagerForIndexableCaches.getCache(SinkitCacheName.infinispan_rules.toString());
        // If we have customerId from Resolver, we don't need to search based on clientIPAddress.
        if (customerIdFromResolver == null || customerIdFromResolver < 0) {
            // At first, we lookup Rule
            final String clientIPAddressPaddedBigInt;
            try {
                clientIPAddressPaddedBigInt = CIDRUtils.getStartEndAddresses(clientIPAddress).getLeft();
            } catch (UnknownHostException e) {
                log.log(Level.SEVERE, "getSinkHole: clientIPAddress " + clientIPAddress + " in not a valid address.");
                return null;
            }
            start = System.currentTimeMillis();
            // Lookup Rules (gives customerId, feeds and their settings)
            //TODO: Add test that all such found rules have the same customerId
            //TODO: factor .getRules out of webApiEJB
            rules = rulesLookup(clientIPAddressPaddedBigInt, rulesCache);
            log.log(Level.FINE, "rulesLookup took: " + (System.currentTimeMillis() - start) + " ms.");
        } else {
            rules = rulesLookup(customerIdFromResolver, rulesCache);
        }

        //If there is no rule, we simply don't sinkhole anything.
        if (CollectionUtils.isEmpty(rules)) {
            //TODO: Distinguish this from an error state.
            return null;
        }

        //TODO: regarding get(0): Solve overlapping customer settings.
        // Customer ID for this whole method context
        final int customerId = rules.get(0).getCustomerId();

        // Sanity check. If we have a customer ID from resolver, it must be the same as the one we found.
        if ((customerIdFromResolver != null && customerIdFromResolver > 0) && (customerId != customerIdFromResolver)) {
            log.log(Level.SEVERE, "customerIdFromResolver is " + customerIdFromResolver + " and customerId from rules is "
                    + customerId + ", they MUST be the same. Returning null without further processing.");
            return null;
        }

        // Next we fetch one and only one or none CustomList for a given fqdnOrIp
        start = System.currentTimeMillis();
        final CustomList customList = retrieveOneCustomList(customerId, isFQDN, fqdnOrIp);


        log.log(Level.FINE, "retrieveOneCustomList took: " + (System.currentTimeMillis() - start) + " ms.");
        final boolean probablyIsIPv6 = fqdnOrIp.contains(":");
        // Was it found in any of customer's Black/White/Log lists?
        // TODO: Implement logging for whitelisted stuff that's positive on IoC.
        if (customList != null) {
            // Whitelisted
            if ("W".equals(customList.getWhiteBlackLog())) {
                //TODO: Distinguish this from an error state.
                return null;
                //Blacklisted
            } else if ("B".equals(customList.getWhiteBlackLog())) {
                try {
                    if (DNS_REQUEST_LOGGING_ENABLED) {
                        dnsApiLoggingEJB.logDNSEvent(EventLogAction.BLOCK, String.valueOf(customerId), clientIPAddress, fqdn, null, (isFQDN == IPorFQDNValidator.DECISION.FQDN) ? fqdnOrIp : null,
                                (isFQDN == IPorFQDNValidator.DECISION.FQDN) ? null : fqdnOrIp, customListfeedTypeMap, null, log);
                    }
                } catch (ArchiveException e) {
                    log.log(Level.SEVERE, "getSinkHole: Logging customer BLOCK failed: ", e);
                }
                return new Sinkhole(probablyIsIPv6 ? IPV6SINKHOLE : IPV4SINKHOLE);
            } else if ("L".equals(customList.getWhiteBlackLog())) {
                try {
                    if (DNS_REQUEST_LOGGING_ENABLED) {
                        dnsApiLoggingEJB.logDNSEvent(EventLogAction.AUDIT, String.valueOf(customerId), clientIPAddress, fqdn, null, (isFQDN == IPorFQDNValidator.DECISION.FQDN) ? fqdnOrIp : null,
                                (isFQDN == IPorFQDNValidator.DECISION.FQDN) ? null : fqdnOrIp, customListfeedTypeMap, null, log);
                    }
                } catch (ArchiveException e) {
                    log.log(Level.SEVERE, "getSinkHole: Logging customer LOG failed: ", e);
                }
            } else {
                log.log(Level.SEVERE, "getSinkHole: getWhiteBlackLog must be one of B, W, L but was: " + customList.getWhiteBlackLog());
                return null;
            }
        }

        // Now it's the time to search IoC cache
        // Lookup BlacklistedRecord from IoC cache (gives feeds)

        final String[] toBeChecked;
        if (isFQDN == IPorFQDNValidator.DECISION.IP) {
            toBeChecked = new String[]{fqdnOrIp};
        } else {
            toBeChecked = WhitelistUtils.explodeDomains(fqdnOrIp);
        }


        for (String subdomainOrIp : toBeChecked) {

            log.log(Level.FINE, "getSinkHole: getting IoC key " + subdomainOrIp + " from original request " + fqdnOrIp);
            start = System.currentTimeMillis();
            final BlacklistedRecord blacklistedRecord = blacklistCache.withFlags(Flag.SKIP_CACHE_LOAD).get(DigestUtils.md5Hex(subdomainOrIp));
            log.log(Level.FINE, "blacklistCache.get took: " + (System.currentTimeMillis() - start) + " ms.");

            if (blacklistedRecord == null) {
                log.log(Level.FINE, "No hit. The requested fqdnOrIp: " + subdomainOrIp + " is clean.");
                continue;
            }

            // Feed UID : [{Type1, IoCID1}, {Type2, IoCID2}, ...]
            final Map<String, Set<ImmutablePair<String, String>>> feedTypeMap = new HashMap<>();

            if (MapUtils.isNotEmpty(blacklistedRecord.getSources())) {
                // Once blacklisted source issue is fixed replace this for-loop for feedTypeMap.putAll(blacklistedRecord.getSources());
                Set<ImmutablePair<String, String>> blacklistedRecordSource;
                for (Map.Entry<String, ImmutablePair<String, String>> typeIoCImmutablePair : blacklistedRecord.getSources().entrySet()) {
                    blacklistedRecordSource = new HashSet<>();
                    blacklistedRecordSource.add(typeIoCImmutablePair.getValue());
                    feedTypeMap.put(typeIoCImmutablePair.getKey(), blacklistedRecordSource);
                }
            }

            //If there is no feed, we simply don't sinkhole anything. It is weird though.
            if (MapUtils.isEmpty(feedTypeMap)) {
                log.log(Level.WARNING, "getSinkHole: IoC without feed settings.");
                return null;
            }

            // Feed UID, Mode <L|S|D>. In the end, we operate on a one selected Feed:mode ImmutablePair only.
            String mode = null;
            //TODO: Nested cycles, could we do better here?
            for (Rule rule : rules) {
                log.log(Level.FINE, "FEED_DECISION: Processing rule " + rule);
                for (String uuid : rule.getSources().keySet()) {
                    Set<ImmutablePair<String, String>> typeDocIds = feedTypeMap.get(uuid);
                    if (typeDocIds == null) {
                        continue;
                    }
                    log.log(Level.FINE, "FEED_DECISION: Processing uuid " + uuid);
                    // feed uuid : [{type , docID}, {type2, docID2}, ...] so "getA()" means get type
                    for (ImmutablePair<String, String> typeDocId : typeDocIds) {
                        if (typeDocId != null && ThreatType.parseName(typeDocId.getLeft()) != null) {
                            String tmpMode = rule.getSources().get(uuid);
                            if (mode == null || ("L".equals(tmpMode) && !"S".equals(mode)) || "S".equals(tmpMode)) {
                                //S >= L >= D >= null, i.e. if a feed is Disabled, we don't switch to Sinkhole.
                                log.log(Level.FINE, "FEED_DECISION: mode " + tmpMode + " DOES TAKE priority over mode " + mode);
                                mode = tmpMode;
                            } else {
                                log.log(Level.FINE, "FEED_DECISION: mode " + tmpMode + " DOES NOT take priority over mode " + mode);
                            }
                        } else {
                            log.log(Level.FINE, "getSinkHole: BlacklistedRecord " + subdomainOrIp + " from original request " + fqdnOrIp + " for feed " + uuid + " does not have Type nor DocID.");
                        }
                    }
                    log.log(Level.FINE, "FEED_DECISION: Final decision for uuid " + uuid + " is: " + mode);
                }
                log.log(Level.FINE, "FEED_DECISION: Final decision for rule " + rule + " is: " + mode);
            }
            log.log(Level.FINE, "FEED_DECISION: Final decision overall is: " + mode);

            final Map.Entry<String, HashMap<String, Integer>> theMostAccurateFeed;
            if (blacklistedRecord.getAccuracy() != null) {
                // The sum of all accuracies of all feeds. We don't want that.
                // final Integer accuracy = blacklistedRecord.getAccuracy().values().stream().flatMap(x -> x.values().stream()).mapToInt(Integer::intValue).sum();
                // Compute maximum from each feed, not overall...
                final Optional<Map.Entry<String, HashMap<String, Integer>>> fdcmp = blacklistedRecord.getAccuracy().entrySet().stream()
                        .max(Comparator.comparingInt(s -> s.getValue().values().stream().mapToInt(Integer::intValue).sum()));
                if (fdcmp.isPresent()) {
                    theMostAccurateFeed = fdcmp.get();
                } else {
                    theMostAccurateFeed = null;
                }

                // Whitelisted?
                if (blacklistedRecord.getPresentOnWhiteList()) {
                    // This is very fishy. We should perhaps add a new state to @see EventLogAction enum...
                    mode = "D";
                }
            } else {
                // We don't work with accuracy at all.
                theMostAccurateFeed = null;
            }

            // Let's decide on feed mode:
            if (mode == null) {
                //TODO: Distinguish this from an error state.
                log.log(Level.FINE, "getSinkHole: No match, no feed settings, we don't sinkhole.");
                return null;
            } else if ("S".equals(mode)) {
                log.log(Level.INFO, "getSinkHole: Sinkhole. The most accurate feed: " + new Gson().toJson(theMostAccurateFeed));
                try {
                    log.log(Level.FINE, "getSinkHole: Calling coreService.logDNSEvent(EventLogAction.BLOCK,...");
                    if (DNS_REQUEST_LOGGING_ENABLED) {
                        dnsApiLoggingEJB.logDNSEvent(EventLogAction.BLOCK, String.valueOf(customerId), clientIPAddress, fqdn, null,
                                (isFQDN == IPorFQDNValidator.DECISION.FQDN) ? subdomainOrIp : null,
                                (isFQDN == IPorFQDNValidator.DECISION.FQDN) ? null : subdomainOrIp,
                                feedTypeMap, theMostAccurateFeed, log);
                    }
                    log.log(Level.FINE, "getSinkHole: coreService.logDNSEvent returned.");
                } catch (ArchiveException e) {
                    log.log(Level.SEVERE, "getSinkHole: Logging BLOCK failed: ", e);
                }
                return new Sinkhole(probablyIsIPv6 ? IPV6SINKHOLE : IPV4SINKHOLE);
            } else if ("L".equals(mode)) {
                //Log it for customer
                log.log(Level.INFO, "getSinkHole: Audit. The most accurate feed: " + new Gson().toJson(theMostAccurateFeed));
                try {
                    if (DNS_REQUEST_LOGGING_ENABLED) {
                        dnsApiLoggingEJB.logDNSEvent(EventLogAction.AUDIT, String.valueOf(customerId), clientIPAddress, fqdn, null,
                                (isFQDN == IPorFQDNValidator.DECISION.FQDN) ? subdomainOrIp : null,
                                (isFQDN == IPorFQDNValidator.DECISION.FQDN) ? null : subdomainOrIp,
                                feedTypeMap, theMostAccurateFeed, log);
                    }
                } catch (ArchiveException e) {
                    log.log(Level.SEVERE, "getSinkHole: Logging AUDIT failed: ", e);
                }
                //TODO: Distinguish this from an error state.
                return null;
            } else if ("D".equals(mode)) {
                //Log it for us
                log.log(Level.INFO, "getSinkHole: Log internally. The most accurate feed: " + new Gson().toJson(theMostAccurateFeed));
                try {
                    if (DNS_REQUEST_LOGGING_ENABLED) {
                        dnsApiLoggingEJB.logDNSEvent(EventLogAction.INTERNAL, String.valueOf(customerId), clientIPAddress, fqdn, null,
                                (isFQDN == IPorFQDNValidator.DECISION.FQDN) ? subdomainOrIp : null,
                                (isFQDN == IPorFQDNValidator.DECISION.FQDN) ? null : subdomainOrIp,
                                feedTypeMap, theMostAccurateFeed, log);
                    }
                } catch (ArchiveException e) {
                    log.log(Level.SEVERE, "getSinkHole: Logging INTERNAL failed: ", e);
                }
                // TODO: Distinguish this from an error state.
                return null;
            } else {
                log.log(Level.SEVERE, "getSinkHole: Feed mode must be one of L,S,D, null but was: " + mode);
                return null;
            }
        }
        return null;
    }

    // TODO: Too complicated, OMG
    private static List<String> explodeDomain(final String fqdn) {
        final String[] subs = fqdn.split("\\.");
        final List<String> result = new ArrayList<>(subs.length);
        if (subs.length > 2) {
            final StringBuilder sb = new StringBuilder(fqdn.length());
            for (int i = subs.length - 2; i >= 0; i--) {
                sb.setLength(fqdn.length());
                for (int j = i; j < subs.length - 1; j++) {
                    sb.append(subs[j]);
                    if (j + 1 <= subs.length - 1 || j == i) {
                        sb.append(".");
                    }
                }
                sb.append(subs[subs.length - 1]);
                result.add(sb.toString());
                sb.setLength(0);
            }
            return result;
        }
        return Collections.singletonList(fqdn);
    }
}
