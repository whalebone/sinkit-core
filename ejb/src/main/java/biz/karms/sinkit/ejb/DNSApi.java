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
    Sinkhole getSinkHole(String clientIPAddress, String fqdnOrIp, String fqdn, Integer clientId);
}
