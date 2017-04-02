package biz.karms.sinkit.rest;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

import static biz.karms.sinkit.ejb.protostream.CustomlistProtostreamGenerator.SINKIT_CUSTOMLIST_PROTOSTREAM_GENERATOR_D_H_M_S;
import static biz.karms.sinkit.ejb.protostream.CustomlistProtostreamGenerator.customListFileMd5;
import static biz.karms.sinkit.ejb.protostream.WhitelistProtostreamGenerator.SINKIT_WHITELIST_PROTOSTREAM_GENERATOR_D_H_M_S;
import static biz.karms.sinkit.ejb.protostream.WhitelistProtostreamGenerator.whiteListFileMd5;
import static biz.karms.sinkit.ejb.protostream.WhitelistProtostreamGenerator.whiteListFilePath;
import static biz.karms.sinkit.rest.DnsREST.CLIENT_ID_HEADER_PARAM;

/**
 * @author Michal Karm Babacek
 */
@RequestScoped
@Path("/")
public class ProtostreamREST implements Serializable {

    private static final long serialVersionUID = -811275040019884876L;

    @Inject
    SinkitService sinkitService;

    @Inject
    private Logger log;

    public static final String iocFilePath = System.getProperty("java.io.tmpdir") + "/ioc.bin";
    public static final String customListFilePath = System.getProperty("java.io.tmpdir") + "/customlist.bin";

    public static final int TRY_LATER = 466;
    public static final String X_ERROR = "X-error";
    public static final String X_FILE_LENGTH = "X-file-length";
    public static final String X_FILE_MD5 = "X-file-md5";

    /**
     * @returns huge byte array Protocol Buffer with Whitelist records
     */
    @GET
    @Path("/protostream/whitelist")
    @Produces({"application/x-protobuf"})
    public Response getProtostreamWhitelist() {
        if (SINKIT_WHITELIST_PROTOSTREAM_GENERATOR_D_H_M_S == null) {
            return Response.status(Response.Status.NOT_FOUND).header(X_ERROR, "This is a wrong node. Protostream generator is not started.").build();
        }
        final File whiteListBinary = new File(whiteListFilePath);
        final File whitelistBinaryMD5 = new File(whiteListFileMd5);
        if (whiteListBinary.exists() && whitelistBinaryMD5.exists()) {
            InputStream is;
            String md5sum;
            try {
                is = new FileInputStream(whiteListBinary);
                md5sum = new String(Files.readAllBytes(whitelistBinaryMD5.toPath()), StandardCharsets.UTF_8);
            } catch (FileNotFoundException e) {
                log.log(Level.SEVERE, whiteListFilePath + " not found.");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).header(X_ERROR, whiteListFilePath + " not found.").build();
            } catch (IOException e) {
                log.log(Level.SEVERE, whiteListFileMd5 + " not found.");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).header(X_ERROR, whiteListFileMd5 + " not found.").build();
            }
            return Response.ok().entity(is)
                    .header(X_FILE_LENGTH, String.valueOf(whiteListBinary.length()))
                    .header(X_FILE_MD5, md5sum)
                    .build();
        } else {
            return Response.status(TRY_LATER).header(X_ERROR, "Try later, please.").build();
        }
    }

    /**
     * @returns huge byte array Protocol Buffer with custom list records
     */
    @GET
    @Path("/protostream/customlist")
    @Produces({"application/x-protobuf"})
    public Response getProtostreamCustomList(@HeaderParam(CLIENT_ID_HEADER_PARAM) Integer clientId) {
        if (SINKIT_CUSTOMLIST_PROTOSTREAM_GENERATOR_D_H_M_S == null) {
            return Response.status(Response.Status.NOT_FOUND).header(X_ERROR, "This is a wrong node. Protostream generator is not started.").build();
        }
        if (clientId == null || clientId < 0) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).header(X_ERROR, CLIENT_ID_HEADER_PARAM + " seems to be invalid or missing").build();
        }
        final File customListBinary = new File(customListFilePath + clientId);
        final File customlistBinaryMD5 = new File(customListFileMd5 + clientId);
        if (customListBinary.exists() && customlistBinaryMD5.exists()) {
            InputStream is;
            String md5sum;
            try {
                is = new FileInputStream(customListBinary);
                md5sum = new String(Files.readAllBytes(customlistBinaryMD5.toPath()), StandardCharsets.UTF_8);
            } catch (FileNotFoundException e) {
                log.log(Level.SEVERE, customListFilePath + clientId + " not found.");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).header(X_ERROR, customListFilePath + clientId + " not found.").build();
            } catch (IOException e) {
                log.log(Level.SEVERE, customListFileMd5 + clientId + " not found.");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).header(X_ERROR, customListFileMd5 + clientId + " not found.").build();
            }
            return Response.ok().entity(is)
                    .header(X_FILE_LENGTH, String.valueOf(customListBinary.length()))
                    .header(X_FILE_MD5, md5sum)
                    .build();
        } else {
            return Response.status(TRY_LATER).header(X_ERROR, "Try later, please.").build();
        }
    }

}
