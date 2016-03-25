package biz.karms.sinkit.ejb.virustotal;

import com.kanishka.virustotal.exception.APIKeyNotFoundException;
import com.kanishka.virustotalv2.VirustotalPublicV2;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

/**
 * Created by Tomas Kozel
 */
@Dependent
public class VirusTotalResources {

    @Inject
    VirusTotalClientProvider virusTotalClientProvider;

    /**
     * For more info about exception
     * @see VirusTotalClientProvider
     */
    @Produces
    @Default
    VirustotalPublicV2 getVirusTotalClient() throws APIKeyNotFoundException {
        return virusTotalClientProvider.getVirusTotalClient();
    }
}
