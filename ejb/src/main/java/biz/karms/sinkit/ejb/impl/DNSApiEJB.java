package biz.karms.sinkit.ejb.impl;

import biz.karms.sinkit.ejb.ArchiveService;
import biz.karms.sinkit.ejb.CoreService;
import biz.karms.sinkit.ejb.DNSApi;
import biz.karms.sinkit.ejb.GSBService;
import biz.karms.sinkit.ejb.WebApi;
import biz.karms.sinkit.ejb.cache.annotations.SinkitCache;
import biz.karms.sinkit.ejb.cache.annotations.SinkitCacheName;
import biz.karms.sinkit.ejb.cache.pojo.BlacklistedRecord;
import biz.karms.sinkit.ejb.cache.pojo.CustomList;
import biz.karms.sinkit.ejb.cache.pojo.Rule;
import biz.karms.sinkit.ejb.dto.Sinkhole;
import biz.karms.sinkit.ejb.util.CIDRUtils;
import biz.karms.sinkit.eventlog.EventDNSRequest;
import biz.karms.sinkit.eventlog.EventLogAction;
import biz.karms.sinkit.eventlog.EventLogRecord;
import biz.karms.sinkit.eventlog.EventReason;
import biz.karms.sinkit.eventlog.VirusTotalRequest;
import biz.karms.sinkit.eventlog.VirusTotalRequestStatus;
import biz.karms.sinkit.exception.ArchiveException;
import biz.karms.sinkit.ioc.IoCRecord;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.validator.routines.DomainValidator;
import org.infinispan.Cache;
import org.infinispan.query.Search;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.jboss.marshalling.Pair;

import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


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
    @SinkitCache(SinkitCacheName.BLACKLIST_CACHE)
    private Cache<String, BlacklistedRecord> blacklistCache;

    @Inject
    @SinkitCache(SinkitCacheName.RULES_CACHE)
    private Cache<String, Rule> ruleCache;

    @Inject
    @SinkitCache(SinkitCacheName.CUSTOM_LISTS_CACHE)
    private Cache<String, CustomList> customListsCache;

    @Inject
    @SinkitCache(SinkitCacheName.CUSTOM_LISTS_LOCAL_CACHE)
    private Cache<String, List<CustomList>> customListsLocalCache;

    @Inject
    @SinkitCache(SinkitCacheName.RULES_LOCAL_CACHE)
    private Cache<String, List<Rule>> ruleLocalCache;

    private static final List<String> FEEDTYPES = Arrays.asList("c&c", "malware", "ransomware", "malware configuration", "phishing", "blacklist");
    private static final String IPV6SINKHOLE = System.getenv("SINKIT_SINKHOLE_IPV6");
    private static final String IPV4SINKHOLE = System.getenv("SINKIT_SINKHOLE_IP");

    // TODO: List? Array? Map with additional data? Let's think this over.
    // TODO: Replace/factor out duplicated code in .getRules out of webApiEJB
    private List<Rule> rulesLookup(final String clientIPAddressPaddedBigInt) {
        try {
            log.log(Level.FINE, "Getting key BigInteger zero padded representation " + clientIPAddressPaddedBigInt);
            // Let's try to hit it
            final Rule rule = ruleCache.get(clientIPAddressPaddedBigInt);
            if (rule != null) {
                return Collections.singletonList(rule);
            }
            // Let's search subnets
            final String keyInCache = DigestUtils.md5Hex(clientIPAddressPaddedBigInt + clientIPAddressPaddedBigInt);
            log.log(Level.FINE, "keyInCache: " + keyInCache + ", from: " + (clientIPAddressPaddedBigInt + clientIPAddressPaddedBigInt));

            final List<Rule> cached = ruleLocalCache.get(keyInCache);
            if (cached != null) {
                return cached;
            } else {
                final QueryFactory qf = Search.getQueryFactory(ruleCache);
                Query query = qf.from(Rule.class)
                        .having("startAddress").lte(clientIPAddressPaddedBigInt)
                        .and()
                        .having("endAddress").gte(clientIPAddressPaddedBigInt)
                        .toBuilder().build();
                if (query != null) {
                    final List<Rule> result = query.list();
                    ruleLocalCache.putAsync(keyInCache, result);
                    return result;

                }
                return Collections.emptyList();
            }

            /*
            SearchManager searchManager = org.infinispan.query.Search.getSearchManager(ruleCache);
            QueryBuilder queryBuilder = searchManager.buildQueryBuilderForClass(Rule.class).get();
            Query luceneQuery = queryBuilder
                    .bool()
                    .must(queryBuilder.range().onField("startAddress").below(clientIPAddressPaddedBigInt).createQuery())
                    .must(queryBuilder.range().onField("endAddress").above(clientIPAddressPaddedBigInt).createQuery())
                    .createQuery();

            CacheQuery query = searchManager.getQuery(luceneQuery, Rule.class);
            log.log(Level.FINE, "Query result size: " + query.getResultSize());
            //log.log(Level.FINE, "Query result size: " + query.explain(1).toString());
            return query.list();
            */

        } catch (Exception e) {
            log.log(Level.SEVERE, "getRules client address troubles", e);
            return null;
        }
    }

    private List<CustomList> customListsLookup(final Integer customerId, final boolean isFQDN, final String fqdnOrIp) {
        //SearchManager searchManager = org.infinispan.query.Search.getSearchManager(customListsCache);
        //QueryBuilder queryBuilder = searchManager.buildQueryBuilderForClass(CustomList.class).get();
        //Query luceneQuery;
        final QueryFactory qf = Search.getQueryFactory(customListsCache);
        Query query = null;
        if (isFQDN) {

            final String keyInCache = DigestUtils.md5Hex(customerId + fqdnOrIp);
            log.log(Level.FINE, "keyInCache: " + keyInCache + ", from: " + (customerId + fqdnOrIp));

            final List<CustomList> cached = customListsLocalCache.get(keyInCache);
            if (cached != null) {
                return cached;
            } else {

            /*
            luceneQuery = queryBuilder
            .bool()
            .must(queryBuilder.keyword().onField("customerId").matching(customerId).createQuery())
            .must(queryBuilder.keyword().wildcard().onField("fqdn").matching(fqdnOrIp).createQuery())
            .createQuery();
             */
                query = qf.from(CustomList.class)
                        .having("customerId").eq(customerId)
                        .and()
                        .having("fqdn").like("%" + fqdnOrIp + "%")
                        .toBuilder().build();
                if (query != null) {
                    final List<CustomList> result = query.list();
                    customListsLocalCache.putAsync(keyInCache, result);
                    return result;
                }
                return null;
            }
        } else {
            final String clientIPAddressPaddedBigInt;
            try {
                clientIPAddressPaddedBigInt = CIDRUtils.getStartEndAddresses(fqdnOrIp).getA();
            } catch (UnknownHostException e) {
                log.log(Level.FINE, "customListsLookup: " + fqdnOrIp + " in not a valid IP address nor a valid FQDN.");
                return null;
            }

            /*
            luceneQuery = queryBuilder
                    .bool()
                    .must(queryBuilder.keyword().onField("customerId").matching(customerId).createQuery())
                    .must(queryBuilder.range().onField("listStartAddress").below(clientIPAddressPaddedBigInt).createQuery())
                    .must(queryBuilder.range().onField("listEndAddress").above(clientIPAddressPaddedBigInt).createQuery())
                    .createQuery();
            */

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
                    customListsLocalCache.putAsync(keyInCache, result);
                    return result;
                }
                return null;
            }
        }
        //return searchManager.getQuery(luceneQuery, CustomList.class).list();
    }

    private CustomList retrieveOneCustomList(final Integer customerId, final boolean isFQDN, final String fqdnOrIp) {
        //TODO logic about B|W lists
        //TODO logic about *something.foo.com being less important then bar.something.foo.com
        //TODO This is just a stupid dummy/mock
        final List<CustomList> customLists = customListsLookup(customerId, isFQDN, fqdnOrIp);
        return (CollectionUtils.isEmpty(customLists)) ? null : customLists.get(0);
    }

    /**
     * Sinkhole, to be called by DNS client.
     *
     * @param clientIPAddress - DNS server IP
     * @param fqdnOrIp        - FQDN DNS is trying to resolve or resolved IP (v6 or v4)
     * @param fqdn
     * @return null if there is an error and/or there is no reason to sinkhole or Sinkhole instance on positive hit
     */
    @Override
    public Sinkhole getSinkHole(final String clientIPAddress, final String fqdnOrIp, final String fqdn) {
        /**
         * At first, we lookup Rules
         */
        final String clientIPAddressPaddedBigInt;
        try {
            clientIPAddressPaddedBigInt = CIDRUtils.getStartEndAddresses(clientIPAddress).getA();
        } catch (UnknownHostException e) {
            log.log(Level.SEVERE, "getSinkHole: clientIPAddress " + clientIPAddress + " in not a valid address.");
            return null;
        }
        final boolean probablyIsIPv6 = fqdnOrIp.contains(":");

        // Lookup Rules (gives customerId, feeds and their settings)
        //TODO: Add test that all such found rules have the same customerId
        //TODO: factor .getRules out of webApiEJB
        final List<Rule> rules = rulesLookup(clientIPAddressPaddedBigInt);

        //If there is no rule, we simply don't sinkhole anything.
        if (CollectionUtils.isEmpty(rules)) {
            //TODO: Distinguish this from an error state.
            return null;
        }

        //TODO: regarding get(0): Solve overlapping customer settings.
        // Customer ID for this whole method context
        final int customerId = rules.get(0).getCustomerId();
        // To determine whether key is a FQDN or an IP address
        final boolean isFQDN = DomainValidator.getInstance().isValid(fqdnOrIp);

        /**
         * Next we fetch one and only one or none CustomList for a given fqdnOrIp
         */
        final CustomList customList = retrieveOneCustomList(customerId, isFQDN, fqdnOrIp);

        // Was it found in any of customer's Black/White/Log lists?
        // TODO: Implement logging for whitelisted stuff that's positive on IoC.
        if (customList != null) {
            // Whitelisted
            if ("W".equals(customList.getWhiteBlackLog())) {
                //TODO: Distinguish this from an error state.
                return null;
                //Blacklisted
            } else if ("B".equals(customList.getWhiteBlackLog())) {
                return new Sinkhole(probablyIsIPv6 ? IPV6SINKHOLE : IPV4SINKHOLE);
                // L for audit logging is not implemented on purpose. Ask Robert/Karm.
            } else if ("L".equals(customList.getWhiteBlackLog())) {
                // Do nothing
                log.log(Level.SEVERE, "getSinkHole: getWhiteBlackLog returned L. It shouldn't be used. Something is wrong.");
            } else {
                log.log(Level.SEVERE, "getSinkHole: getWhiteBlackLog must be one of B, W, L but was: " + customList.getWhiteBlackLog());
                return null;
            }
        }

        /**
         * Now it's the time to search IoC cache
         */
        // Lookup BlacklistedRecord from IoC cache (gives feeds)
        log.log(Level.FINE, "getSinkHole: getting key " + fqdnOrIp);
        BlacklistedRecord blacklistedRecord = blacklistCache.get(fqdnOrIp);
        if (blacklistedRecord == null) {
            log.log(Level.FINE, "No hit. The requested fqdnOrIp: " + fqdnOrIp + " is clean.");

            // Our IoC cache didn't contain a hit, so we give it a shot with GSB.
            final Set<String> gsbResults = gsbService.lookup(fqdnOrIp);
            if (!CollectionUtils.isEmpty(gsbResults)) {
                // TODO: We should somehow log this the IoC way...
                log.log(Level.INFO, "fqdnOrIp: " + fqdnOrIp + " found in GSB. Ignoring silently.");
            }
            return null;
        }
        // Feed UID : Type
        final Map<String, Pair<String, String>> feedTypeMap = blacklistedRecord.getSources();
        //If there is no feed, we simply don't sinkhole anything. It is weird though.
        if (MapUtils.isEmpty(feedTypeMap)) {
            log.log(Level.WARNING, "getSinkHole: IoC without feed settings.");
            return null;
        }
        // Feed UID, Mode <L|S|D>. In the end, we operate on a one selected Feed:mode pair only.
        String mode = null;
        //TODO: Nested cycles, worth profiling.
        for (Rule rule : rules) {
            for (String uuid : rule.getSources().keySet()) {
                // feed uuid : [type , docID], so "getA()" means get type
                Pair<String, String> typeDocId = feedTypeMap.get(uuid);
                if (typeDocId != null && FEEDTYPES.contains(typeDocId.getA())) {
                    String tmpMode = rule.getSources().get(uuid);
                    if (mode == null || ("S".equals(tmpMode) && !"D".equals(mode)) || "D".equals(tmpMode)) {
                        //D >= S >= L >= null, i.e. if a feed is Disabled, we don't switch to Sinkhole.
                        mode = tmpMode;
                    }
                } else {
                    log.log(Level.FINE, "getSinkHole: BlacklistedRecord " + blacklistedRecord.getBlackListedDomainOrIP() + " for feed " + uuid + " does not have Type nor DocID.");
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
                logDNSEvent(EventLogAction.BLOCK, String.valueOf(customerId), clientIPAddress, fqdn, null, (isFQDN) ? fqdnOrIp : null, (isFQDN) ? null : fqdnOrIp, unwrapDocumentIds(feedTypeMap.values()));
                log.log(Level.FINE, "getSinkHole: coreService.logDNSEvent returned.");
            } catch (ArchiveException e) {
                log.log(Level.SEVERE, "getSinkHole: Logging BLOCK failed: ", e);
            } finally {
                return new Sinkhole(probablyIsIPv6 ? IPV6SINKHOLE : IPV4SINKHOLE);
            }
        } else if ("L".equals(mode)) {
            //Log it for customer
            log.log(Level.FINE, "getSinkHole: Log.");
            try {
                logDNSEvent(EventLogAction.AUDIT, String.valueOf(customerId), clientIPAddress, fqdn, null, (isFQDN) ? fqdnOrIp : null, (isFQDN) ? null : fqdnOrIp, unwrapDocumentIds(feedTypeMap.values()));
            } catch (ArchiveException e) {
                log.log(Level.SEVERE, "getSinkHole: Logging AUDIT failed: ", e);
            } finally {
                //TODO: Distinguish this from an error state.
                return null;
            }
        } else if ("D".equals(mode)) {
            //Log it for us
            log.log(Level.FINE, "getSinkHole: Log internally.");
            try {
                logDNSEvent(EventLogAction.INTERNAL, String.valueOf(customerId), clientIPAddress, fqdn, null, (isFQDN) ? fqdnOrIp : null, (isFQDN) ? null : fqdnOrIp, unwrapDocumentIds(feedTypeMap.values()));
            } catch (ArchiveException e) {
                log.log(Level.SEVERE, "getSinkHole: Logging INTERNAL failed: ", e);
            } finally {
                //TODO: Distinguish this from an error state.
                return null;
            }
        } else {
            log.log(Level.SEVERE, "getSinkHole: Feed mode must be one of L,S,D, null but was: " + mode);
            return null;
        }

    }

    private Set<String> unwrapDocumentIds(Collection<Pair<String, String>> pairs) {
        return pairs.stream().map(Pair::getB).collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * Fire and Forget pattern
     *
     * @param action      BLOCK|AUDIT|INTERNAL
     * @param clientUid   Client's UID
     * @param requestIp   Client's IP address
     * @param requestFqdn TFDN client wanted to have resolved by DNS
     * @param requestType DNS request type
     * @param reasonFqdn  IOC hit
     * @param reasonIp    IOC hit
     * @param matchedIoCs IoCs with the same feed listed
     * @throws ArchiveException
     */
    @Asynchronous
    @Override
    public void logDNSEvent(
            EventLogAction action,
            String clientUid,
            String requestIp,
            String requestFqdn,
            String requestType,
            String reasonFqdn,
            String reasonIp,
            Set<String> matchedIoCs
    ) throws ArchiveException {
        log.log(Level.FINE, "Logging DNS event. clientUid: " + clientUid + ", requestIp: " + requestIp + ", requestFqdn: " + requestFqdn + ", requestType: " + requestType + ", reasonFqdn: " + reasonFqdn + ", reasonIp: " + reasonIp);
        EventLogRecord logRecord = new EventLogRecord();

        EventDNSRequest request = new EventDNSRequest();
        request.setIp(requestIp);
        request.setFqdn(requestFqdn);
        request.setType(requestType);
        logRecord.setRequest(request);

        EventReason reason = new EventReason();
        reason.setIp(reasonIp);
        reason.setFqdn(reasonFqdn);
        logRecord.setReason(reason);

        logRecord.setAction(action);
        logRecord.setClient(clientUid);
        logRecord.setLogged(Calendar.getInstance().getTime());

        List<IoCRecord> matchedIoCsList = new ArrayList<>();
        log.log(Level.FINE, "Iterating matchedIoCs...");
        for (String iocId : matchedIoCs) {
            IoCRecord ioc = archiveService.getIoCRecordById(iocId);
            if (ioc == null) {
                log.warning("Match IoC with id " + iocId + " was not found (deactivated??) -> skipping.");
                continue;
            }
            ioc.setVirusTotalReports(null);
            ioc.getSeen().setLast(null);
            ioc.setRaw(null);
            ioc.setActive(null);
            ioc.setDocumentId(null); //documentId will changed whe ioc is deactivated so it's useless to have it here. Ioc.uniqueRef will be used for referencing ioc

            matchedIoCsList.add(ioc);
        }
        IoCRecord[] matchedIoCsArray = matchedIoCsList.toArray(new IoCRecord[matchedIoCsList.size()]);
        logRecord.setMatchedIocs(matchedIoCsArray);

        VirusTotalRequest vtReq = new VirusTotalRequest();
        vtReq.setStatus(VirusTotalRequestStatus.WAITING);
        logRecord.setVirusTotalRequest(vtReq);
        archiveService.archiveEventLogRecord(logRecord);
    }
}
