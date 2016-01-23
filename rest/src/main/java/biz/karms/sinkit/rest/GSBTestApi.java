package biz.karms.sinkit.rest;

import org.apache.commons.lang3.ArrayUtils;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.xml.bind.DatatypeConverter;
import java.util.Random;

/**
 * Created by tom on 12/22/15.
 *
 * @author Tomas Kozel
 */
@RequestScoped
@Path("/gsbapitest")
public class GSBTestApi {

    @POST
    @Path("/fullhash")
    @Produces({"application/octet-stream"})
    public byte[] testApiFullHashes(@HeaderParam(SinkitREST.AUTH_HEADER_PARAM) String token, byte[] fullHashRequest) {
        byte[] hash = DatatypeConverter.parseHexBinary("cf4b367e49bf0b22041c6f065f4aa19f3cfe39c8d5abc0617343d1a66c6a26f5"); // http://google.com/
        byte[] hash2 = new byte[32];
        new Random().nextBytes(hash2);

        byte[] metadata = new byte[3];
        metadata[0] = (byte) 0; //NULL
        metadata[1] = (byte) 3; //END OF TEXT
        metadata[2] = (byte) 10; //LINE FEED
        byte[] metadata2 = new byte[4];
        new Random().nextBytes(metadata2);

        byte[] rawResponse = ArrayUtils.addAll("1\ngoog-malware-shavar:32:2:m\n".getBytes(), hash);
        rawResponse = ArrayUtils.addAll(rawResponse, hash2);
        rawResponse = ArrayUtils.addAll(rawResponse, String.valueOf(metadata.length).getBytes());
        rawResponse = ArrayUtils.addAll(rawResponse, "\n".getBytes());
        rawResponse = ArrayUtils.addAll(rawResponse, metadata);
        rawResponse = ArrayUtils.addAll(rawResponse, String.valueOf(metadata2.length).getBytes());
        rawResponse = ArrayUtils.addAll(rawResponse, "\n".getBytes());
        rawResponse = ArrayUtils.addAll(rawResponse, metadata2);
        return rawResponse;
    }
}
