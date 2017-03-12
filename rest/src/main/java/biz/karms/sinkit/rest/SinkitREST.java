package biz.karms.sinkit.rest;

import biz.karms.sinkit.exception.IoCValidationException;
import com.google.gson.JsonSyntaxException;

import javax.enterprise.context.SessionScoped;
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
 */
@SessionScoped
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
        return sinkitService.createHelloMessage(name);
    }

    @GET
    @Path("/stats")
    @Produces({"application/json;charset=UTF-8"})
    public String getStats(@HeaderParam(AUTH_HEADER_PARAM) String token) {
        return sinkitService.getStats();
    }

    @GET
    @Path("/blacklist/record/{key}")
    @Produces({"application/json;charset=UTF-8"})
    public String getBlacklistedRecord(@HeaderParam(AUTH_HEADER_PARAM) String token, @PathParam("key") String key) {
        return sinkitService.getBlacklistedRecord(key);
    }

    @GET
    @Path("/blacklist/records")
    @Produces({"application/json;charset=UTF-8"})
    public String getBlacklistedRecordKeys(@HeaderParam(AUTH_HEADER_PARAM) String token) {
        return sinkitService.getBlacklistedRecordKeys();
    }

    @DELETE
    @Path("/blacklist/record/{key}")
    @Produces({"application/json;charset=UTF-8"})
    public String deleteBlacklistedRecord(@HeaderParam(AUTH_HEADER_PARAM) String token, @PathParam("key") String key) {
        return sinkitService.deleteBlacklistedRecord(key);
    }

    @POST
    @Path("/blacklist/record/")
    @Produces({"application/json;charset=UTF-8"})
    //@Consumes({"application/json;charset=UTF-8"})
    public String putBlacklistedRecord(@HeaderParam(AUTH_HEADER_PARAM) String token, @FormParam("record") String record) {
        return sinkitService.putBlacklistedRecord(record);
    }

    @POST
    @Path("/blacklist/ioc/")
    @Produces({"application/json;charset=UTF-8"})
    //@Consumes({"application/json;charset=UTF-8"})
    public Response putIoCRecord(@HeaderParam(AUTH_HEADER_PARAM) String token, String ioc) {
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
    @Path("/whitelist/ioc/")
    @Produces({"application/json;charset=UTF-8"})
    //@Consumes({"application/json;charset=UTF-8"})
    public Response putWhitelistIoCRecord(@HeaderParam(AUTH_HEADER_PARAM) String token, String ioc) {
        try {
            String response = sinkitService.processWhitelistIoCRecord(ioc);
            return Response.status(Response.Status.OK).entity(response).build();
        } catch (IoCValidationException | JsonSyntaxException ex) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ex.getMessage() + " JSON was:" + ioc).build();
        } catch (Exception ex) {
            log.severe("Processiong whitelist entry went wrong: " + ex.getMessage());
            log.severe("IoC: " + ioc);
            ex.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }

    @GET
    @Path("/whitelist/isempty/")
    @Produces({"application/json;charset=UTF-8"})
    //@Consumes({"application/json;charset=UTF-8"})
    public String isWhitelistEmpty(@HeaderParam(AUTH_HEADER_PARAM) String token) {
        return sinkitService.isWhitelistEmpty();
    }

    @GET
    @Path("/whitelist/record/{key}")
    @Produces({"application/json;charset=UTF-8"})
    //@Consumes({"application/json;charset=UTF-8"})
    public String getWhitelistedRecord(@HeaderParam(AUTH_HEADER_PARAM) String token, @PathParam("key") String key) {
        return sinkitService.getWhitelistedRecord(key);
    }

    @DELETE
    @Path("/whitelist/record/{key}")
    @Produces({"application/json;charset=UTF-8"})
    //@Consumes({"application/json;charset=UTF-8"})
    public String removeWhitelistedRecord(@HeaderParam(AUTH_HEADER_PARAM) String token, @PathParam("key") String key) {
        return sinkitService.getWhitelistedRecord(key);
    }

    @POST
    @Path("/rebuildCache/")
    @Produces({"application/json;charset=UTF-8"})
    public Response rebuildCache(@HeaderParam(AUTH_HEADER_PARAM) String token) {
        String response = sinkitService.runCacheRebuilding();
        return Response.status(Response.Status.OK).entity(response).build();
    }

    @GET
    @Path("/rules/{ip}")
    @Produces({"application/json;charset=UTF-8"})
    public String getRules(@HeaderParam(AUTH_HEADER_PARAM) String token, @PathParam("ip") String ip) {
        return sinkitService.getRules(ip);
    }

    @GET
    @Path("/rules/all")
    @Produces({"application/json;charset=UTF-8"})
    public String getAllRules(@HeaderParam(AUTH_HEADER_PARAM) String token) {
        return sinkitService.getAllRules();
    }

    /**
     * Rattus - PORTAL --> CORE
     */
    @PUT
    @Path("/rules/customer/{customerId}")
    @Produces({"application/json;charset=UTF-8"})
    //@Consumes({"application/json;charset=UTF-8"})
    public String putDNSClientSettings(@HeaderParam(AUTH_HEADER_PARAM) String token, @PathParam("customerId") Integer customerId, String settings) {
        return sinkitService.putDNSClientSettings(customerId, settings);
    }

    @DELETE
    @Path("/rules/customer/{customerId}")
    @Produces({"application/json;charset=UTF-8"})
    //@Consumes({"application/json;charset=UTF-8"})
    public String deleteDNSClientSettings(@HeaderParam(AUTH_HEADER_PARAM) String token, @PathParam("customerId") Integer customerId) {
        return sinkitService.deleteRulesByCustomer(customerId);
    }

    @POST
    @Path("/rules/all")
    @Produces({"application/json;charset=UTF-8"})
    //@Consumes({"application/json;charset=UTF-8"})
    public String postAllDNSClientSettings(@HeaderParam(AUTH_HEADER_PARAM) String token, String rules) {
        return sinkitService.postAllDNSClientSettings(rules);
    }


    @PUT
    @Path("/lists/{customerId}")
    @Produces({"application/json;charset=UTF-8"})
    //@Consumes({"application/json;charset=UTF-8"})
    public String putCustomLists(@HeaderParam(AUTH_HEADER_PARAM) String token, @PathParam("customerId") Integer customerId, String lists) {
        return sinkitService.putCustomLists(customerId, lists);
    }

    @PUT
    @Path("/feed/{feedUid}")
    @Produces({"application/json;charset=UTF-8"})
    //@Consumes({"application/json;charset=UTF-8"})
    public String putFeedSettings(@HeaderParam(AUTH_HEADER_PARAM) String token, @PathParam("feedUid") String feedUid, String settings) {
        return sinkitService.putFeedSettings(feedUid, settings);
    }

    @POST
    @Path("/feed/create")
    @Produces({"application/json;charset=UTF-8"})
    //@Consumes({"application/json;charset=UTF-8"})
    public String postCreateFeedSettings(@HeaderParam(AUTH_HEADER_PARAM) String token, String feed) {
        return sinkitService.postCreateFeedSettings(feed);
    }

    @POST
    @Path("/log/record")
    public String logRecrod(@HeaderParam(AUTH_HEADER_PARAM) String token, String logRecord) {
        try {
            sinkitService.addEventLogRecord(logRecord);
            return "OK";
        } catch (Exception e) {
            e.printStackTrace();
            return "Not Ok";
        }
    }

    @POST
    @Path("/total/enrich")
    public String enrich(@HeaderParam(AUTH_HEADER_PARAM) String token) {
        try {
            sinkitService.enrich();
            return "OK";
        } catch (Exception e) {
            e.printStackTrace();
            return "Not Ok";
        }
    }

    @PUT
    @Path("/gsb/{hashPrefix}")
    @Produces({"application/json;charset=UTF-8"})
    //@Consumes({"application/json;charset=UTF-8"})
    public Response putGSBHashRecord(@HeaderParam(AUTH_HEADER_PARAM) String token, @PathParam("hashPrefix") String hashPrefix) {
        try {
            boolean response = sinkitService.putGSBHashPrefix(hashPrefix);
            Response.Status status;
            if (response) {
                status = Response.Status.OK;
            } else {
                status = Response.Status.INTERNAL_SERVER_ERROR;
            }
            return Response.status(status).entity(response).build();
        } catch (Exception ex) {
            log.severe("putGSBHashRecord: Adding hash prefix " + hashPrefix + " to GSB cache went wrong: " + ex.getMessage());
            ex.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }

    @DELETE
    @Path("/gsb/{hashPrefix}")
    @Produces({"application/json;charset=UTF-8"})
    //@Consumes({"application/json;charset=UTF-8"})
    public Response removeGSBHashRecord(@HeaderParam(AUTH_HEADER_PARAM) String token, @PathParam("hashPrefix") String hashPrefix) {
        try {
            boolean response = sinkitService.removeGSBHashPrefix(hashPrefix);
            Response.Status status;
            if (response) {
                status = Response.Status.OK;
            } else {
                status = Response.Status.INTERNAL_SERVER_ERROR;
            }
            return Response.status(status).entity(response).build();
        } catch (Exception ex) {
            log.severe("removeGSBHashRecord: Removing hash prefix " + hashPrefix + " to GSB cache went wrong: " + ex.getMessage());
            ex.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }

    @GET
    @Path("/gsb/stats")
    @Produces({"application/json;charset=UTF-8"})
    public String getGSBStats(@HeaderParam(AUTH_HEADER_PARAM) String token) {
        return sinkitService.getGSBStats();
    }

    @GET
    @Path("/gsb/lookup/{url}")
    @Produces({"application/json;charset=UTF-8"})
    public String gsbLookup(@HeaderParam(AUTH_HEADER_PARAM) String token, @PathParam("url") String url) {
        return sinkitService.gsbLookup(url);
    }

    @DELETE
    @Path("/gsb")
    @Produces({"application/json;charset=UTF-8"})
    public String gsbClearCache(@HeaderParam(AUTH_HEADER_PARAM) String token) {
        return sinkitService.clearGSBCache();
    }
}
