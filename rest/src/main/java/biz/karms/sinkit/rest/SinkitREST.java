package biz.karms.sinkit.rest;

import biz.karms.sinkit.exception.ArchiveException;
import biz.karms.sinkit.exception.IoCValidationException;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.util.logging.Logger;

/**
 * @author Michal Karm Babacek
 */
@RequestScoped
@Path("/")
public class SinkitREST implements Serializable {

    private static final long serialVersionUID = -811275040019884876L;

    @Inject
    SinkitService sinkitService;

    @Inject
    private transient Logger log;

    @GET
    @Path("/hello/{name}")
    @Produces({"application/json;charset=UTF-8"})
    public String getHelloMessage(@PathParam("name") String name) {
        return sinkitService.createHelloMessage(name);
    }

    @GET
    @Path("/stats")
    @Produces({"application/json;charset=UTF-8"})
    public String getStats() {
        return sinkitService.getStats();
    }

    @GET
    @Path("/blacklist/record/{key}")
    @Produces({"application/json;charset=UTF-8"})
    public String getBlacklistedRecord(@PathParam("key") String key) {
        return sinkitService.getBlacklistedRecord(key);
    }

    @GET
    @Path("/blacklist/records")
    @Produces({"application/json;charset=UTF-8"})
    public String getBlacklistedRecordKeys() {
        return sinkitService.getBlacklistedRecordKeys();
    }

    @DELETE
    @Path("/blacklist/record/{key}")
    @Produces({"application/json;charset=UTF-8"})
    public String deleteBlacklistedRecord(@PathParam("key") String key) {
        return sinkitService.deleteBlacklistedRecord(key);
    }

    @POST
    @Path("/blacklist/accuracyupdate/")
    @Produces({"application/json;charset=UTF-8"})
    //@Consumes({"application/json;charset=UTF-8"})
    public Response updateAccuracy(String report) {
        try {
            sinkitService.updateAccuracy(report);
            return Response.status(Response.Status.OK).entity("Succeeded in updating with accuchecker report: " + report).build();
        } catch (ArchiveException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        } catch (JsonParseException ex) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ex.getMessage()).build();
        } catch (IoCValidationException ex) {

            return Response.status(Response.Status.BAD_REQUEST).entity(ex.getMessage()).build();
        }


    }

    @POST
    @Path("/blacklist/record/")
    @Produces({"application/json;charset=UTF-8"})
    //@Consumes({"application/json;charset=UTF-8"})
    public String putBlacklistedRecord(@FormParam("record") String record) {
        return sinkitService.putBlacklistedRecord(record);
    }

    @POST
    @Path("/blacklist/ioc/")
    @Produces({"application/json;charset=UTF-8"})
    //@Consumes({"application/json;charset=UTF-8"})
    public Response putIoCRecord(String ioc) {
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
    public Response putWhitelistIoCRecord(String ioc) {
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
    public String isWhitelistEmpty() {
        return sinkitService.isWhitelistEmpty();
    }

    @GET
    @Path("/whitelist/record/{key}")
    @Produces({"application/json;charset=UTF-8"})
    //@Consumes({"application/json;charset=UTF-8"})
    public String getWhitelistedRecord(@PathParam("key") String key) {
        return sinkitService.getWhitelistedRecord(key);
    }

    @DELETE
    @Path("/whitelist/record/{key}")
    @Produces({"application/json;charset=UTF-8"})
    //@Consumes({"application/json;charset=UTF-8"})
    public String removeWhitelistedRecord(@PathParam("key") String key) {
        return sinkitService.removeWhitelistedRecord(key);
    }

    @POST
    @Path("/rebuildCache/")
    @Produces({"application/json;charset=UTF-8"})
    public Response rebuildCache() {
        String response = sinkitService.runCacheRebuilding();
        return Response.status(Response.Status.OK).entity(response).build();
    }

    @GET
    @Path("/rules/{ip}")
    @Produces({"application/json;charset=UTF-8"})
    public String getRules(@PathParam("ip") String ip) {
        return sinkitService.getRules(ip);
    }

    @GET
    @Path("/rules/all")
    @Produces({"application/json;charset=UTF-8"})
    public String getAllRules() {
        return sinkitService.getAllRules();
    }

    /**
     * Rattus - PORTAL --> CORE
     */
    @PUT
    @Path("/rules/customer/{customerId}")
    @Produces({"application/json;charset=UTF-8"})
    //@Consumes({"application/json;charset=UTF-8"})
    public String putDNSClientSettings(@PathParam("customerId") Integer customerId, String settings) {
        return sinkitService.putDNSClientSettings(customerId, settings);
    }

    @DELETE
    @Path("/rules/customer/{customerId}")
    @Produces({"application/json;charset=UTF-8"})
    //@Consumes({"application/json;charset=UTF-8"})
    public String deleteDNSClientSettings(@PathParam("customerId") Integer customerId) {
        return sinkitService.deleteRulesByCustomer(customerId);
    }

    @POST
    @Path("/rules/all")
    @Produces({"application/json;charset=UTF-8"})
    //@Consumes({"application/json;charset=UTF-8"})
    public String postAllDNSClientSettings(String rules) {
        return sinkitService.postAllDNSClientSettings(rules);
    }


    @PUT
    @Path("/lists/{customerId}")
    @Produces({"application/json;charset=UTF-8"})
    //@Consumes({"application/json;charset=UTF-8"})
    public String putCustomLists(@PathParam("customerId") Integer customerId, String lists) {
        return sinkitService.putCustomLists(customerId, lists);
    }

    @PUT
    @Path("/feed/{feedUid}")
    @Produces({"application/json;charset=UTF-8"})
    //@Consumes({"application/json;charset=UTF-8"})
    public String putFeedSettings(@PathParam("feedUid") String feedUid, String settings) {
        return sinkitService.putFeedSettings(feedUid, settings);
    }

    @POST
    @Path("/feed/create")
    @Produces({"application/json;charset=UTF-8"})
    //@Consumes({"application/json;charset=UTF-8"})
    public String postCreateFeedSettings(String feed) {
        return sinkitService.postCreateFeedSettings(feed);
    }

    @POST
    @Path("/log/record")
    public String logRecrod(String logRecord) {
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
    public String enrich() {
        try {
            sinkitService.enrich();
            return "OK";
        } catch (Exception e) {
            e.printStackTrace();
            return "Not Ok";
        }
    }
}
