package biz.karms.sinkit.rest;

import biz.karms.sinkit.ejb.*;
import biz.karms.sinkit.exception.ArchiveException;
import biz.karms.sinkit.exception.IoCSourceIdException;
import biz.karms.sinkit.exception.IoCValidationException;
import biz.karms.sinkit.ioc.IoCRecord;
import com.google.gson.GsonBuilder;

import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Michal Karm Babacek
 *         <p/>
 *         TODO: Validation and filtering :-)
 */
@SessionScoped
public class SinkitService implements Serializable {
    private static final long serialVersionUID = -940606926425473624L;
    public static final String ERR_MSG = "Error, please, check your input.";

    @EJB
    private WebApiEJB webapiEJB;

    @EJB
    private CoreServiceEJB coreService;

    @EJB
    private DNSApiEJB dnsApiEJB;

    @Inject
    private Logger log;

    String createHelloMessage(final String name) {
        return new GsonBuilder().create().toJson(webapiEJB.sayHello(name));
    }

    String getStats() {
        return new GsonBuilder().create().toJson(webapiEJB.getStats());
    }

    String putBlacklistedRecord(final String json) {
        try {
            log.log(Level.FINEST, "Received JSON [" + json + "]");
            BlacklistedRecord blacklistedRecord = new GsonBuilder().create().fromJson(json, BlacklistedRecord.class);
            blacklistedRecord = webapiEJB.putBlacklistedRecord(blacklistedRecord);
            if (blacklistedRecord != null) {
                return new GsonBuilder().create().toJson(blacklistedRecord);
            } else {
                return new GsonBuilder().create().toJson(ERR_MSG);
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "putBlacklistedRecord", e);
            return new GsonBuilder().create().toJson(ERR_MSG);
        }
    }

    String getBlacklistedRecord(final String key) {
        return new GsonBuilder().create().toJson(webapiEJB.getBlacklistedRecord(key));
    }

    String getSinkHole(final String client, final String key) {
        return new GsonBuilder().create().toJson(dnsApiEJB.getSinkHole(client, key));
    }

    String getBlacklistedRecordKeys() {
        return new GsonBuilder().create().toJson(webapiEJB.getBlacklistedRecordKeys());
    }

    String deleteBlacklistedRecord(final String key) {
        String message = webapiEJB.deleteBlacklistedRecord(key);
        if (message == null) {
            return new GsonBuilder().create().toJson(ERR_MSG);
        }
        return new GsonBuilder().create().toJson(message);
    }

    String putRule(final String json) {
        try {
            log.log(Level.FINEST, "Received JSON [" + json + "]");
            Rule rule = new GsonBuilder().create().fromJson(json, Rule.class);
            if (rule == null) {
                return new GsonBuilder().create().toJson(ERR_MSG);
            }
            return new GsonBuilder().create().toJson(webapiEJB.putRule(rule));
        } catch (Exception e) {
            log.log(Level.SEVERE, "putRule", e);
            return new GsonBuilder().create().toJson(ERR_MSG);
        }
    }

    String getRules(final String clientIPAddress) {
        return new GsonBuilder().create().toJson(webapiEJB.getRules(clientIPAddress));
    }

    String getRuleKeys() {
        return new GsonBuilder().create().toJson(webapiEJB.getRuleKeys());
    }

    String deleteRule(final String cidrAddress) {
        String message = webapiEJB.deleteRule(cidrAddress);
        if (message == null) {
            return new GsonBuilder().create().toJson(ERR_MSG);
        }
        return new GsonBuilder().create().toJson(message);
    }

    String processIoCRecord(String jsonIoCRecord) throws IoCValidationException, ArchiveException {

        IoCRecord ioc = new GsonBuilder().setDateFormat(IoCRecord.DATE_FORMAT).create().fromJson(jsonIoCRecord, IoCRecord.class);
        ioc = coreService.processIoCRecord(ioc);
        return new GsonBuilder().setDateFormat(IoCRecord.DATE_FORMAT).create().toJson(ioc);
    }
}
