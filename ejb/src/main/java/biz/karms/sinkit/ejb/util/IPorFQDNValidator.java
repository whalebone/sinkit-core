package biz.karms.sinkit.ejb.util;

import com.google.common.net.HostSpecifier;
import com.google.common.net.InetAddresses;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.regex.Pattern;

/**
 * @author Michal Karm Babacek
 */
public class IPorFQDNValidator {
    public enum DECISION {
        IP,
        FQDN,
        GARBAGE,
    }

    public static final Pattern sanitize = Pattern.compile("[^a-zA-Z0-9-_\\.:].*");
    public static final Pattern dotUnderscoreRemove = Pattern.compile("^_|\\.$");
    public static final Pattern dotUnderscoreSeparatorPattern = Pattern.compile("\\._");

    // TODO: This needs broader verification and discussion
    public static ImmutablePair<DECISION, String> decide(final String data) {
        String modified = sanitize.matcher(data).replaceAll("");
        if (InetAddresses.isInetAddress(data)) {
            // We return sanitized version with which we could operate as with IP address
            return new ImmutablePair<>(DECISION.IP, modified);
        } else {
            modified = dotUnderscoreRemove.matcher(modified).replaceAll("");
            modified = dotUnderscoreSeparatorPattern.matcher(modified).replaceAll("\\.");
            if (HostSpecifier.isValid(modified)) {
                // We are satisfied that FQDN could be made of this data, but we return the original form.
                return new ImmutablePair<>(DECISION.FQDN, data);
            } else {
                return new ImmutablePair<>(DECISION.GARBAGE, data);
            }
        }
    }
}
