package biz.karms.sinkit.ejb.gsb.util;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;

/**
 * Created by tom on 12/6/15.
 *
 * @author Tomas Kozel
 */
public class GSBUtils {

    public static final String HASH_ALGORITHM = "SHA-256";
    public static final String ENCODING = "UTF-8";

    public static byte[] computeHash(String message) {
        try {
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);

            md.update(message.getBytes(ENCODING));
            return md.digest();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("What!? " + HASH_ALGORITHM + " is unknown algorithm!? Cannot compute hash.");
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException("What!? " + ENCODING + " is unsupported encoding!? Cannot compute hash.");
        }
    }

    public static String hashToString(byte[] hash) {
        return (new HexBinaryAdapter()).marshal(hash).toLowerCase();
    }
}
