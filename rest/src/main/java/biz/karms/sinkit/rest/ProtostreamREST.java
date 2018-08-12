package biz.karms.sinkit.rest;

import org.apache.commons.lang3.StringUtils;

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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import static biz.karms.sinkit.rest.DnsREST.CLIENT_ID_HEADER_PARAM;
import static java.lang.String.format;

/**
 * @author Michal Karm Babacek
 */
@RequestScoped
@Path("/")
public class ProtostreamREST implements Serializable {

    private static final long serialVersionUID = -811275040019884876L;

    private static final String GENERATED_PROTOFILES_DIRECTORY =
            (System.getenv().containsKey("SINKIT_GENERATED_PROTOFILES_DIRECTORY") && StringUtils.isNotEmpty(System.getenv("SINKIT_GENERATED_PROTOFILES_DIRECTORY")))
                    ? System.getenv("SINKIT_GENERATED_PROTOFILES_DIRECTORY") : System.getProperty("java.io.tmpdir");

    private static final String iocWithCustomFilePath = GENERATED_PROTOFILES_DIRECTORY + "/iocWithCustom.bin";
    private static final String iocWithCustomFileMd5 = GENERATED_PROTOFILES_DIRECTORY + "/iocWithCustom.bin.md5";

    private static final String whiteListFilePath = GENERATED_PROTOFILES_DIRECTORY + "/whitelist.bin";
    private static final String whiteListFileMd5 = GENERATED_PROTOFILES_DIRECTORY + "/whitelist.bin.md5";

    private static final String customListFilePath = GENERATED_PROTOFILES_DIRECTORY + "/customlist.bin";
    private static final String customListFileMd5 = GENERATED_PROTOFILES_DIRECTORY + "/customlist.bin.md5";

    private static final String iocListFilePath = GENERATED_PROTOFILES_DIRECTORY + "/ioclist.bin";
    private static final String iocListFileMd5 = GENERATED_PROTOFILES_DIRECTORY + "/ioclist.bin.md5";

    @Inject
    SinkitService sinkitService;

    @Inject
    private transient Logger log;

    public static final int TRY_LATER = 466;
    public static final String X_ERROR = "X-error";
    public static final String X_FILE_LENGTH = "X-file-length";
    public static final String X_FILE_MD5 = "X-file-md5";

    /**
     * @returns huge byte array Protocol Buffer with Whitelist records without those
     * fqdn that at least one user has set for Logging or Blocking
     */
    @GET
    @Path("/protostream/iocwithcustom")
    @Produces({"application/x-protobuf"})
    public Response getProtostreamIoCWithCustom() {
        /*if (SINKIT_ALL_IOC_PROTOSTREAM_GENERATOR_D_H_M_S == null) {
            return Response.status(Response.Status.NOT_FOUND).header(X_ERROR, "This is a wrong node. Protostream generator is not started.").build();
        }*/
        final File iocWithCustomBinary = new File(iocWithCustomFilePath);
        final File iocWithCustomBinaryMD5 = new File(iocWithCustomFileMd5);
        if (iocWithCustomBinary.exists() && iocWithCustomBinaryMD5.exists()) {
            InputStream is;
            String md5sum;
            try {
                is = new FileInputStream(iocWithCustomBinary);
                md5sum = new String(Files.readAllBytes(iocWithCustomBinaryMD5.toPath()), StandardCharsets.UTF_8);
            } catch (FileNotFoundException e) {
                log.log(Level.SEVERE, iocWithCustomFilePath + " not found.");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).header(X_ERROR, iocWithCustomFilePath + " not found.").build();
            } catch (IOException e) {
                log.log(Level.SEVERE, iocWithCustomFileMd5 + " not found.");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).header(X_ERROR, iocWithCustomFileMd5 + " not found.").build();
            }
            return Response.ok().entity(is)
                    .header(X_FILE_LENGTH, String.valueOf(iocWithCustomBinary.length()))
                    .header(X_FILE_MD5, md5sum)
                    .build();
        } else {
            return Response.status(TRY_LATER).header(X_ERROR, "Try later, please.").build();
        }
    }

    /**
     * @returns huge byte array Protocol Buffer with Whitelist records
     */
    @GET
    @Path("/protostream/whitelist")
    @Produces({"application/x-protobuf"})
    public Response getProtostreamWhitelist() {
        /*if (SINKIT_WHITELIST_PROTOSTREAM_GENERATOR_D_H_M_S == null) {
            return Response.status(Response.Status.NOT_FOUND).header(X_ERROR, "This is a wrong node. Protostream generator is not started.").build();
        }*/
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
        /*if (SINKIT_CUSTOMLIST_PROTOSTREAM_GENERATOR_D_H_M_S == null) {
            return Response.status(Response.Status.NOT_FOUND).header(X_ERROR, "This is a wrong node. Protostream generator is not started.").build();
        }*/
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
            // If any other file is generated, it means the generator cycle already passed, but there are no data for this particular client ID
            try (DirectoryStream<java.nio.file.Path> someFiles = Files.newDirectoryStream(Paths.get(GENERATED_PROTOFILES_DIRECTORY), "ioclist.bin*")) {
                if (someFiles.iterator().hasNext()) {
                    // We return anyway, no matter if the call succeeded.
                    customListBinary.createNewFile();
                    // The desired behaviour is to return an empty file
                    return Response.ok().entity(new FileInputStream(customListBinary))
                            .header(X_FILE_LENGTH, "0")
                            .header(X_FILE_MD5, "")
                            .build();
                }
            } catch (IOException e) {
                return Response.status(TRY_LATER).header(X_ERROR, "Try later, please.").build();
            }
            return Response.status(TRY_LATER).header(X_ERROR, "Try later, please.").build();
        }
    }

    /**
     * @returns huge byte array Protocol Buffer with custom list records
     */
    @GET
    @Path("/protostream/ioclist")
    @Produces({"application/x-protobuf"})
    public Response getProtostreamIOCList(@HeaderParam(CLIENT_ID_HEADER_PARAM) Integer clientId) {
        /*if (SINKIT_IOC_PROTOSTREAM_GENERATOR_D_H_M_S == null) {
            return Response.status(Response.Status.NOT_FOUND).header(X_ERROR, "This is a wrong node. Protostream generator is not started.").build();
        }*/
        if (clientId == null || clientId < 0) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).header(X_ERROR, CLIENT_ID_HEADER_PARAM + " seems to be invalid or missing").build();
        }
        final File iocListBinary = new File(iocListFilePath + clientId);
        final File customlistBinaryMD5 = new File(iocListFileMd5 + clientId);
        if (iocListBinary.exists() && customlistBinaryMD5.exists()) {
            InputStream is;
            String md5sum;
            try {
                is = new FileInputStream(iocListBinary);
                md5sum = new String(Files.readAllBytes(customlistBinaryMD5.toPath()), StandardCharsets.UTF_8);
            } catch (FileNotFoundException e) {
                log.log(Level.SEVERE, iocListFilePath + clientId + " not found.");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).header(X_ERROR, iocListFilePath + clientId + " not found.").build();
            } catch (IOException e) {
                log.log(Level.SEVERE, iocListFileMd5 + clientId + " not found.");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).header(X_ERROR, iocListFileMd5 + clientId + " not found.").build();
            }
            return Response.ok().entity(is)
                    .header(X_FILE_LENGTH, String.valueOf(iocListBinary.length()))
                    .header(X_FILE_MD5, md5sum)
                    .build();
        } else {
            // If any other file is generated, it means the generator cycle already passed, but there are no data for this particular client ID
            try (DirectoryStream<java.nio.file.Path> someFiles = Files.newDirectoryStream(Paths.get(GENERATED_PROTOFILES_DIRECTORY), "ioclist.bin*")) {
                if (someFiles.iterator().hasNext()) {
                    // We return anyway, no matter if the call succeeded.
                    iocListBinary.createNewFile();
                    // The desired behaviour is to return an empty file
                    return Response.ok().entity(new FileInputStream(iocListBinary))
                            .header(X_FILE_LENGTH, "0")
                            .header(X_FILE_MD5, "")
                            .build();
                }
            } catch (IOException e) {
                return Response.status(TRY_LATER).header(X_ERROR, "Try later, please.").build();
            }
            return Response.status(TRY_LATER).header(X_ERROR, "Try later, please.").build();
        }
    }


    /**
     * @returns huge byte array Protocol Buffer with custom list records
     */
    @GET
    @Path("/protostream/resolvercache")
    @Produces({"application/x-protobuf"})
    public Response getResolverCacheProtostreamFile(@HeaderParam("x-resolver-id") Integer resolverId) {
        if (resolverId == null || resolverId < 0) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).header(X_ERROR, "x-resolver-id" + " seems to be invalid or missing").build();
        }

        final java.nio.file.Path fileName = Paths.get(format("%s/%s_resolver_cache.bin", GENERATED_PROTOFILES_DIRECTORY, resolverId));
        final java.nio.file.Path md5FileName = Paths.get(format("%s/%s_resolver_cache.bin.md5", GENERATED_PROTOFILES_DIRECTORY, resolverId));

        if (Files.exists(fileName) && Files.exists(md5FileName)) {
            try {
                final String md5sum = new String(Files.readAllBytes(md5FileName), StandardCharsets.UTF_8);
                final InputStream inputStream = Files.newInputStream(fileName);
                return Response.ok().entity(inputStream)
                        .header(X_FILE_LENGTH, String.valueOf(inputStream.available()))
                        .header(X_FILE_MD5, md5sum)
                        .build();
            } catch (FileNotFoundException e) {
                log.log(Level.SEVERE, fileName.toString() + resolverId + " not found.");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).header(X_ERROR, fileName.toFile().getName() + " not found.").build();
            } catch (IOException e) {
                log.log(Level.SEVERE, iocListFileMd5 + resolverId + " not found.");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).header(X_ERROR, md5FileName.toFile().getName() + " not found.").build();
            }
        } else {
            // If any other file is generated, it means the generator cycle already passed, but there are no data for this particular resolver ID
            try (DirectoryStream<java.nio.file.Path> someFiles = Files.newDirectoryStream(Paths.get(GENERATED_PROTOFILES_DIRECTORY), "*_resolver_cache.bin")) {
                if (someFiles.iterator().hasNext()) {
                    // We return anyway, no matter if the call succeeded.
                    // The desired behaviour is to return an empty file
                    return Response.ok().entity(Files.newInputStream(fileName))
                            .header(X_FILE_LENGTH, "0")
                            .header(X_FILE_MD5, "")
                            .build();
                }
            } catch (IOException e) {
                return Response.status(TRY_LATER).header(X_ERROR, "Try later, please.").build();
            }
            return Response.status(TRY_LATER).header(X_ERROR, "Try later, please.").build();
        }
    }
}
