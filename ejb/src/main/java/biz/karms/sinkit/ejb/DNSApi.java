package biz.karms.sinkit.ejb;

import biz.karms.sinkit.ejb.dto.Sinkhole;

import javax.ejb.Local;

/**
 * @author Michal Karm Babacek
 */
@Local
public interface DNSApi {
    Sinkhole getSinkHole(String clientIPAddress, String fqdnOrIp, String fqdn, Integer clientId);
}
