package biz.karms.sinkit.ejb;

import biz.karms.sinkit.ejb.cache.pojo.WhitelistedRecord;
import biz.karms.sinkit.ioc.IoCRecord;

import javax.ejb.Local;

/**
 * Created by tkozel on 1/9/16.
 */
@Local
public interface WhitelistCacheService {
    boolean put(final IoCRecord iocRecord);

    WhitelistedRecord get(final IoCRecord iocRecord);

    boolean dropTheWholeCache();
}
