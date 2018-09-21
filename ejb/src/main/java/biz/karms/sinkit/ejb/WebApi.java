package biz.karms.sinkit.ejb;

import biz.karms.sinkit.ejb.cache.pojo.BlacklistedRecord;
import biz.karms.sinkit.ejb.dto.AllDNSSettingDTO;
import biz.karms.sinkit.ejb.dto.CustomerCustomListDTO;
import biz.karms.sinkit.ejb.dto.FeedSettingCreateDTO;
import biz.karms.sinkit.exception.EndUserConfigurationValidationException;
import biz.karms.sinkit.exception.ResolverConfigurationValidationException;
import biz.karms.sinkit.resolver.EndUserConfiguration;
import biz.karms.sinkit.resolver.ResolverConfiguration;

import javax.ejb.Local;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Michal Karm Babacek
 */
@Local
public interface WebApi {
    String sayHello(final String queryString);

    Map<String, Map<String, String>> getStats();

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

    ResolverConfiguration putResolverConfiguration(ResolverConfiguration configuration) throws ResolverConfigurationValidationException;

    ResolverConfiguration getResolverConfiguration(int resolverId);

    ResolverConfiguration deleteResolverConfiguration(int resolverId);

    List<ResolverConfiguration> getAllResolverConfigurations();

    EndUserConfiguration putEndUserConfiguration(EndUserConfiguration configuration) throws EndUserConfigurationValidationException;

    EndUserConfiguration getEndUserConfiguration(String identity);

    EndUserConfiguration deleteEndUserConfiguration(String identity);

    List<EndUserConfiguration> getAllEndUserConfigurations();
}
