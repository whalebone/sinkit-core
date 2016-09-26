package biz.karms.sinkit.ejb;

import biz.karms.sinkit.ejb.dto.Sinkhole;
import biz.karms.sinkit.eventlog.EventLogAction;
import biz.karms.sinkit.exception.ArchiveException;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.ejb.Local;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * @author Michal Karm Babacek
 */
@Local
public interface DNSApi {
    Sinkhole getSinkHole(final String clientIPAddress, final String fqdnOrIp, String fqdn);

    void logDNSEvent(
            EventLogAction action,
            String clientUid,
            String requestIp,
            String requestFqdn,
            String requestType,
            String reasonFqdn,
            String reasonIp,
            // {feed: [type1: iocId1, type2: iocId2, ...]}
            Map<String, Set<ImmutablePair<String, String>>> matchedIoCs,
            ArchiveService archiveService,
            Logger logger
    ) throws ArchiveException;
}
