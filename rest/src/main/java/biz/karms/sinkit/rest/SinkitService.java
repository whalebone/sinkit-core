package biz.karms.sinkit.rest;

import biz.karms.sinkit.ejb.ServiceEJB;

import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;

/**
 * @author Michal Karm Babacek
 */
@SessionScoped
public class SinkitService implements Serializable {
    private static final long serialVersionUID = -940606926425474624L;

    @EJB
    private ServiceEJB serviceEJB;

    String createHelloMessage(String name) {
        return serviceEJB.sayHello(name);
    }

    String getBlacklistedRecord(String key) {
        return serviceEJB.getBlacklistedRecord(key);
    }

    String getBlacklistedRecordKeys() {
        return serviceEJB.getBlacklistedRecordKeys();
    }

    String getStats() {
        return serviceEJB.getStats();
    }

    String deleteBlacklistedRecord(String key) {
        return serviceEJB.deleteBlacklistedRecord(key);
    }

    String putBlacklistedRecord(String record) {
        return serviceEJB.putBlacklistedRecord(record);
    }
}
