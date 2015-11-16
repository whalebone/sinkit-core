package biz.karms.sinkit.ejb;

import javax.ejb.ConcurrentAccessException;
import javax.ejb.Local;
import java.util.concurrent.Future;

/**
 * @author Michal Karm Babacek
 */
@Local
public interface CacheBuilder {
    boolean isCacheRebuildRunning();

    Future<Integer> runCacheRebuilding() throws ConcurrentAccessException;
}
