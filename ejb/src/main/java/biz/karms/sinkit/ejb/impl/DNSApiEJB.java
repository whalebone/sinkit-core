package biz.karms.sinkit.ejb.impl;

import biz.karms.sinkit.ejb.ArchiveService;
import biz.karms.sinkit.ejb.CoreService;
import biz.karms.sinkit.ejb.DNSApi;
import biz.karms.sinkit.ejb.GSBService;
import biz.karms.sinkit.ejb.ThreatType;
import biz.karms.sinkit.ejb.WebApi;
import biz.karms.sinkit.ejb.cache.annotations.SinkitCache;
import biz.karms.sinkit.ejb.cache.annotations.SinkitCacheName;
import biz.karms.sinkit.ejb.cache.pojo.BlacklistedRecord;
import biz.karms.sinkit.ejb.cache.pojo.CustomList;
import biz.karms.sinkit.ejb.cache.pojo.Rule;
import biz.karms.sinkit.ejb.dto.Sinkhole;
import biz.karms.sinkit.ejb.elastic.logstash.LogstashClient;
import biz.karms.sinkit.ejb.util.CIDRUtils;
import biz.karms.sinkit.ejb.util.IPorFQDNValidator;
import biz.karms.sinkit.ejb.util.IoCIdentificationUtils;
import biz.karms.sinkit.eventlog.EventDNSRequest;
import biz.karms.sinkit.eventlog.EventLogAction;
import biz.karms.sinkit.eventlog.EventLogRecord;
import biz.karms.sinkit.eventlog.EventReason;
import biz.karms.sinkit.eventlog.VirusTotalRequest;
import biz.karms.sinkit.eventlog.VirusTotalRequestStatus;
import biz.karms.sinkit.exception.ArchiveException;
import biz.karms.sinkit.exception.IoCSourceIdException;
import biz.karms.sinkit.ioc.IoCClassification;
import biz.karms.sinkit.ioc.IoCFeed;
import biz.karms.sinkit.ioc.IoCRecord;
import biz.karms.sinkit.ioc.IoCSeen;
import biz.karms.sinkit.ioc.IoCSource;
import biz.karms.sinkit.ioc.IoCSourceId;
import biz.karms.sinkit.ioc.IoCTime;
import biz.karms.sinkit.ioc.util.IoCSourceIdBuilder;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.Search;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;

import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    private ArchiveService archiveService;

    @EJB //TODO: https://github.com/whalebone/sinkit-core/issues/81
    private GSBService gsbService;

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
    private static final String GSB_FEED_NAME = (System.getenv().containsKey("SINKIT_GSB_FEED_NAME")) ? System.getenv("SINKIT_GSB_FEED_NAME") : "google-safebrowsing-api";
    private static final String CUSTOM_LIST_FEED_NAME = (System.getenv().containsKey("SINKIT_CUSTOM_LIST_FEED_NAME")) ? System.getenv("SINKIT_CUSTOM_LIST_FEED_NAME") : "custom-list";
    private static final Map<String, Set<ImmutablePair<String, String>>> customListfeedTypeMap = new HashMap<String, Set<ImmutablePair<String, String>>>() {
        {
            put(CUSTOM_LIST_FEED_NAME, new HashSet<ImmutablePair<String, String>>() {
                {
                    add(new ImmutablePair<>(CUSTOM_LIST_FEED_NAME, null));
                }
            });
        }
    };
    private static final boolean USE_LOGSTASH = StringUtils.isNotBlank(System.getenv(LogstashClient.LOGSTASH_URL_ENV));
    private static final int ARCHIVE_FAILED_TRIALS = 10;
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
                        logDNSEvent(EventLogAction.BLOCK, String.valueOf(customerId), clientIPAddress, fqdn, null, (isFQDN == IPorFQDNValidator.DECISION.FQDN) ? fqdnOrIp : null,
                                (isFQDN == IPorFQDNValidator.DECISION.FQDN) ? null : fqdnOrIp, customListfeedTypeMap, archiveService, log);
                    }
                } catch (ArchiveException e) {
                    log.log(Level.SEVERE, "getSinkHole: Logging customer BLOCK failed: ", e);
                }
                return new Sinkhole(probablyIsIPv6 ? IPV6SINKHOLE : IPV4SINKHOLE);
            } else if ("L".equals(customList.getWhiteBlackLog())) {
                try {
                    if (DNS_REQUEST_LOGGING_ENABLED) {
                        logDNSEvent(EventLogAction.AUDIT, String.valueOf(customerId), clientIPAddress, fqdn, null, (isFQDN == IPorFQDNValidator.DECISION.FQDN) ? fqdnOrIp : null,
                                (isFQDN == IPorFQDNValidator.DECISION.FQDN) ? null : fqdnOrIp, customListfeedTypeMap, archiveService, log);
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
        log.log(Level.FINE, "getSinkHole: getting IoC key " + fqdnOrIp);
        start = System.currentTimeMillis();
        final BlacklistedRecord blacklistedRecord = blacklistCache.withFlags(Flag.SKIP_CACHE_LOAD).get(DigestUtils.md5Hex(fqdnOrIp));
        log.log(Level.FINE, "blacklistCache.get took: " + (System.currentTimeMillis() - start) + " ms.");

        Set<ThreatType> gsbResults = null;
        if (isFQDN == IPorFQDNValidator.DECISION.FQDN) {
            // Let's search GSB cache
            log.log(Level.FINE, "getSinkHole: getting GSB key " + fqdnOrIp);
            start = System.currentTimeMillis();
            gsbResults = gsbService.lookup(fqdnOrIp);
            log.log(Level.FINE, "gsbService.lookup took: " + (System.currentTimeMillis() - start) + " ms.");
        }

        if (blacklistedRecord == null && CollectionUtils.isEmpty(gsbResults)) {
            log.log(Level.FINE, "No hit. The requested fqdnOrIp: " + fqdnOrIp + " is clean.");
            return null;
        }

        // Feed UID : [{Type1, IoCID1}, {Type2, IoCID2}, ...]
        final Map<String, Set<ImmutablePair<String, String>>> feedTypeMap = new HashMap<>();

        if (blacklistedRecord != null && MapUtils.isNotEmpty(blacklistedRecord.getSources())) {
            // Once blacklisted source issue is fixed replace this for-loop for feedTypeMap.putAll(blacklistedRecord.getSources());
            Set<ImmutablePair<String, String>> blacklistedRecordSource;
            for (Map.Entry<String, ImmutablePair<String, String>> typeIoCImmutablePair : blacklistedRecord.getSources().entrySet()) {
                blacklistedRecordSource = new HashSet<>();
                blacklistedRecordSource.add(typeIoCImmutablePair.getValue());
                feedTypeMap.put(typeIoCImmutablePair.getKey(), blacklistedRecordSource);
            }
        }

        if (CollectionUtils.isNotEmpty(gsbResults)) {
            log.log(Level.FINE, "getSinkHole: gsbResults contains records: " + gsbResults.size());
            // GSB_IOC_DOES_NOT_EXIST - the IoC doesn't exist at this time. It will be created at Logging time.
            Set<ImmutablePair<String, String>> gsbTypes = new HashSet<>(gsbResults.size());
            for (ThreatType gsbThreatType : gsbResults) {
                gsbTypes.add(new ImmutablePair<>(gsbThreatType.getName(), null));
            }
            feedTypeMap.put(GSB_FEED_NAME, gsbTypes);
        } else {
            log.log(Level.FINE, "getSinkHole: gsbResults contains no record");
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
            for (String uuid : rule.getSources().keySet()) {
                Set<ImmutablePair<String, String>> typeDocIds = feedTypeMap.get(uuid);
                if (typeDocIds == null) {
                    continue;
                }
                // feed uuid : [{type , docID}, {type2, docID2}, ...] so "getA()" means get type
                for (ImmutablePair<String, String> typeDocId : typeDocIds) {
                    if (typeDocId != null && ThreatType.parseName(typeDocId.getLeft()) != null) {
                        String tmpMode = rule.getSources().get(uuid);
                        if (mode == null || ("S".equals(tmpMode) && !"D".equals(mode)) || "D".equals(tmpMode)) {
                            //D >= S >= L >= null, i.e. if a feed is Disabled, we don't switch to Sinkhole.
                            mode = tmpMode;
                        }
                    } else {
                        log.log(Level.FINE, "getSinkHole: BlacklistedRecord/GSB Rcord " + fqdnOrIp + " for feed " + uuid + " does not have Type nor DocID.");
                    }
                }
            }
        }

        log.log(Level.FINE, "getSinkHole: Feed mode decision:");
        // Let's decide on feed mode:
        if (mode == null) {
            //TODO: Distinguish this from an error state.
            log.log(Level.FINE, "getSinkHole: No match, no feed settings, we don't sinkhole.");
            return null;
        } else if ("S".equals(mode)) {
            log.log(Level.FINE, "getSinkHole: Sinkhole.");
            try {
                log.log(Level.FINE, "getSinkHole: Calling coreService.logDNSEvent(EventLogAction.BLOCK,...");
                if (DNS_REQUEST_LOGGING_ENABLED) {
                    logDNSEvent(EventLogAction.BLOCK, String.valueOf(customerId), clientIPAddress, fqdn, null, (isFQDN == IPorFQDNValidator.DECISION.FQDN) ? fqdnOrIp : null, (isFQDN == IPorFQDNValidator.DECISION.FQDN) ? null : fqdnOrIp, feedTypeMap, archiveService, log);
                }
                log.log(Level.FINE, "getSinkHole: coreService.logDNSEvent returned.");
            } catch (ArchiveException e) {
                log.log(Level.SEVERE, "getSinkHole: Logging BLOCK failed: ", e);
            }
            return new Sinkhole(probablyIsIPv6 ? IPV6SINKHOLE : IPV4SINKHOLE);
        } else if ("L".equals(mode)) {
            //Log it for customer
            log.log(Level.FINE, "getSinkHole: Log.");
            try {
                if (DNS_REQUEST_LOGGING_ENABLED) {
                    logDNSEvent(EventLogAction.AUDIT, String.valueOf(customerId), clientIPAddress, fqdn, null, (isFQDN == IPorFQDNValidator.DECISION.FQDN) ? fqdnOrIp : null, (isFQDN == IPorFQDNValidator.DECISION.FQDN) ? null : fqdnOrIp, feedTypeMap, archiveService, log);
                }
            } catch (ArchiveException e) {
                log.log(Level.SEVERE, "getSinkHole: Logging AUDIT failed: ", e);
            }
            //TODO: Distinguish this from an error state.
            return null;
        } else if ("D".equals(mode)) {
            //Log it for us
            log.log(Level.FINE, "getSinkHole: Log internally.");
            try {
                if (DNS_REQUEST_LOGGING_ENABLED) {
                    logDNSEvent(EventLogAction.INTERNAL, String.valueOf(customerId), clientIPAddress, fqdn, null, (isFQDN == IPorFQDNValidator.DECISION.FQDN) ? fqdnOrIp : null, (isFQDN == IPorFQDNValidator.DECISION.FQDN) ? null : fqdnOrIp, feedTypeMap, archiveService, log);
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

    /*private static Set<String> unwrapDocumentIds(Collection<ImmutablePair<String, String>> ImmutablePairs) {
        return ImmutablePairs.stream().map(ImmutablePair::getRight).collect(Collectors.toCollection(HashSet::new));
    }*/

    /**
     * Fire and Forget pattern
     * <p>
     * This is called only when there is a match for an IoC, it is not called
     * on each request.
     *
     * @param action      BLOCK|AUDIT|INTERNAL
     * @param clientUid   Client's UID
     * @param requestIp   Client's IP address
     * @param requestFqdn TFDN client wanted to have resolved by DNS
     * @param requestType DNS request type
     * @param reasonFqdn  IOC hit
     * @param reasonIp    IOC hit
     * @param matchedIoCs IoCs with the same feed listed
     * @throws ArchiveException When things go south.
     */
    @Asynchronous
    public void logDNSEvent(
            final EventLogAction action,
            final String clientUid,
            final String requestIp,
            final String requestFqdn,
            final String requestType,
            final String reasonFqdn,
            final String reasonIp,
            final Map<String, Set<ImmutablePair<String, String>>> matchedIoCs,
            final ArchiveService archiveService,
            final Logger log
    ) throws ArchiveException {
        log.log(Level.FINE, "Logging DNS event. clientUid: " + clientUid + ", requestIp: " + requestIp + ", requestFqdn: " + requestFqdn + ", requestType: " + requestType + ", reasonFqdn: " + reasonFqdn + ", reasonIp: " + reasonIp);
        final EventLogRecord logRecord = new EventLogRecord();

        final EventDNSRequest request = new EventDNSRequest();
        request.setIp(requestIp);
        request.setFqdn(requestFqdn);
        request.setType(requestType);
        logRecord.setRequest(request);

        final EventReason reason = new EventReason();
        reason.setIp(reasonIp);
        reason.setFqdn(reasonFqdn);
        logRecord.setReason(reason);

        logRecord.setAction(action);
        logRecord.setClient(clientUid);
        logRecord.setLogged(Calendar.getInstance().getTime());

        VirusTotalRequestStatus vtRequestStatus;
        if (MapUtils.isNotEmpty(matchedIoCs)) {
            final Set<IoCRecord> matchedIoCsList = new HashSet<>();
            log.log(Level.FINE, "Iterating matchedIoCs...");
            // { feed : [ type1 : iocId1, type2 : iocId2]}
            for (Map.Entry<String, Set<ImmutablePair<String, String>>> matchedIoC : matchedIoCs.entrySet()) {
                // feedName = matchedIoC.getKey();
                for (ImmutablePair<String, String> typeIoCID : matchedIoC.getValue()) {
                    // iocId = typeIoCID.getB();
                    // type = typeIoCID.getA();
                    //if (StringUtils.isNotBlank(typeIoCID.getB())) {
                    IoCRecord ioCRecord;
                    if (StringUtils.isBlank(typeIoCID.getRight()) && StringUtils.isNotBlank(reasonFqdn)) {
                        // Nope, no IP, just FQDN for GSB
                        ioCRecord = processNonExistingIoC(reasonFqdn, matchedIoC.getKey(), typeIoCID.getLeft(), archiveService, log);
                    } else {
                        ioCRecord = processRegularIoCId(typeIoCID.getRight(), archiveService, log);
                    }
                    if (ioCRecord != null) {
                        matchedIoCsList.add(ioCRecord);
                    }
                    //}
                }
            }
            final IoCRecord[] matchedIoCsArray = matchedIoCsList.toArray(new IoCRecord[matchedIoCsList.size()]);
            logRecord.setMatchedIocs(matchedIoCsArray);
            vtRequestStatus = VirusTotalRequestStatus.WAITING;
        } else {
            vtRequestStatus = VirusTotalRequestStatus.NOT_NEEDED;
        }

        final VirusTotalRequest vtReq = new VirusTotalRequest();
        vtReq.setStatus(vtRequestStatus);
        logRecord.setVirusTotalRequest(vtReq);

        Arrays.stream(logRecord.getMatchedIocs()).filter(x -> CUSTOM_LIST_FEED_NAME.equals(x.getFeed().getName())).forEach(x -> x.setUniqueRef(null));

        if (USE_LOGSTASH) {
            archiveService.archiveEventLogRecordUsingLogstash(logRecord);
        } else {
            archiveService.archiveEventLogRecord(logRecord);
        }
    }

    private static IoCRecord processRegularIoCId(final String iocId, ArchiveService archiveService, Logger log) throws ArchiveException {
        final IoCRecord ioc = archiveService.getIoCRecordById(iocId);
        if (ioc == null) {
            log.warning("Match IoC with id " + iocId + " was not found (deactivated??) -> skipping.");
            return null;
        }
        ioc.setVirusTotalReports(null);
        ioc.getSeen().setLast(null);
        ioc.setRaw(null);
        ioc.setActive(null);
        //documentId will changed whe ioc is deactivated so it's useless to have it here. Ioc.uniqueRef will be used for referencing ioc
        ioc.setDocumentId(null);
        return ioc;
    }

    /**
     * TODO: This implementation is a WIP.
     * The commented out code deals with remote logging to remote Core - Core machines.
     * For the time being, it is discarded due to cumbersomeness of the API with regard to DocumentID
     * and the fact that we will start having the keys in our IoC cache.
     * Furthermore, it might be a premature optimization.
     */
    private static IoCRecord processNonExistingIoC(final String matchedFQDN, String feedName, String type, ArchiveService archiveService, Logger log) {
        //HttpURLConnection coreServerConnection = null;
        //BufferedWriter jsonContentBuffer = null;
        //final Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

        final IoCRecord ioc = new IoCRecord();
        final IoCFeed feed = new IoCFeed();
        feed.setName(feedName);
        ioc.setFeed(feed);
        final IoCClassification classification = new IoCClassification();
        classification.setType(type);
        ioc.setClassification(classification);
        final IoCSource source = new IoCSource();
        source.setFQDN(matchedFQDN);
        ioc.setSource(source);
        final IoCTime time = new IoCTime();
        time.setObservation(Calendar.getInstance().getTime());
        time.setSource(time.getObservation());
        ioc.setTime(time);
        //final String serializedPayload = gson.toJson(ioc);

        try {
            /*
            coreServerConnection = (HttpURLConnection) new URL("http://feedcore-lb:8080/sinkit/rest/blacklist/ioc/").openConnection();
            coreServerConnection.setDoOutput(true);
            coreServerConnection.setRequestMethod(HttpMethod.POST);

            coreServerConnection.setRequestProperty("Content-Length", "" + Integer.toString(serializedPayload.getBytes().length));
            coreServerConnection.setRequestProperty("Content-Type", "application/json");
            coreServerConnection.setRequestProperty("X-sinkit-token", TOKEN);

            jsonContentBuffer = new BufferedWriter(new OutputStreamWriter(coreServerConnection.getOutputStream()));
            jsonContentBuffer.write(serializedPayload);
            jsonContentBuffer.flush();

            if (coreServerConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                log.log(Level.SEVERE, "logGSBEventRemotely: Creting IoC for GSB log failed.");
                return;
            }
            */

            final IoCSourceId sid = IoCSourceIdBuilder.build(ioc);
            ioc.getSource().setId(sid);
            ioc.setActive(true);

            final IoCSeen seen = new IoCSeen();
            seen.setLast(time.getObservation());
            seen.setFirst(time.getObservation());
            ioc.setSeen(seen);
            ioc.getTime().setReceivedByCore(time.getObservation());
            ioc.setDocumentId(IoCIdentificationUtils.computeHashedId(ioc));

            archiveService.archiveReceivedIoCRecord(ioc);

            IoCRecord retrievedIoC = archiveService.getIoCRecordById(ioc.getDocumentId());

            for (int i = 0; retrievedIoC == null && i < ARCHIVE_FAILED_TRIALS; i++) {
                retrievedIoC = archiveService.getIoCRecordById(ioc.getDocumentId());
            }

            if (retrievedIoC == null) {
                return null;
            }

            retrievedIoC.setVirusTotalReports(null);
            retrievedIoC.getSeen().setLast(null);
            retrievedIoC.setRaw(null);
            retrievedIoC.setActive(null);
            //documentId will changed whe ioc is deactivated so it's useless to have it here. Ioc.uniqueRef will be used for referencing ioc
            retrievedIoC.setDocumentId(null);

            return retrievedIoC;
        /*} catch (ProtocolException e) {
            log.log(Level.SEVERE, "logGSBEventRemotely: Request construction failed.", e);
        } catch (MalformedURLException e) {
            log.log(Level.SEVERE, "logGSBEventRemotely: URL co call Core servers is malformed.", e);
        } catch (IOException e) {
            log.log(Level.SEVERE, "logGSBEventRemotely: Failed to contact Core servers and log GSB event.", e);
        */
        } catch (IoCSourceIdException e) {
            log.log(Level.SEVERE, "logGSBEventRemotely: IoC construction failed.", e);
        } catch (ArchiveException e) {
            log.log(Level.SEVERE, "logGSBEventRemotely: Archive communication failed.", e);
        } /*finally {
            if (jsonContentBuffer != null) {
                try {
                    jsonContentBuffer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (coreServerConnection != null) {
                coreServerConnection.disconnect();
            }
        }*/
        return null;
    }
}
