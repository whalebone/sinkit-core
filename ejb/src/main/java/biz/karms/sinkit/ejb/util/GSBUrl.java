package biz.karms.sinkit.ejb.util;

import biz.karms.sinkit.ejb.gsb.util.GSBUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.net.URISyntaxException;

/**
 * Created by tom on 11/29/15.
 *
 * @author Tomas Kozel
 * @author Michal Karm Babacek
 */
public class GSBUrl {

    private GSBUrl() {
    }

    public static String getUrl(final String ipOrFQDN) {
        if (ipOrFQDN == null) {
            throw new IllegalArgumentException("ipOrFQDN is null.");
        }
        try {
            return GSBUtils.canonicalizeUrl(ipOrFQDN);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("ipOrFQDN " + ipOrFQDN + " has wrong format: " + ex.getMessage(), ex);
        }
    }

    public static String getHashStringPrefix(final int bytes, final String hashString) {
        if (hashString == null) {
            throw new IllegalArgumentException("hashString is null.");
        }
        return (bytes * 2 > hashString.length()) ? hashString : hashString.substring(0, bytes * 2);
    }

    public static byte[] getHashPrefix(final int length, final byte[] hash) {
        if (hash == null) {
            throw new IllegalArgumentException("hash is null.");
        }
        if (hash.length < length) {
            return hash;
        }
        return ArrayUtils.subarray(hash, 0, length);
    }

    public static String getHashString(final byte[] hash) {
        if (hash == null) {
            throw new IllegalArgumentException("hash is null.");
        }
        return GSBUtils.hashToString(hash);
    }

    public static byte[] getHash(final String canonicalizedURL) {
        if (canonicalizedURL == null) {
            throw new IllegalArgumentException("canonicalizedURL is null.");
        }
        return GSBUtils.computeHash(canonicalizedURL);
    }

}
