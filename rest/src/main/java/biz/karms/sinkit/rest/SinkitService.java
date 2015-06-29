package biz.karms.sinkit.rest;

import biz.karms.sinkit.ejb.BlacklistedRecord;
import biz.karms.sinkit.ejb.Rule;
import biz.karms.sinkit.ejb.ServiceEJB;
import biz.karms.sinkit.ejb.ArchiveServiceEJB;
import biz.karms.sinkit.ejb.CoreServiceEJB;
import biz.karms.sinkit.ioc.IoCRecord;

import com.google.gson.GsonBuilder;

import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import java.io.IOException;
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
    private static final long serialVersionUID = -940606926425474624L;
    public static final String ERR_MSG = "Error, please, check your input.";

    @EJB
    private ServiceEJB serviceEJB;

    @EJB
    private CoreServiceEJB coreService;

    @Inject
    private Logger log;

    String createHelloMessage(final String name) {
        return new GsonBuilder().create().toJson(serviceEJB.sayHello(name));
    }

    String getStats() {
        return new GsonBuilder().create().toJson(serviceEJB.getStats());
    }

    String putBlacklistedRecord(final String json) {
        try {
            log.log(Level.FINEST, "Received JSON [" + json + "]");
            BlacklistedRecord blacklistedRecord = new GsonBuilder().create().fromJson(json, BlacklistedRecord.class);
            blacklistedRecord = serviceEJB.putBlacklistedRecord(blacklistedRecord);
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
        return new GsonBuilder().create().toJson(serviceEJB.getBlacklistedRecord(key));
    }

    String getBlacklistedRecordKeys() {
        return new GsonBuilder().create().toJson(serviceEJB.getBlacklistedRecordKeys());
    }

    String deleteBlacklistedRecord(final String key) {
        String message = serviceEJB.deleteBlacklistedRecord(key);
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
            return new GsonBuilder().create().toJson(serviceEJB.putRule(rule));
        } catch (Exception e) {
            log.log(Level.SEVERE, "putRule", e);
            return new GsonBuilder().create().toJson(ERR_MSG);
        }
    }

    String getRules(final String clientIPAddress) {
        return new GsonBuilder().create().toJson(serviceEJB.getRules(clientIPAddress));
    }

    String getRuleKeys() {
        return new GsonBuilder().create().toJson(serviceEJB.getRuleKeys());
    }

    String deleteRule(final String cidrAddress) {
        String message = serviceEJB.deleteRule(cidrAddress);
        if (message == null) {
            return new GsonBuilder().create().toJson(ERR_MSG);
        }
        return new GsonBuilder().create().toJson(message);
    }

    String processIoCRecord(String jsonIoCRecord)  {

        String response;
        try {
            IoCRecord ioc = new GsonBuilder().setDateFormat(IoCRecord.DATE_FORMAT).create().fromJson(jsonIoCRecord, IoCRecord.class);

            ioc = coreService.processIoCRecord(ioc);

            response = new GsonBuilder().setDateFormat(IoCRecord.DATE_FORMAT).create().toJson(ioc);
        } catch (Exception e) {
            e.printStackTrace();
            response = new GsonBuilder().setDateFormat(IoCRecord.DATE_FORMAT).create().toJson(e.getMessage());
        }
        return response;
    }
}
