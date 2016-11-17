package biz.karms.sinkit.rest;

import biz.karms.sinkit.ejb.DNSApi;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.infinispan.util.concurrent.TimeoutException;

import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.EJBException;
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

    private static final long serialVersionUID = 4307429873614798L;

    @EJB(beanInterface = DNSApi.class)
    private DNSApi dnsApi;

    @Inject
    private Logger log;

    private static final Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

    String getSinkHole(final String client, final String key, final String fqdn) {
        long start = System.currentTimeMillis();
        //TODO: Define the protocol already...
        String returned = "null";
        try {
            returned = gson.toJson(dnsApi.getSinkHole(client, key, fqdn));
        } catch (TimeoutException | EJBException e) {
            log.log(Level.SEVERE, "getSinkHole went south:", e);
        }
        log.log(Level.FINE, "getSinkHole took: " + (System.currentTimeMillis() - start) + " ms.");
        return returned;
    }

    @Asynchronous
    void getAsyncSinkHole(final String client, final String key, final String fqdn) {
        getSinkHole(client, key, fqdn);
    }
}
