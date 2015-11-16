package biz.karms.sinkit.ejb;

import biz.karms.sinkit.ioc.IoCRecord;

import javax.ejb.Local;

/**
 * @author Michal Karm Babacek
 */
@Local
public interface CacheService {
    boolean addToCache(final IoCRecord ioCRecord);

    boolean removeFromCache(final IoCRecord ioCRecord);

    boolean dropTheWholeCache();
}
