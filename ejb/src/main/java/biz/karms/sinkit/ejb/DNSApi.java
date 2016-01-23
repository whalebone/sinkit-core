package biz.karms.sinkit.ejb;

import biz.karms.sinkit.ejb.dto.Sinkhole;
import biz.karms.sinkit.eventlog.EventLogAction;
import biz.karms.sinkit.exception.ArchiveException;

import javax.ejb.Local;
import java.util.Set;

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
            Set<String> matchedIoCs
    ) throws ArchiveException;
}
