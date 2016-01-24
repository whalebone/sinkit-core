package biz.karms.sinkit.ejb.gsb.impl;

import biz.karms.sinkit.ejb.gsb.GSBClient;
import biz.karms.sinkit.ejb.gsb.dto.FullHashLookupResponse;
import biz.karms.sinkit.ejb.gsb.util.FullHashLookupResponseReader;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.util.logging.Logger;

/**
 * Created by tom on 12/5/15.
 *
 * @author Tomas Kozel
 */
@Stateless
public class GSBClientEJB implements GSBClient {

    @Inject
    private Logger logger;

    public static final String FULL_HASH_URL_ENV = "SINKIT_GSB_FULLHASH_URL";
    public static final String API_KEY_ENV = "SINKIT_GSB_API_KEY";

    public static String FULL_HASH_URL = "https://safebrowsing.google.com/safebrowsing/gethash";
    public static final String CLIENT = "api";
    public static final String APPVER = "1.0";
    public static final String PVER = "3.0";

    private String fullHashUrl;

    @PostConstruct
    public void setUp() {
        fullHashUrl = System.getenv(FULL_HASH_URL_ENV);
        if (fullHashUrl == null) {
            fullHashUrl = FULL_HASH_URL;
        }
    }

    @Override
    public FullHashLookupResponse getFullHashes(byte[] hashPrefix) {

        byte[] request = ArrayUtils.addAll("4:4\n".getBytes(), hashPrefix);
        Client client = ClientBuilder.newClient().register(FullHashLookupResponseReader.class);
        return client.target(fullHashUrl)
                .queryParam("client", CLIENT)
                .queryParam("key", System.getenv(API_KEY_ENV))
                .queryParam("appver", APPVER)
                .queryParam("pver", PVER)
                .request()
                .accept(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                .post(Entity.entity(request, MediaType.APPLICATION_OCTET_STREAM_TYPE), FullHashLookupResponse.class);
    }
}
