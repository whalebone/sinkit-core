package biz.karms.sinkit.ejb;

import biz.karms.sinkit.ejb.cache.pojo.WhitelistedRecord;
import biz.karms.sinkit.ioc.IoCRecord;

import javax.ejb.Local;

/**
 * Created by tkozel on 1/9/16.
 */
@Local
public interface WhitelistCacheService {
    WhitelistedRecord put(final IoCRecord iocRecord, boolean completed);

    WhitelistedRecord setCompleted(WhitelistedRecord partialWhite);

    WhitelistedRecord get(String id);

    boolean remove(String id);

    int getStats();

    boolean dropTheWholeCache();

}
