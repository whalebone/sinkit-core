package biz.karms.sinkit.ejb.impl;

import biz.karms.sinkit.ejb.ArchiveService;
import biz.karms.sinkit.ejb.elastic.logstash.LogstashClient;
import biz.karms.sinkit.ejb.util.IoCIdentificationUtils;
import biz.karms.sinkit.eventlog.Accuracy;
import biz.karms.sinkit.eventlog.EventDNSRequest;
import biz.karms.sinkit.eventlog.EventLogAction;
import biz.karms.sinkit.eventlog.EventLogRecord;
import biz.karms.sinkit.eventlog.EventReason;
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
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Tomas Kozel
 */
@Stateless
@Asynchronous
@TransactionAttribute(TransactionAttributeType.NEVER)
@Local
public class DNSApiLoggingEJB {

    @Inject
    private Logger log;

    @EJB
    private ArchiveService archiveService;

    private static final boolean USE_LOGSTASH = StringUtils.isNotBlank(System.getenv(LogstashClient.LOGSTASH_URL_ENV));
    private static final int ARCHIVE_FAILED_TRIALS = 10;

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
    public void logDNSEvent(
            final EventLogAction action,
            final String clientUid,
            final String requestIp,
            final String requestFqdn,
            final String requestType,
            final String reasonFqdn,
            final String reasonIp,
            final Map<String, Set<ImmutablePair<String, String>>> matchedIoCs,
            final Map.Entry<String, HashMap<String, Integer>> theMostAccurateFeed,
            final Logger log
    ) throws ArchiveException {
        log.log(Level.FINE, "Logging DNS event. clientUid: " + clientUid + ", requestIp: " + requestIp + ", requestFqdn: " + requestFqdn + ", requestType: " + requestType + ", reasonFqdn: " + reasonFqdn + ", reasonIp: " + reasonIp);
        final EventLogRecord logRecord = new EventLogRecord();

        // Problems with Map.Entry in Elastic [TODO]
        final Map<String, Map<String, Integer>> accuracyFeed = new HashMap<>();
        if (theMostAccurateFeed != null) {
            accuracyFeed.put(theMostAccurateFeed.getKey(), theMostAccurateFeed.getValue());
            final int accuracy = theMostAccurateFeed.getValue().values().stream().mapToInt(Integer::intValue).sum();
            logRecord.setAccuracy(new Accuracy(accuracy, accuracyFeed));
        }

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

        if (MapUtils.isNotEmpty(matchedIoCs)) {
            final Set<IoCRecord> matchedIoCsList = new HashSet<>();
            log.log(Level.FINE, "Iterating matchedIoCs...");
            // { feed : [ type1 : iocId1, type2 : iocId2]}
            for (Map.Entry<String, Set<ImmutablePair<String, String>>> matchedIoC : matchedIoCs.entrySet()) {
                for (ImmutablePair<String, String> typeIoCID : matchedIoC.getValue()) {
                    IoCRecord ioCRecord;
                    if (StringUtils.isBlank(typeIoCID.getRight()) && StringUtils.isNotBlank(reasonFqdn)) {
                        // Nope, no IP, just FQDN for GSB
                        ioCRecord = processNonExistingIoC(reasonFqdn, matchedIoC.getKey(), typeIoCID.getLeft(), log);
                    } else {
                        ioCRecord = processRegularIoCId(typeIoCID.getRight(), archiveService, log);
                    }
                    if (ioCRecord != null) {
                        matchedIoCsList.add(ioCRecord);
                    }
                }
            }
            final IoCRecord[] matchedIoCsArray = matchedIoCsList.toArray(new IoCRecord[matchedIoCsList.size()]);
            logRecord.setMatchedIocs(matchedIoCsArray);
        }

        Arrays.stream(logRecord.getMatchedIocs()).filter(x -> DNSApiEJB.CUSTOM_LIST_FEED_NAME.equals(x.getFeed().getName())).forEach(x -> x.setUniqueRef(null));

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
    private IoCRecord processNonExistingIoC(final String matchedFQDN, String feedName, String type, Logger log) {
        final IoCRecord ioc = new IoCRecord();
        final IoCFeed feed = new IoCFeed();
        feed.setName(feedName);
        ioc.setFeed(feed);
        final IoCClassification classification = new IoCClassification();
        classification.setType(type);
        ioc.setClassification(classification);
        final IoCSource source = new IoCSource();
        source.setFqdn(matchedFQDN);
        ioc.setSource(source);
        final IoCTime time = new IoCTime();
        time.setObservation(Calendar.getInstance().getTime());
        time.setSource(time.getObservation());
        ioc.setTime(time);

        try {
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

            retrievedIoC.getSeen().setLast(null);
            retrievedIoC.setRaw(null);
            retrievedIoC.setActive(null);
            //documentId will changed whe ioc is deactivated so it's useless to have it here. Ioc.uniqueRef will be used for referencing ioc
            retrievedIoC.setDocumentId(null);

            return retrievedIoC;
        } catch (IoCSourceIdException e) {
            log.log(Level.SEVERE, "logGSBEventRemotely: IoC construction failed.", e);
        } catch (ArchiveException e) {
            log.log(Level.SEVERE, "logGSBEventRemotely: Archive communication failed.", e);
        }
        return null;
    }
}
