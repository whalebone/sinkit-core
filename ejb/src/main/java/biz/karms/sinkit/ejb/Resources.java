package biz.karms.sinkit.ejb;

import biz.karms.sinkit.ejb.elastic.JestClientProvider;
import biz.karms.sinkit.ejb.virustotal.VirusTotalClientProvider;
import com.kanishka.virustotal.exception.APIKeyNotFoundException;
import com.kanishka.virustotalv2.VirustotalPublicV2;
import io.searchbox.client.JestClient;
import org.infinispan.manager.DefaultCacheManager;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import java.util.logging.Logger;

/**
 * @author Michal Karm Babacek
 */
@Dependent
public class Resources {

    @Inject
    MyCacheManagerProvider cacheManagerProvider;

    @Produces
    @Default
    Logger getLogger(InjectionPoint ip) {
        String category = ip.getMember().getDeclaringClass().getName();
        return Logger.getLogger(category);
    }

    @Produces
    @Default
    DefaultCacheManager getDefaultCacheManager() {
        return cacheManagerProvider.getCacheManager();
    }
}
