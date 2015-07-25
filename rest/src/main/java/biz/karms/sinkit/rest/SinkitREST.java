package biz.karms.sinkit.rest;

import biz.karms.sinkit.exception.IoCValidationException;
import biz.karms.sinkit.ioc.*;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

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
    private Logger log;

    @Inject
    StupidAuthenticator stupidAuthenticator;
    
    public static final String AUTH_HEADER_PARAM = "X-sinkit-token";
    public static final String AUTH_FAIL = "❤ AUTH ERROR ❤";

    @GET
    @Path("/hello/{name}")
    @Produces({"application/json;charset=UTF-8"})
    public String getHelloMessage(@HeaderParam(AUTH_HEADER_PARAM) String token, @PathParam("name") String name) {
        if (stupidAuthenticator.isAuthenticated(token)) {
            return sinkitService.createHelloMessage(name);
        } else {
            return AUTH_FAIL;
        }
    }

    @GET
    @Path("/stats")
    @Produces({"application/json;charset=UTF-8"})
    public String getStats(@HeaderParam(AUTH_HEADER_PARAM) String token) {
        if (stupidAuthenticator.isAuthenticated(token)) {
            return sinkitService.getStats();
        } else {
            return AUTH_FAIL;
        }
    }

    @GET
    @Path("/blacklist/dns/{client}/{key}")
    @Produces({"application/json;charset=UTF-8"})
    public String getSinkHole(@HeaderParam(AUTH_HEADER_PARAM) String token, @PathParam("client") String client, @PathParam("key") String key) {
        if (stupidAuthenticator.isAuthenticated(token)) {
            return sinkitService.getSinkHole(client, key);
        } else {
            return AUTH_FAIL;
        }
    }

    @GET
    @Path("/blacklist/record/{key}")
    @Produces({"application/json;charset=UTF-8"})
    public String getBlacklistedRecord(@HeaderParam(AUTH_HEADER_PARAM) String token, @PathParam("key") String key) {
        if (stupidAuthenticator.isAuthenticated(token)) {
            return sinkitService.getBlacklistedRecord(key);
        } else {
            return AUTH_FAIL;
        }
    }

    @GET
    @Path("/blacklist/records")
    @Produces({"application/json;charset=UTF-8"})
    public String getBlacklistedRecordKeys(@HeaderParam(AUTH_HEADER_PARAM) String token) {
        if (stupidAuthenticator.isAuthenticated(token)) {
            return sinkitService.getBlacklistedRecordKeys();
        } else {
            return AUTH_FAIL;
        }
    }

    @DELETE
    @Path("/blacklist/record/{key}")
    @Produces({"application/json;charset=UTF-8"})
    public String deleteBlacklistedRecord(@HeaderParam(AUTH_HEADER_PARAM) String token, @PathParam("key") String key) {
        if (stupidAuthenticator.isAuthenticated(token)) {
            return sinkitService.deleteBlacklistedRecord(key);
        } else {
            return AUTH_FAIL;
        }
    }

    @POST
    @Path("/blacklist/record/")
    @Produces({"application/json;charset=UTF-8"})
    public String putBlacklistedRecord(@HeaderParam(AUTH_HEADER_PARAM) String token, @FormParam("record") String record) {
        if (stupidAuthenticator.isAuthenticated(token)) {
            return sinkitService.putBlacklistedRecord(record);
        } else {
            return AUTH_FAIL;
        }
    }

    @POST
    @Path("/blacklist/ioc/")
    @Produces({"application/json;charset=UTF-8"})
    public Response putIoCRecord(@HeaderParam(AUTH_HEADER_PARAM) String token, String ioc) {

        if (!stupidAuthenticator.isAuthenticated(token)) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(AUTH_FAIL).build();
        }

        try {
            String response = sinkitService.processIoCRecord(ioc);
            return Response.status(Response.Status.OK).entity(response).build();
        } catch (IoCValidationException | JsonSyntaxException ex) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ex.getMessage()).build();
        } catch (Exception ex) {
            log.severe("Processiong IoC went wrong: " + ex.getMessage());
            log.severe("IoC: " + ioc);
            ex.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }

    /**
     * Rules
     */
    @POST
    @Path("/rules/rule/")
    @Produces({"application/json;charset=UTF-8"})
    public String putRule(@HeaderParam(AUTH_HEADER_PARAM) String token, @FormParam("rule") String rule) {
        if (stupidAuthenticator.isAuthenticated(token)) {
            return sinkitService.putRule(rule);
        } else {
            return AUTH_FAIL;
        }
    }

    @GET
    @Path("/rules/{ip}")
    @Produces({"application/json;charset=UTF-8"})
    public String getRules(@HeaderParam(AUTH_HEADER_PARAM) String token, @PathParam("ip") String ip) {
        if (stupidAuthenticator.isAuthenticated(token)) {
            return sinkitService.getRules(ip);
        } else {
            return AUTH_FAIL;
        }
    }
}
