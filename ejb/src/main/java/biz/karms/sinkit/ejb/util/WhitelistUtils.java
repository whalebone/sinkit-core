package biz.karms.sinkit.ejb.util;

import biz.karms.sinkit.ejb.cache.pojo.WhitelistedRecord;
import biz.karms.sinkit.ioc.IoCRecord;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Tomas Kozel
 */
public class WhitelistUtils {

    public static final Pattern pattern = Pattern.compile("^.*\\.([^\\.]+\\.[^.]+)$");

    public static String stripSubdomains(final String fqdn) {
        Matcher matcher = pattern.matcher(fqdn);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return fqdn;
    }

    public static WhitelistedRecord createWhitelistedRecord(final IoCRecord iocRecord, final boolean completed) {
        final WhitelistedRecord white = new WhitelistedRecord();
        white.setRawId(iocRecord.getSource().getId().getValue());
        final Calendar expiresAt = Calendar.getInstance();
        expiresAt.add(Calendar.SECOND, iocRecord.getSource().getTtl().intValue());
        white.setExpiresAt(expiresAt);
        white.setSourceName(iocRecord.getFeed().getName());
        white.setCompleted(completed);
        return white;
    }

    public static String[] explodeDomains(final String fqdn) {
        final String[] explodedFqdn = fqdn.split("\\.");
        if (explodedFqdn.length == 1) {
            return explodedFqdn;
        }
        final ArrayList<String> fqdns = new ArrayList<>();
        String subFqdn = explodedFqdn[explodedFqdn.length - 2] + "." + explodedFqdn[explodedFqdn.length - 1];
        fqdns.add(subFqdn);
        for (int i = explodedFqdn.length - 3; i >= 0; i--) {
            subFqdn = explodedFqdn[i] + "." + subFqdn;
            fqdns.add(subFqdn);
        }
        return fqdns.toArray(new String[fqdns.size()]);
    }
}
