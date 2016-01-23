package biz.karms.sinkit.ejb.util;

import biz.karms.sinkit.ejb.cache.pojo.WhitelistedRecord;
import biz.karms.sinkit.ioc.IoCRecord;
import biz.karms.sinkit.ioc.IoCSource;
import biz.karms.sinkit.ioc.IoCSourceIdType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by tkozel on 1/9/16.
 */
public class WhitelistUtils {

    public static final String HASH_ALG = "MD5";

    public static String computeHashedId(final String whitelistedId) {
        return IoCIdentificationUtils.hashString(whitelistedId.getBytes(), HASH_ALG);
    }

    public static String stripSubdomains(final String fqdn) {
        Pattern pattern = Pattern.compile("^.*\\.([^\\.]+\\.[^.]+)$");
        Matcher matcher = pattern.matcher(fqdn);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return fqdn;
    }

    public static WhitelistedRecord createWhitelistedRecord(final IoCRecord iocRecord, boolean completed) {
        WhitelistedRecord white = new WhitelistedRecord();
        white.setRawId(iocRecord.getSource().getId().getValue());
        Calendar expiresAt = Calendar.getInstance();
        expiresAt.add(Calendar.SECOND, iocRecord.getSource().getTTL().intValue());
        white.setExpiresAt(expiresAt);
        white.setSourceName(iocRecord.getFeed().getName());
        white.setCompleted(completed);
        return white;
    }

    public static String[] explodeDomains(String fqdn) {
        String[] explodedFqdn = fqdn.split("\\.");
        if (explodedFqdn.length == 1) {
            return explodedFqdn;
        }
        ArrayList<String> fqdns = new ArrayList<>();
        String subFqdn = explodedFqdn[explodedFqdn.length - 2] + "." + explodedFqdn[explodedFqdn.length - 1];
        fqdns.add(subFqdn);
        for (int i = explodedFqdn.length - 3; i >= 0; i--) {
            subFqdn = explodedFqdn[i] + "." + subFqdn;
            fqdns.add(subFqdn);
        }
        return fqdns.toArray(new String[fqdns.size()]);
    }
}
