package biz.karms.sinkit.ejb;

import com.google.gson.GsonBuilder;
import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Michal Karm Babacek
 */
// TODO: Really?
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class ServiceEJB {

    @Inject
    private Logger log;

    @Inject
    private DefaultCacheManager m;

    private Cache<String, BlacklistedRecord> blacklistCache = null;

    @Inject
    private javax.transaction.UserTransaction utx;

    @PostConstruct
    public void setup() {
        blacklistCache = m.getCache("BLACKLIST_CACHE");
    }

    public static final String ERR_MSG = "Error, please, check your input.";

    // Testing purposes
    public String sayHello(String queryString) {
        return new GsonBuilder().create().toJson("Hello there." + queryString);
    }

    public String putBlacklistedRecord(String json) {
        try {
            log.info("Received JSON [" + json+ "]");
            BlacklistedRecord blacklistedRecord = new GsonBuilder().create().fromJson(json, BlacklistedRecord.class);
            utx.begin();
            log.info("Putting key [" + blacklistedRecord.getBlackListedDomainOrIP() + "]");
            blacklistCache.put(blacklistedRecord.getBlackListedDomainOrIP(), blacklistedRecord);
            utx.commit();
            return new GsonBuilder().create().toJson(blacklistedRecord);
        } catch (Exception e) {
            e.printStackTrace();
            log.log(Level.SEVERE, "putBlacklistedRecord", e);
            try {
                if (utx.getStatus() != javax.transaction.Status.STATUS_NO_TRANSACTION) {
                    utx.rollback();
                }
            } catch (Exception e1) {
                // TODO Hmm, make it better :-(
                e1.printStackTrace();
            }
            return new GsonBuilder().create().toJson(ERR_MSG);
        }
    }

    public String getBlacklistedRecord(String key) {
        log.info("getting key [" + key + "]");
        BlacklistedRecord blacklistedRecord = blacklistCache.get(key);
        return new GsonBuilder().create().toJson(blacklistedRecord);
    }

    public String getBlacklistedRecordKeys() {
        return new GsonBuilder().create().toJson(blacklistCache.keySet());
    }

    public String getStats() {
        Map<String, Integer> info = new HashMap<String, Integer>();
        info.put("Total documents", blacklistCache.size());
        return new GsonBuilder().create().toJson(info);
    }


    public String deleteBlacklistedRecord(String key) {
        try {
            utx.begin();
            String response;
            if (blacklistCache.containsKey(key)) {
                blacklistCache.remove(key);
                response = key + " DELETED";
            } else {
                response = key + " DOES NOT EXIST";
            }
            utx.commit();
            return new GsonBuilder().create().toJson(response);
        } catch (Exception e) {
            e.printStackTrace();
            log.log(Level.SEVERE, "deleteBlacklistedRecord", e);
            try {
                if (utx.getStatus() != javax.transaction.Status.STATUS_NO_TRANSACTION) {
                    utx.rollback();
                }
            } catch (Exception e1) {
                // TODO Hmm, make it better :-(
                e1.printStackTrace();
            }
            return new GsonBuilder().create().toJson(ERR_MSG);
        }
    }
}
