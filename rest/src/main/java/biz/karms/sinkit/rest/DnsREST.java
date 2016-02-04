package biz.karms.sinkit.rest;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

/**
 * @author Michal Karm Babacek
 *         <p>
 *         TODO: Validation :-)
 *         TODO: OAuth
 */
@RequestScoped
@Path("/blacklist/dns/")
public class DnsREST {

    @Inject
    DnsService dnsService;

    public static final String AUTH_HEADER_PARAM = "X-sinkit-token";
    public static final String AUTH_FAIL = "❤ AUTH ERROR ❤";

    /**
     * @param token  Access token
     * @param client IP address of the DNS client that queried resolver
     * @param key    IP address or FQDN that is to be checked for IoC match
     * @param fqdn   FQDN DNS client wanted to have resolved
     * @return
     */
    @GET
    @Path("/{client}/{key}/{fqdn}")
    @Produces({"application/json;charset=UTF-8"})
    public String getSinkHole(@HeaderParam(AUTH_HEADER_PARAM) String token, @PathParam("client") String client, @PathParam("key") String key, @PathParam("fqdn") String fqdn) {
        if (StupidAuthenticator.isAuthenticated(token)) {
            return dnsService.getSinkHole(client, key, fqdn);
        } else {
            return AUTH_FAIL;
        }
    }

    @GET
    @Path("/async/{client}/{key}/{fqdn}")
    @Produces({"application/json;charset=UTF-8"})
    public String getAsyncSinkHole(@HeaderParam(AUTH_HEADER_PARAM) String token, @PathParam("client") String client, @PathParam("key") String key, @PathParam("fqdn") String fqdn) {
        if (StupidAuthenticator.isAuthenticated(token)) {
            dnsService.getAsyncSinkHole(client, key, fqdn);
            // TODO: API FTW
            return "";
        } else {
            return AUTH_FAIL;
        }
    }
}
