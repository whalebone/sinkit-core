package biz.karms.sinkit.ejb.virustotal;

import com.kanishka.virustotal.exception.APIKeyNotFoundException;
import com.kanishka.virustotalv2.VirusTotalConfig;
import com.kanishka.virustotalv2.VirustotalPublicV2;
import com.kanishka.virustotalv2.VirustotalPublicV2Impl;

import javax.ejb.Singleton;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.logging.Logger;

/**
 * @author Tomas Kozel
 */
@ApplicationScoped
public class VirusTotalClientProvider {

    @Inject
    private Logger log;

    private VirustotalPublicV2 virusTotalClient;

    /**
     * returns (with lazy initialization) total virus client
     *
     * Exception is thrown by new VirustotalPublicV2Impl() only if api key is not set
     *
     * @return VirustotalPublicV2 Total Virus client
     * @throws APIKeyNotFoundException
     */
    public VirustotalPublicV2 getVirusTotalClient() throws APIKeyNotFoundException {

        if (virusTotalClient == null) {

            log.info("\n\n Virus Total client does not exist - constructing a new one\n\n");

            VirusTotalConfig.getConfigInstance().setVirusTotalAPIKey(System.getenv("SINKIT_VIRUS_TOTAL_API_KEY"));
            virusTotalClient = new VirustotalPublicV2Impl();
        }

        return virusTotalClient;
    }
}
