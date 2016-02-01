package biz.karms.sinkit.rest;

import biz.karms.sinkit.ejb.DNSApi;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Michal Karm Babacek
 *         <p>
 *         TODO: Validation and filtering :-)
 */
@RequestScoped
public class DnsService implements Serializable {

    private static final long serialVersionUID = 4307452614798L;

    @EJB
    private DNSApi dnsApi;

    @Inject
    private Logger log;

    private static final Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

    String getSinkHole(final String client, final String key, String fqdn) {
        long start = System.currentTimeMillis();
        String returned = gson.toJson(dnsApi.getSinkHole(client, key, fqdn));
        log.log(Level.INFO, "getSinkHole took: " + (System.currentTimeMillis() - start) + " ms.");
        return returned;
    }
}
