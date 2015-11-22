package biz.karms.sinkit.ejb;

import biz.karms.sinkit.ejb.cache.pojo.BlacklistedRecord;
import biz.karms.sinkit.ejb.dto.AllDNSSettingDTO;
import biz.karms.sinkit.ejb.dto.CustomerCustomListDTO;
import biz.karms.sinkit.ejb.dto.FeedSettingCreateDTO;

import javax.ejb.Remote;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Michal Karm Babacek
 */
@Remote
public interface WebApi {
    String sayHello(final String queryString);

    Map<String, Integer> getStats();

    BlacklistedRecord putBlacklistedRecord(final BlacklistedRecord blacklistedRecord);

    BlacklistedRecord getBlacklistedRecord(final String key);

    Object[] getBlacklistedRecordKeys();

    String deleteBlacklistedRecord(final String key);

    List<?> getRules(final String clientIPAddress);

    Set<String> getRuleKeys();

    String deleteRule(final String cidrAddress);

    String putDNSClientSettings(final Integer customerId, final HashMap<String, HashMap<String, String>> customerDNSSetting);

    String postAllDNSClientSettings(final AllDNSSettingDTO[] allDNSSetting);

    String putCustomLists(final Integer customerId, final CustomerCustomListDTO[] customerCustomLists);

    String putFeedSettings(final String feedUid, final HashMap<Integer, HashMap<String, String>> feedSettings);

    String postCreateFeedSettings(FeedSettingCreateDTO feedSettingCreate);

    List<?> getAllRules();

    String deleteRulesByCustomer(Integer customerId);
}
