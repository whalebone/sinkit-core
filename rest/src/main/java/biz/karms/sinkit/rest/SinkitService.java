package biz.karms.sinkit.rest;

import biz.karms.sinkit.ejb.ArchiveService;
import biz.karms.sinkit.ejb.CoreService;
import biz.karms.sinkit.ejb.WebApi;
import biz.karms.sinkit.ejb.cache.pojo.BlacklistedRecord;
import biz.karms.sinkit.ejb.cache.pojo.WhitelistedRecord;
import biz.karms.sinkit.ejb.dto.AllDNSSettingDTO;
import biz.karms.sinkit.ejb.dto.CustomerCustomListDTO;
import biz.karms.sinkit.ejb.dto.FeedSettingCreateDTO;
import biz.karms.sinkit.ejb.impl.DNSApiLoggingEJB;
import biz.karms.sinkit.eventlog.EventLogRecord;
import biz.karms.sinkit.exception.ArchiveException;
import biz.karms.sinkit.exception.IoCValidationException;
import biz.karms.sinkit.ioc.IoCAccuCheckerReport;
import biz.karms.sinkit.ioc.IoCRecord;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Michal Karm Babacek
 */
@RequestScoped
public class SinkitService implements Serializable {

    private static final String ERR_MSG = "Error, please, check your input.";
    private static final long serialVersionUID = 4301258460502614798L;

    @EJB
    private WebApi webapi;

    @EJB
    private CoreService coreService;

    @EJB
    private DNSApiLoggingEJB dnsApiLoggingEJB;

    @EJB
    private ArchiveService archiveService;

    @Inject
    private transient Logger log;

    String createHelloMessage(final String name) {
        return new GsonBuilder().create().toJson(webapi.sayHello(name));
    }

    String getStats() {
        return new GsonBuilder().create().toJson(webapi.getStats());
    }

    String putBlacklistedRecord(final String json) {
        try {
            log.log(Level.FINE, "Received JSON " + json);
            BlacklistedRecord blacklistedRecord = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create().fromJson(json, BlacklistedRecord.class);
            blacklistedRecord = webapi.putBlacklistedRecord(blacklistedRecord);
            if (blacklistedRecord != null) {
                return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create().toJson(blacklistedRecord);
            } else {
                return new GsonBuilder().create().toJson(ERR_MSG);
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "putBlacklistedRecord", e);
            return new GsonBuilder().create().toJson(ERR_MSG);
        }
    }

    String getBlacklistedRecord(final String key) {
        return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create().toJson(webapi.getBlacklistedRecord(key));
    }

    String getBlacklistedRecordKeys() {
        return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create().toJson(webapi.getBlacklistedRecordKeys());
    }

    String deleteBlacklistedRecord(final String key) {
        String message = webapi.deleteBlacklistedRecord(key);
        if (message == null) {
            return new GsonBuilder().create().toJson(ERR_MSG);
        }
        return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create().toJson(message);
    }

    String getRules(final String clientIPAddress) {
        return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create().toJson(webapi.getRules(clientIPAddress));
    }

    String getRuleKeys() {
        return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create().toJson(webapi.getRuleKeys());
    }

    String deleteRule(final String cidrAddress) {
        String message = webapi.deleteRule(cidrAddress);
        if (message == null) {
            return new GsonBuilder().create().toJson(ERR_MSG);
        }
        return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create().toJson(message);
    }

    String processIoCRecord(final String jsonIoCRecord) throws IoCValidationException, ArchiveException {
        log.log(Level.FINE, "jsonIoCRecord: " + jsonIoCRecord);
        final IoCRecord ioc = new GsonBuilder().setDateFormat(IoCRecord.DATE_FORMAT).create().fromJson(jsonIoCRecord, IoCRecord.class);
        return new GsonBuilder().setDateFormat(IoCRecord.DATE_FORMAT).create().toJson(coreService.processIoCRecord(ioc));
    }

    boolean updateAccuracy(final String report) throws ArchiveException, JsonParseException, IoCValidationException {
        log.log(Level.FINE, "Received report from accuchecker: " + report);

        final IoCAccuCheckerReport parsed_report = new GsonBuilder().create().fromJson(report, IoCAccuCheckerReport.class);
        // retry until true? ????
        return coreService.updateWithAccuCheckerReport(parsed_report);
    }

    String processWhitelistIoCRecord(final String jsonIoCRecord) throws IoCValidationException, ArchiveException {
        IoCRecord ioc = new GsonBuilder().setDateFormat(IoCRecord.DATE_FORMAT).create().fromJson(jsonIoCRecord, IoCRecord.class);
        boolean response = coreService.processWhitelistIoCRecord(ioc);
        return new GsonBuilder().setDateFormat(IoCRecord.DATE_FORMAT).create().toJson(response);
    }

    String getWhitelistedRecord(final String id) {
        if (id == null) {
            return new GsonBuilder().create().toJson(ERR_MSG);
        }
        WhitelistedRecord white = coreService.getWhitelistedRecord(id);
        return new GsonBuilder().setDateFormat(IoCRecord.DATE_FORMAT).create().toJson(white);
    }

    String removeWhitelistedRecord(final String id) {
        if (id == null) {
            return new GsonBuilder().create().toJson(ERR_MSG);
        }
        boolean response = coreService.removeWhitelistedRecord(id);
        return new GsonBuilder().create().toJson(response);
    }

    String isWhitelistEmpty() {
        return new GsonBuilder().create().toJson(coreService.isWhitelistEmpty());
    }

    String runCacheRebuilding() {
        String response;
        if (coreService.runCacheRebuilding()) {
            response = "Cache rebuilding started";
        } else {
            response = "Cache rebuilding already started";
        }
        return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create().toJson(response);

    }

    String putDNSClientSettings(final Integer customerId, final String json) {
        try {
            log.log(Level.FINE, "Received JSON " + json);
            HashMap<String, HashMap<String, String>> customerDNSSetting = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create().fromJson(json, new TypeToken<HashMap<String, HashMap<String, String>>>() {
            }.getType());
            if (customerDNSSetting == null) {
                return new GsonBuilder().create().toJson(ERR_MSG);
            }
            return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create().toJson(webapi.putDNSClientSettings(customerId, customerDNSSetting));
        } catch (Exception e) {
            log.log(Level.SEVERE, "putDNSClientSettings", e);
            return new GsonBuilder().create().toJson(ERR_MSG);
        }
    }

    String postAllDNSClientSettings(final String json) {
        try {
            log.log(Level.FINE, "Received JSON " + json);
            AllDNSSettingDTO[] allDNSSetting = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create().fromJson(json, AllDNSSettingDTO[].class);
            if (allDNSSetting == null) {
                return new GsonBuilder().create().toJson(ERR_MSG);
            }
            return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create().toJson(webapi.postAllDNSClientSettings(allDNSSetting));
        } catch (Exception e) {
            log.log(Level.SEVERE, "postAllDNSClientSettings", e);
            return new GsonBuilder().create().toJson(ERR_MSG);
        }
    }

    String putCustomLists(final Integer customerId, final String json) {
        try {
            log.log(Level.FINE, "Received JSON " + json);
            CustomerCustomListDTO[] customerCustomLists = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create().fromJson(json, CustomerCustomListDTO[].class);
            if (customerCustomLists == null) {
                log.log(Level.FINE, "Returning error.");
                return new GsonBuilder().create().toJson(ERR_MSG);
            }
            log.log(Level.FINE, "Gonna call webapi...");
            return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create().toJson(webapi.putCustomLists(customerId, customerCustomLists));
        } catch (Exception e) {
            log.log(Level.SEVERE, "putCustomLists", e);
            return new GsonBuilder().create().toJson(ERR_MSG);
        }
    }

    String putFeedSettings(final String feedUid, final String json) {
        try {
            log.log(Level.FINE, "Received JSON " + json);
            HashMap<Integer, HashMap<String, String>> feedSettings = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create().fromJson(json, new TypeToken<HashMap<Integer, HashMap<String, String>>>() {
            }.getType());
            if (feedSettings == null) {
                return new GsonBuilder().create().toJson(ERR_MSG);
            }
            return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create().toJson(webapi.putFeedSettings(feedUid, feedSettings));
        } catch (Exception e) {
            log.log(Level.SEVERE, "putFeedSettings", e);
            return new GsonBuilder().create().toJson(ERR_MSG);
        }
    }

    String postCreateFeedSettings(final String json) {
        try {
            log.log(Level.FINE, "Received JSON " + json);
            FeedSettingCreateDTO feedSettingCreate = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create().fromJson(json, FeedSettingCreateDTO.class);
            if (feedSettingCreate == null) {
                return new GsonBuilder().create().toJson(ERR_MSG);
            }
            return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create().toJson(webapi.postCreateFeedSettings(feedSettingCreate));
        } catch (Exception e) {
            log.log(Level.SEVERE, "postCreateFeedSettings", e);
            return new GsonBuilder().create().toJson(ERR_MSG);
        }
    }

    void addEventLogRecord(final String json) throws ArchiveException {
        EventLogRecord logRec = new GsonBuilder().create().fromJson(json, EventLogRecord.class);
        Map<String, Set<ImmutablePair<String, String>>> ids = new HashMap<>();
        Set<ImmutablePair<String, String>> typeIoCId;
        for (IoCRecord ioc : logRec.getMatchedIocs()) {
            typeIoCId = new HashSet<>();
            typeIoCId.add(new ImmutablePair<>("", ioc.getDocumentId()));
            ids.put(ioc.getDocumentId(), typeIoCId);
        }
        dnsApiLoggingEJB.logDNSEvent(
                logRec.getAction(),
                logRec.getClient(),
                logRec.getRequest().getIp(),
                logRec.getRequest().getFqdn(),
                logRec.getRequest().getType(),
                logRec.getReason().getFqdn(),
                logRec.getReason().getIp(),
                ids,
                null,
                log);
    }

    public void enrich() {
        coreService.enrich();
    }

    public String getAllRules() {
        return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create().toJson(webapi.getAllRules());
    }

    public String deleteRulesByCustomer(final Integer customerId) {
        return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create().toJson(webapi.deleteRulesByCustomer(customerId));
    }

}
