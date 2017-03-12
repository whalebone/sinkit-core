package biz.karms.sinkit.rest;

import org.apache.commons.lang3.ArrayUtils;

import javax.enterprise.context.SessionScoped;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.xml.bind.DatatypeConverter;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Random;

/**
 * @author Tomas Kozel
 */
@SessionScoped
@Path("/gsbapitest")
public class GSBTestApi implements Serializable {

    private static final long serialVersionUID = 5541951331672323961L;

    @POST
    @Path("/fullhash")
    @Produces({"application/octet-stream"})
    public byte[] testApiFullHashes(byte[] fullHashRequest) {
        byte[] prefix = ArrayUtils.subarray(fullHashRequest, fullHashRequest.length - 4, fullHashRequest.length);
        byte[] googleHash = DatatypeConverter.parseHexBinary("88981e6263be34a6c0b53ada73d168b68828dd643723d34a812e9f8a6abb5ee9"); // google.com/
        byte[] evilHash = DatatypeConverter.parseHexBinary("c759a0aaa49a133ff527065e3d18c51388eae5c72c927b5703d07ca2e80c0f35"); // evil.com/
        byte[] hash2 = new byte[32];
        new Random().nextBytes(hash2);

        byte[] metadata = new byte[3];
        metadata[0] = (byte) 0; //NULL
        metadata[1] = (byte) 3; //END OF TEXT
        metadata[2] = (byte) 10; //LINE FEED
        byte[] metadata2 = new byte[4];
        new Random().nextBytes(metadata2);

        byte[] rawResponse;
        if (Arrays.equals(prefix, ArrayUtils.subarray(googleHash, 0, prefix.length))) {
            rawResponse = ArrayUtils.addAll("1\ngoog-malware-shavar:32:2:m\n".getBytes(), googleHash);
        } else if (Arrays.equals(prefix, ArrayUtils.subarray(evilHash, 0, prefix.length))) {
            rawResponse = ArrayUtils.addAll("1\ngoog-phish-shavar:32:2:m\n".getBytes(), evilHash);
        } else {
            rawResponse = "1\n".getBytes();
            return rawResponse;
        }

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
