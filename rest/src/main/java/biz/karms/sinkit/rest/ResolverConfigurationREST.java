package biz.karms.sinkit.rest;

import biz.karms.sinkit.ejb.WebApi;
import biz.karms.sinkit.exception.EndUserConfigurationValidationException;
import biz.karms.sinkit.exception.ResolverConfigurationValidationException;
import biz.karms.sinkit.resolver.EndUserConfiguration;
import biz.karms.sinkit.resolver.ResolverConfiguration;

import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.logging.Logger;

@RequestScoped
@Path("/")
public class ResolverConfigurationREST {

    @Inject
    private transient Logger log;

    @EJB
    private WebApi webapi;

    @POST
    @Path("/resolver/configuration/")
    public Response putResolverConfigurationRecord(ResolverConfiguration configuration) {
        try {
            ResolverConfiguration persistedConfiguration = webapi.putResolverConfiguration(configuration);
            return Response.status(Response.Status.OK).entity(persistedConfiguration).build();
        } catch (ResolverConfigurationValidationException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage() + " JSON was:" + configuration).build();
        } catch (Exception ex) {
            log.severe("Processing ResolverConfiguration went wrong: " + ex.getMessage());
            log.severe("ResolverConfiguration: " + configuration);
            ex.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }

    @GET
    @Path("/resolver/configuration/{id}")
    public ResolverConfiguration getResolverConfigurationRecord(@PathParam("id") int resolverId) {
        return webapi.getResolverConfiguration(resolverId);
    }

    @DELETE
    @Path("/resolver/configuration/{id}")
    public ResolverConfiguration deleteResolverConfigurationRecord(@PathParam("id") int resolverId) {
        return webapi.deleteResolverConfiguration(resolverId);
    }

    @GET
    @Path("/resolver/configuration/")
    public List<ResolverConfiguration> getAllResolverConfigurations() {
        return webapi.getAllResolverConfigurations();
    }

    @POST
    @Path("/resolver/enduser/")
    public Response putEndUserConfigurationRecord(EndUserConfiguration configuration) {
        try {
            EndUserConfiguration persistedConfiguration = webapi.putEndUserConfiguration(configuration);
            return Response.status(Response.Status.OK).entity(persistedConfiguration).build();
        } catch (EndUserConfigurationValidationException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage() + " JSON was:" + configuration).build();
        } catch (Exception ex) {
            log.severe("Processing EndUserConfiguration went wrong: " + ex.getMessage());
            log.severe("EndUserConfiguration: " + configuration);
            ex.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }

    @GET
    @Path("/resolver/enduser/{id}")
    public EndUserConfiguration getEndUserConfigurationRecord(@PathParam("id") String id) {
        return webapi.getEndUserConfiguration(id);
    }

    @DELETE
    @Path("/resolver/enduser/{id}")
    public EndUserConfiguration deleteEndUserConfigurationRecord(@PathParam("id") String id) {
        return webapi.deleteEndUserConfiguration(id);
    }

    @GET
    @Path("/resolver/enduser/")
    public List<EndUserConfiguration> getAllEndUserConfigurations() {
        return webapi.getAllEndUserConfigurations();
    }
}
