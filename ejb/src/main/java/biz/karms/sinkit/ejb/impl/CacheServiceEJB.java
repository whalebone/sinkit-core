package biz.karms.sinkit.ejb.impl;

import biz.karms.sinkit.ejb.CacheService;
import biz.karms.sinkit.ejb.MyCacheManagerProvider;
import biz.karms.sinkit.ejb.cache.pojo.BlacklistedRecord;
import biz.karms.sinkit.ejb.cache.pojo.Rule;
import biz.karms.sinkit.ioc.IoCRecord;
import org.infinispan.Cache;
import org.jboss.marshalling.Pair;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Michal Karm Babacek
 */
@Stateless
//@TransactionManagement(TransactionManagementType.BEAN)
//@TransactionManagement(TransactionManagementType.CONTAINER)
public class CacheServiceEJB implements CacheService {

    @Inject
    private Logger log;

    @Inject
    private MyCacheManagerProvider m;

    private Cache<String, BlacklistedRecord> blacklistCache;

    private Cache<String, Rule> ruleCache;

    //@Inject
    //private javax.transaction.UserTransaction utx;

    @PostConstruct
    public void setup() {
        blacklistCache = m.getCache("BLACKLIST_CACHE");
        ruleCache = m.getCache("RULES_CACHE");

        if (blacklistCache == null || ruleCache == null) {
            throw new IllegalStateException("Both BLACKLIST_CACHE and RULES_CACHE must not be null.");
        }

    }

    //TODO: Batch mode. It is wasteful to operate for 1 single update like this for thousand times.
    @Override
    public boolean addToCache(final IoCRecord ioCRecord) {
        if (ioCRecord == null || ioCRecord.getSource() == null || ioCRecord.getClassification() == null || ioCRecord.getFeed() == null || ioCRecord.getDocumentId() == null) {
            log.log(Level.SEVERE, "addToCache: ioCRecord itself or its source, documentId, classification or feed were null. Can't process that.");
           /* try {
                if (utx.getStatus() != javax.transaction.Status.STATUS_NO_TRANSACTION) {
                    utx.rollback();
                }
            } catch (Exception e1) {
                log.log(Level.SEVERE, "addToCache: Rolling back.", e1);
                return false; //finally?
            }
            */
            return false;
        }

        if (ioCRecord.getSource().getId() == null || ioCRecord.getSource().getId().getValue() == null) {
            log.log(Level.SEVERE, "addToCache: ioCRecord can't have source id null.");
            /*try {
                if (utx.getStatus() != javax.transaction.Status.STATUS_NO_TRANSACTION) {
                    utx.rollback();
                }
            } catch (Exception e1) {
                log.log(Level.SEVERE, "addToCache: Rolling back.", e1);
                return false;
            } //finally?*/
            return false;
        }

        // TODO: Should we use hashes? Is thus constructed key <= 255?
        final String key = ioCRecord.getSource().getId().getValue();
        if (key != null) {
            try {
                //utx.begin();
                if (key != null) {
                    if (blacklistCache.containsKey(key)) {
                        BlacklistedRecord blacklistedRecord = blacklistCache.get(key);
                        HashMap<String, Pair<String, String>> feedToTypeUpdate = blacklistedRecord.getSources();
                        if (ioCRecord.getFeed().getName() != null && ioCRecord.getClassification().getType() != null) {
                            feedToTypeUpdate.putIfAbsent(ioCRecord.getFeed().getName(), new Pair<>(ioCRecord.getClassification().getType(), ioCRecord.getDocumentId()));
                        } else {
                            log.log(Level.SEVERE, "addToCache: ioCRecord's feed or classification type were null");
                        }
                        blacklistedRecord.setSources(feedToTypeUpdate);
                        blacklistedRecord.setListed(Calendar.getInstance());
                        log.log(Level.FINE, "Replacing key [" + key + "]");
                        blacklistCache.replaceAsync(key, blacklistedRecord);
                        //utx.commit();
                    } else {
                        HashMap<String, Pair<String, String>> feedToType = new HashMap<>();
                        if (ioCRecord.getFeed().getName() != null && ioCRecord.getClassification().getType() != null) {
                            feedToType.put(ioCRecord.getFeed().getName(), new Pair<>(ioCRecord.getClassification().getType(), ioCRecord.getDocumentId()));
                        } else {
                            log.log(Level.SEVERE, "addToCache: ioCRecord's feed or classification type were null");
                        }
                        BlacklistedRecord blacklistedRecord = new BlacklistedRecord(key, Calendar.getInstance(), feedToType);
                        log.log(Level.FINE, "Putting new key [" + blacklistedRecord.getBlackListedDomainOrIP() + "]");
                        //blacklistCache.put(blacklistedRecord.getBlackListedDomainOrIP(), blacklistedRecord);
                        blacklistCache.putAsync(blacklistedRecord.getBlackListedDomainOrIP(), blacklistedRecord);
                        //utx.commit();
                    }
                }
            } catch (Exception e) {
                log.log(Level.SEVERE, "addToCache", e);
                /*try {
                    if (utx.getStatus() != javax.transaction.Status.STATUS_NO_TRANSACTION) {
                        utx.rollback();
                    }
                } catch (Exception e1) {
                    log.log(Level.SEVERE, "Rolling back", e1);
                    return false; //finally?
                }*/
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean removeFromCache(final IoCRecord ioCRecord) {
        if (ioCRecord == null || ioCRecord.getSource() == null || ioCRecord.getFeed() == null) {
            log.log(Level.SEVERE, "removeFromCache: ioCRecord itself or its source or its feed were null. Can't process that.");
            /*try {
                if (utx.getStatus() != javax.transaction.Status.STATUS_NO_TRANSACTION) {
                    utx.rollback();
                }
            } catch (Exception e1) {
                log.log(Level.SEVERE, "removeFromCache: Rolling back.", e1);
                return false; //finally?
            }*/
            return false;
        }

        if (ioCRecord.getSource().getId() == null && ioCRecord.getSource().getId().getValue() == null) {
            log.log(Level.SEVERE, "removeFromCache: ioCRecord can't have source id null.");
            /*try {
                if (utx.getStatus() != javax.transaction.Status.STATUS_NO_TRANSACTION) {
                    utx.rollback();
                }
            } catch (Exception e1) {
                log.log(Level.SEVERE, "removeFromCache: Rolling back.", e1);
                return false; //finally?
            }*/
            return false;
        }

        final String key = ioCRecord.getSource().getId().getValue();
        if (key != null) {
            try {
                if (blacklistCache.containsKey(key)) {
                    //utx.begin();
                    BlacklistedRecord blacklistedRecord = blacklistCache.get(key);
                    HashMap<String, Pair<String, String>> feedToTypeUpdate = blacklistedRecord.getSources();
                    if (ioCRecord.getFeed().getName() != null) {
                        feedToTypeUpdate.remove(ioCRecord.getFeed().getName());
                    } else {
                        log.log(Level.FINE, "removeFromCache: ioCRecord's feed was null.");
                    }
                    if (feedToTypeUpdate == null || feedToTypeUpdate.isEmpty()) {
                        // As soon as there are no feeds, we remove the IoC from the cache
                        blacklistCache.removeAsync(key);
                        //utx.commit();
                    } else {
                        blacklistedRecord.setSources(feedToTypeUpdate);
                        blacklistedRecord.setListed(Calendar.getInstance());
                        blacklistCache.replaceAsync(key, blacklistedRecord);
                        //utx.commit();
                    }
                }
            } catch (Exception e) {
                log.log(Level.SEVERE, "removeFromCache", e);
                /*try {
                    if (utx.getStatus() != javax.transaction.Status.STATUS_NO_TRANSACTION) {
                        utx.rollback();
                    }
                } catch (Exception e1) {
                    log.log(Level.SEVERE, "removeFromCache: Rolling back.", e1);
                    return false; //finally?
                }*/
                return false;
            }
        }
        return true;
    }

    /**
     * This is very evil.
     *
     * @return true if everything went well.
     */
    @Override
    public boolean dropTheWholeCache() {
        log.log(Level.SEVERE, "dropTheWholeCache: We are dropping the cache. This has severe operational implications.");
        try {
            //utx.begin();
            // TODO: Clear is faster, but apparently quite ugly. Investigate clearAsync().
            //blacklistCache.clear();
            // TODO: Could this handle millions of records in a dozen node cluster? :)
            blacklistCache.keySet().forEach(key -> blacklistCache.removeAsync(key));
            //utx.commit();
        } catch (Exception e) {
            log.log(Level.SEVERE, "dropTheWholeCache", e);
           /* try {
                if (utx.getStatus() != javax.transaction.Status.STATUS_NO_TRANSACTION) {
                    utx.rollback();
                }
            } catch (Exception e1) {
                log.log(Level.SEVERE, "dropTheWholeCache: Rolling back.", e1);
                return false; //finally?
            }*/
            return false;
        }
        return true;
    }
}
