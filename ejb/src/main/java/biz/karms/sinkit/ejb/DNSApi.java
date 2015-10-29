package biz.karms.sinkit.ejb;

import biz.karms.sinkit.ejb.cache.pojo.CustomList;
import biz.karms.sinkit.ejb.cache.pojo.Rule;
import biz.karms.sinkit.ejb.dto.Sinkhole;

import javax.ejb.Remote;
import java.util.List;

/**
 * @author Michal Karm Babacek
 */
@Remote
public interface DNSApi {
    List<Rule> rulesLookup(final String clientIPAddressPaddedBigInt);

    List<CustomList> customListsLookup(final Integer customerId, final boolean isFQDN, final String fqdnOrIp);

    CustomList retrieveOneCustomList(final Integer customerId, final boolean isFQDN, final String fqdnOrIp);

    Sinkhole getSinkHole(final String clientIPAddress, final String fqdnOrIp);
}
