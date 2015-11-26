package biz.karms.sinkit.ejb;

import biz.karms.sinkit.ejb.cache.pojo.CustomList;
import biz.karms.sinkit.ejb.dto.Sinkhole;
import biz.karms.sinkit.eventlog.EventLogAction;
import biz.karms.sinkit.eventlog.EventLogRecord;
import biz.karms.sinkit.exception.ArchiveException;

import javax.ejb.Remote;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * @author Michal Karm Babacek
 */
@Remote
public interface DNSApi {
    List<?> rulesLookup(final String clientIPAddressPaddedBigInt);

    List<?> customListsLookup(final Integer customerId, final boolean isFQDN, final String fqdnOrIp);

    CustomList retrieveOneCustomList(final Integer customerId, final boolean isFQDN, final String fqdnOrIp);

    Sinkhole getSinkHole(final String clientIPAddress, final String fqdnOrIp, String fqdn);

    Future<EventLogRecord> logDNSEvent(
            EventLogAction action,
            String clientUid,
            String requestIp,
            String requestFqdn,
            String requestType,
            String reasonFqdn,
            String reasonIp,
            Set<String> matchedIoCs
    ) throws ArchiveException;
}
