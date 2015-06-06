package biz.karms.sinkit.rest;

import javax.inject.Inject;
import javax.ws.rs.*;

/**
 * @author Michal Karm Babacek
 *         <p/>
 *         TODO: Validation :-)
 *         TODO: OAuth
 */
@Path("/")
public class SinkitREST {

    @Inject
    SinkitService sinkitService;

    @Inject
    StupidAuthenticator stupidAuthenticator;

    public static final String AUTH_FAIL = "❤ AUTH ERROR ❤";

    @GET
    @Path("/hello/{name}")
    @Produces({"application/json;charset=UTF-8"})
    public String getHelloMessage(@HeaderParam("X-sinkit-token") String token, @PathParam("name") String name) {
        if (stupidAuthenticator.isAuthenticated(token)) {
            return sinkitService.createHelloMessage(name);
        } else {
            return AUTH_FAIL;
        }
    }

    @GET
    @Path("/stats")
    @Produces({"application/json;charset=UTF-8"})
    public String getStats(@HeaderParam("X-sinkit-token") String token) {
        if (stupidAuthenticator.isAuthenticated(token)) {
            return sinkitService.getStats();
        } else {
            return AUTH_FAIL;
        }
    }

    @GET
    @Path("/blacklist/record/{key}")
    @Produces({"application/json;charset=UTF-8"})
    public String getBlacklistedRecord(@HeaderParam("X-sinkit-token") String token, @PathParam("key") String key) {
        if (stupidAuthenticator.isAuthenticated(token)) {
            return sinkitService.getBlacklistedRecord(key);
        } else {
            return AUTH_FAIL;
        }
    }

    @GET
    @Path("/blacklist/records")
    @Produces({"application/json;charset=UTF-8"})
    public String getBlacklistedRecordKeys(@HeaderParam("X-sinkit-token") String token) {
        if (stupidAuthenticator.isAuthenticated(token)) {
            return sinkitService.getBlacklistedRecordKeys();
        } else {
            return AUTH_FAIL;
        }
    }

    @DELETE
    @Path("/blacklist/record/{key}")
    @Produces({"application/json;charset=UTF-8"})
    public String deleteBlacklistedRecord(@HeaderParam("X-sinkit-token") String token, @PathParam("key") String key) {
        if (stupidAuthenticator.isAuthenticated(token)) {
            return sinkitService.deleteBlacklistedRecord(key);
        } else {
            return AUTH_FAIL;
        }
    }

    @POST
    @Path("/blacklist/record/")
    @Produces({"application/json;charset=UTF-8"})
    public String putBlacklistedRecord(@HeaderParam("X-sinkit-token") String token, @FormParam("record") String record) {
        if (stupidAuthenticator.isAuthenticated(token)) {
            return sinkitService.putBlacklistedRecord(record);
        } else {
            return AUTH_FAIL;
        }
    }

}
