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
 * Created by tkozel on 31.7.15.
 */
@ApplicationScoped
@Singleton
public class VirusTotalClientProvider {

    //TODO move API_KEY TO env_prop
    private static final String API_KEY = "6cdef8aad7ef5a4c8b0540c8c835bdace5ab288f50af555a77c5538189845227";

    @Inject
    private Logger log;

    private VirustotalPublicV2 virusTotalClient;

    /**
     * returns (with lazy initialization) total virus client
     *
     * Exception is thrown by new VirustotalPublicV2Impl() only if api key is not set
     * but VirusTotalConfig.getConfigInstance().setVirusTotalAPIKey(API_KEY) is called before
     * so the exception can never be thrown
     *
     * @return VirustotalPublicV2 Total Virus client
     * @throws APIKeyNotFoundException
     */
    public VirustotalPublicV2 getVirusTotalClient() throws APIKeyNotFoundException {

        if (virusTotalClient == null) {

            log.info("\n\n Virus Total client does not exist - constructing a new one\n\n");

            VirusTotalConfig.getConfigInstance().setVirusTotalAPIKey(API_KEY);
            virusTotalClient = new VirustotalPublicV2Impl();
        }

        return virusTotalClient;
    }
}
