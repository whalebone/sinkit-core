package biz.karms.sinkit.ejb.gsb.util;

import org.apache.commons.validator.routines.InetAddressValidator;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tom on 12/6/15.
 *
 * @author Tomas Kozel
 */
public class GSBUtils {

    // max number of parts(subdomains) extracted from host name
    // that are used for construct of lookup variants
    // this is according to google spec
    private static final int MAX_HOST_PARTS = 5;

    public static List<String> getLookupVariants(final String ipOrFQDN) {
        final String lookupUrl = ipOrFQDN + '/';  // this is all the canonicalization
        final List<String> lookupVariants = new ArrayList<>();
        // original url is used for lookup every time
        lookupVariants.add(lookupUrl);
        // if ipOrFQDN is IPv4 or IPv6 then there is only one variant
        if (InetAddressValidator.getInstance().isValid(ipOrFQDN)) {
            return lookupVariants;
        }

        String hostParts[] = lookupUrl.split("\\.");
        int maxHostParts;
        if (hostParts.length <= 2) {
            // if hostname consists of 2 parts (i.e. whalebone.io) or less then no other variant
            // than whole hostname (which was added to list before) is possible
            return lookupVariants;
        } else if (hostParts.length > 2 && hostParts.length <= MAX_HOST_PARTS) {
            // if hostname contains num of parts between 3 and MAX_HOST_PARTS (inclusive) then
            // only contained parts - 1 is extracted, since the whole hostname is already
            // added as variant
            maxHostParts = hostParts.length - 1;
        } else {
            // else if hostname contains too much parts than only MAX_HOST_PARTS is extracted
            maxHostParts = MAX_HOST_PARTS;
        }

        // construct lookup variants from the end of the hostname and start at 2nd part so for the
        // hostname a.b.c.d.e we will have d.e, c.d.e, b.c.d.e (a.b.c.d.e has already been added before)
        String variant = hostParts[hostParts.length - 1];
        for (int i = hostParts.length - 2; i >= hostParts.length - maxHostParts; i--) {
            variant = hostParts[i] + "." + variant;
            lookupVariants.add(variant);
        }
        return lookupVariants;
    }

    /*
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String ENCODING = "UTF-8";

    public static final String PROTO_GROUP = "proto";
    public static final String HOST_GROUP = "host";
    public static final String PORT_GROUP = "port";
    public static final String PATH_GROUP = "path";
    public static final String QUERY_GROUP = "query";
    public static final String URL_REGEXP = "^((?<" + PROTO_GROUP + ">[a-zA-Z]*):\\/\\/)?" +
                                            "(?<" + HOST_GROUP + ">[^/]+(?<!:[0-9]{0,5}))" +
                                            "(:(?<" + PORT_GROUP + ">[0-9]+)?)?" +
                                            "((?<" + PATH_GROUP + ">\\/[^?]*)" +
                                            "(?<" + QUERY_GROUP + ">\\?.*)?)?$";
    public static final Map<String, String> DEFUALT_PORTS = new HashMap<>();
    static {
        DEFUALT_PORTS.put("http", "80");
        DEFUALT_PORTS.put("https", "443");
        DEFUALT_PORTS.put("ftp", "20");
    }

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

    public static String canonicalizeUrl(String url) throws URISyntaxException {
        String proto;
        String host;
        String port;
        String path;
        String query;
        StringBuilder sb = new StringBuilder();
        try {
            String toBeCanonicalized = url.trim()
                    .replaceAll("[\t\r\n]", "")      // remove tabulator and new line
                    .replaceFirst("#.*$", "");    // remove segment
            toBeCanonicalized = unescape(toBeCanonicalized);
            Pattern urlPattern = Pattern.compile(URL_REGEXP);
            Matcher matcher = urlPattern.matcher(toBeCanonicalized);
            if (!matcher.find()) {
                throw new URISyntaxException(toBeCanonicalized, "Provided URL is not valid URL");
            }
            // proto to lower case
            if (StringUtils.isNotBlank(matcher.group(PROTO_GROUP))) {
                proto = matcher.group(PROTO_GROUP).toLowerCase();
            } else {
                proto = "http";
            }
            sb.append(proto).append("://");
            // canonicalize host
            if (StringUtils.isBlank(matcher.group(HOST_GROUP))) {
                throw new NullPointerException("URL host is not specified");
            } else {
                host = matcher.group(HOST_GROUP)
                        .replaceAll("\\.+", ".")     // replace consecutive dots with single one
                        .replaceFirst("^\\.", "")    // remove leading dot
                        .replaceFirst("\\.$", "")    // remove trailing dot
                        .toLowerCase();
                if (host.matches("\\d+")) {
                    BigInteger ip = new BigInteger(host);
                    if (ip.compareTo(BigInteger.valueOf(4294967296l)) < 0 && ip.compareTo(BigInteger.valueOf(255l)) > 0) {
                        host = longToIPv4(ip.longValue());
                    }
                }
            }
            sb.append(host);
            // canonicalize port
            if (StringUtils.isNotBlank(matcher.group(PORT_GROUP))) {
                port = matcher.group(PORT_GROUP);
                if (!port.equals(DEFUALT_PORTS.get(proto))) {
                    sb.append(":").append(port);
                }
            }
            //canonicalize path
            if (StringUtils.isNotBlank(matcher.group(PATH_GROUP))) {
                path = matcher.group(PATH_GROUP)
                        .replaceAll("[^/]+\\/\\.\\.\\/?", "/")
                        .replaceAll("[^/]+\\.\\/", "/")
                        .replaceAll("\\/{2,}", "/");
            } else {
                path = "/";
            }
            sb.append(path);
            //canonicalize query
            if (StringUtils.isNotBlank(matcher.group(QUERY_GROUP))) {
                query = matcher.group(QUERY_GROUP);
            } else {
                query = "";
            }
            sb.append(query);
            return escape(sb.toString());

        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("What!? " + ENCODING + " is unsupported encoding!? Cannot unescape URL.");
        }
    }

    private static String unescape(String toBeUnescaped) throws UnsupportedEncodingException {
        Pattern escapePattern = Pattern.compile("(%[a-fA-F0-9]{2})");
        Matcher matcher;
        String escapeSequence;
        String decodedSequence;
        StringBuilder sb = new StringBuilder(toBeUnescaped);
        boolean keepWorking;
        int lastMatchEnd;
        char[] buff;
        do {
            keepWorking = false;
            matcher = escapePattern.matcher(sb.toString());
            buff = sb.toString().toCharArray();
            sb = new StringBuilder();
            lastMatchEnd = 0;
            while (matcher.find()) {
                escapeSequence = matcher.group(0);
                decodedSequence = URLDecoder.decode(escapeSequence, ENCODING);
                sb.append(buff, lastMatchEnd, matcher.start() - lastMatchEnd);
                sb.append(decodedSequence);
                lastMatchEnd = matcher.end();
                keepWorking = true;
            }
            if (lastMatchEnd < buff.length) {
                sb.append(buff, lastMatchEnd, buff.length - lastMatchEnd);
            }
        } while (keepWorking);
        return new String(buff);
    }

    private static String escape(String toBeEscaped) throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        String escapedSequence;
        for (int i = 0; i < toBeEscaped.length(); i++) {
            char c = toBeEscaped.charAt(i);
            if (c == '%' || c == '#' || (int) c <= 32 || (int) c >= 127) {
                if (c == 32) {
                    // URLEncoder encodes space as '+' but we do want '%20'
                    escapedSequence = "%20";
                } else {
                    escapedSequence = URLEncoder.encode(String.valueOf(c), ENCODING);
                }
                sb.append(escapedSequence);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String longToIPv4(long ip) {
        return String.format("%d.%d.%d.%d",
                (ip >> 24 & 0xff),
                (ip >> 16 & 0xff),
                (ip >> 8 & 0xff),
                (ip & 0xff)).replaceAll("^(0\\.)+", "");
    }*/
}
