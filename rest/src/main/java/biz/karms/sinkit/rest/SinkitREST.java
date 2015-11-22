package biz.karms.sinkit.rest;

import biz.karms.sinkit.exception.IoCValidationException;
import com.google.gson.JsonSyntaxException;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

/**
 * @author Michal Karm Babacek
 *         <p>
 *         TODO: Validation :-)
 *         TODO: OAuth
 */
@Path("/")
public class SinkitREST {

    @Inject
    SinkitService sinkitService;

    @Inject
    private Logger log;

    public static final String AUTH_HEADER_PARAM = "X-sinkit-token";
    public static final String AUTH_FAIL = "❤ AUTH ERROR ❤";

    @GET
    @Path("/hello/{name}")
    @Produces({"application/json;charset=UTF-8"})
    public String getHelloMessage(@HeaderParam(AUTH_HEADER_PARAM) String token, @PathParam("name") String name) {
        if (StupidAuthenticator.isAuthenticated(token)) {
            return sinkitService.createHelloMessage(name);
        } else {
            return AUTH_FAIL;
        }
    }

    @GET
    @Path("/stats")
    @Produces({"application/json;charset=UTF-8"})
    public String getStats(@HeaderParam(AUTH_HEADER_PARAM) String token) {
        if (StupidAuthenticator.isAuthenticated(token)) {
            return sinkitService.getStats();
        } else {
            return AUTH_FAIL;
        }
    }

    @GET
    @Path("/blacklist/dns/{client}/{key}")
    @Produces({"application/json;charset=UTF-8"})
    public String getSinkHole(@HeaderParam(AUTH_HEADER_PARAM) String token, @PathParam("client") String client, @PathParam("key") String key) {
        if (StupidAuthenticator.isAuthenticated(token)) {
            return sinkitService.getSinkHole(client, key);
        } else {
            return AUTH_FAIL;
        }
    }

    @GET
    @Path("/blacklist/record/{key}")
    @Produces({"application/json;charset=UTF-8"})
    public String getBlacklistedRecord(@HeaderParam(AUTH_HEADER_PARAM) String token, @PathParam("key") String key) {
        if (StupidAuthenticator.isAuthenticated(token)) {
            return sinkitService.getBlacklistedRecord(key);
        } else {
            return AUTH_FAIL;
        }
    }

    @GET
    @Path("/blacklist/records")
    @Produces({"application/json;charset=UTF-8"})
    public String getBlacklistedRecordKeys(@HeaderParam(AUTH_HEADER_PARAM) String token) {
        if (StupidAuthenticator.isAuthenticated(token)) {
            return sinkitService.getBlacklistedRecordKeys();
        } else {
            return AUTH_FAIL;
        }
    }

    @DELETE
    @Path("/blacklist/record/{key}")
    @Produces({"application/json;charset=UTF-8"})
    public String deleteBlacklistedRecord(@HeaderParam(AUTH_HEADER_PARAM) String token, @PathParam("key") String key) {
        if (StupidAuthenticator.isAuthenticated(token)) {
            return sinkitService.deleteBlacklistedRecord(key);
        } else {
            return AUTH_FAIL;
        }
    }

    @POST
    @Path("/blacklist/record/")
    @Produces({"application/json;charset=UTF-8"})
    //@Consumes({"application/json;charset=UTF-8"})
    public String putBlacklistedRecord(@HeaderParam(AUTH_HEADER_PARAM) String token, @FormParam("record") String record) {
        if (StupidAuthenticator.isAuthenticated(token)) {
            return sinkitService.putBlacklistedRecord(record);
        } else {
            return AUTH_FAIL;
        }
    }

    @POST
    @Path("/blacklist/ioc/")
    @Produces({"application/json;charset=UTF-8"})
    //@Consumes({"application/json;charset=UTF-8"})
    public Response putIoCRecord(@HeaderParam(AUTH_HEADER_PARAM) String token, String ioc) {
        if (!StupidAuthenticator.isAuthenticated(token)) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(AUTH_FAIL).build();
        }
        try {
            String response = sinkitService.processIoCRecord(ioc);
            return Response.status(Response.Status.OK).entity(response).build();
        } catch (IoCValidationException | JsonSyntaxException ex) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ex.getMessage() + " JSON was:" + ioc).build();
        } catch (Exception ex) {
            log.severe("Processiong IoC went wrong: " + ex.getMessage());
            log.severe("IoC: " + ioc);
            ex.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }

    @POST
    @Path("/rebuildCache/")
    @Produces({"application/json;charset=UTF-8"})
    public Response rebuildCache(@HeaderParam(AUTH_HEADER_PARAM) String token) {

        if (!StupidAuthenticator.isAuthenticated(token)) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(AUTH_FAIL).build();
        }

        String response = sinkitService.runCacheRebuilding();
        return Response.status(Response.Status.OK).entity(response).build();
    }

    @GET
    @Path("/rules/{ip}")
    @Produces({"application/json;charset=UTF-8"})
    public String getRules(@HeaderParam(AUTH_HEADER_PARAM) String token, @PathParam("ip") String ip) {
        if (StupidAuthenticator.isAuthenticated(token)) {
            return sinkitService.getRules(ip);
        } else {
            return AUTH_FAIL;
        }
    }

    @GET
    @Path("/rules/all")
    @Produces({"application/json;charset=UTF-8"})
    public String getAllRules(@HeaderParam(AUTH_HEADER_PARAM) String token) {
        if (StupidAuthenticator.isAuthenticated(token)) {
            return sinkitService.getAllRules();
        } else {
            return AUTH_FAIL;
        }
    }

    /**
     * Rattus - PORTAL --> CORE
     */
    @PUT
    @Path("/rules/customer/{customerId}")
    @Produces({"application/json;charset=UTF-8"})
    //@Consumes({"application/json;charset=UTF-8"})
    public String putDNSClientSettings(@HeaderParam(AUTH_HEADER_PARAM) String token, @PathParam("customerId") Integer customerId, String settings) {
        if (StupidAuthenticator.isAuthenticated(token)) {
            return sinkitService.putDNSClientSettings(customerId, settings);
        } else {
            return AUTH_FAIL;
        }
    }

    @DELETE
    @Path("/rules/customer/{customerId}")
    @Produces({"application/json;charset=UTF-8"})
    //@Consumes({"application/json;charset=UTF-8"})
    public String deleteDNSClientSettings(@HeaderParam(AUTH_HEADER_PARAM) String token, @PathParam("customerId") Integer customerId) {
        if (StupidAuthenticator.isAuthenticated(token)) {
            return sinkitService.deleteRulesByCustomer(customerId);
        } else {
            return AUTH_FAIL;
        }
    }

    @POST
    @Path("/rules/all")
    @Produces({"application/json;charset=UTF-8"})
    //@Consumes({"application/json;charset=UTF-8"})
    public String postAllDNSClientSettings(@HeaderParam(AUTH_HEADER_PARAM) String token, String rules) {
        if (StupidAuthenticator.isAuthenticated(token)) {
            return sinkitService.postAllDNSClientSettings(rules);
        } else {
            return AUTH_FAIL;
        }
    }


    @PUT
    @Path("/lists/{customerId}")
    @Produces({"application/json;charset=UTF-8"})
    //@Consumes({"application/json;charset=UTF-8"})
    public String putCustomLists(@HeaderParam(AUTH_HEADER_PARAM) String token, @PathParam("customerId") Integer customerId, String lists) {
        if (StupidAuthenticator.isAuthenticated(token)) {
            return sinkitService.putCustomLists(customerId, lists);
        } else {
            return AUTH_FAIL;
        }
    }

    @PUT
    @Path("/feed/{feedUid}")
    @Produces({"application/json;charset=UTF-8"})
    //@Consumes({"application/json;charset=UTF-8"})
    public String putFeedSettings(@HeaderParam(AUTH_HEADER_PARAM) String token, @PathParam("feedUid") String feedUid, String settings) {
        if (StupidAuthenticator.isAuthenticated(token)) {
            return sinkitService.putFeedSettings(feedUid, settings);
        } else {
            return AUTH_FAIL;
        }
    }

    @POST
    @Path("/feed/create")
    @Produces({"application/json;charset=UTF-8"})
    //@Consumes({"application/json;charset=UTF-8"})
    public String postCreateFeedSettings(@HeaderParam(AUTH_HEADER_PARAM) String token, String feed) {
        if (StupidAuthenticator.isAuthenticated(token)) {
            return sinkitService.postCreateFeedSettings(feed);
        } else {
            return AUTH_FAIL;
        }
    }

    @POST
    @Path("/log/record")
    public String logRecrod(@HeaderParam(AUTH_HEADER_PARAM) String token, String logRecord) {
        if (StupidAuthenticator.isAuthenticated(token)) {
            try {
                sinkitService.addEventLogRecord(logRecord);
                return "OK";
            } catch (Exception e) {
                e.printStackTrace();
                return "Not Ok";
            }
        } else {
            return AUTH_FAIL;
        }
    }

    @POST
    @Path("/total/enrich")
    public String enrich(@HeaderParam(AUTH_HEADER_PARAM) String token) {
        if (StupidAuthenticator.isAuthenticated(token)) {
            try {
                sinkitService.enrich();
                return "OK";
            } catch (Exception e) {
                e.printStackTrace();
                return "Not Ok";
            }
        } else {
            return AUTH_FAIL;
        }
    }
}
