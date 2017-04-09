package biz.karms.sinkit.rest;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import java.io.Serializable;

/**
 * @author Michal Karm Babacek
 */
@RequestScoped
@Path("/blacklist/dns/")
public class DnsREST implements Serializable {

    private static final long serialVersionUID = -8239140250391025545L;

    @Inject
    private DnsService dnsService;

    public static final String CLIENT_ID_HEADER_PARAM = "X-client-id";

    /**
     * @param client IP address of the DNS client that queried resolver
     * @param key    IP address or FQDN that is to be checked for IoC match
     * @param fqdn   FQDN DNS client wanted to have resolved
     * @return
     */
    @GET
    @Path("/{client}/{key}/{fqdn}")
    @Produces({"application/json;charset=UTF-8"})
    public String getSinkHole(
            @HeaderParam(CLIENT_ID_HEADER_PARAM) Integer clientId,
            @PathParam("client") String client,
            @PathParam("key") String key,
            @PathParam("fqdn") String fqdn) {
        return dnsService.getSinkHole(client, key, fqdn, clientId);
    }

    @GET
    @Path("/async/{client}/{key}/{fqdn}")
    @Produces({"application/json;charset=UTF-8"})
    public String getAsyncSinkHole(
            @HeaderParam(CLIENT_ID_HEADER_PARAM) Integer clientId,
            @PathParam("client") String client,
            @PathParam("key") String key,
            @PathParam("fqdn") String fqdn) {
        dnsService.getAsyncSinkHole(client, key, fqdn, clientId);
        // TODO: API FTW
        return "";
    }
}
