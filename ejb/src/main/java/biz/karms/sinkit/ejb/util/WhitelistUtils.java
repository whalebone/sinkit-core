package biz.karms.sinkit.ejb.util;

import biz.karms.sinkit.ejb.cache.pojo.WhitelistedRecord;
import biz.karms.sinkit.ioc.IoCRecord;
import biz.karms.sinkit.ioc.IoCSourceIdType;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by tkozel on 1/9/16.
 */
public class WhitelistUtils {

    public static final String HASH_ALG = "MD5";

    public static String computeHashedId(String whitelistedId) {
        return IoCIdentificationUtils.hashString(whitelistedId.getBytes(), HASH_ALG);
    }

    public static String stripSubdomains(String fqdn) {
        Pattern pattern = Pattern.compile("^.*\\.([^\\.]+\\.[^.]+)$");
        Matcher matcher = pattern.matcher(fqdn);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return fqdn;
    }

    public static WhitelistedRecord createWhitelistedRecord(IoCRecord iocRecord) {
        WhitelistedRecord white = new WhitelistedRecord();
        if (iocRecord.getSource().getId().getType() == IoCSourceIdType.FQDN) {
            white.setRawId(WhitelistUtils.stripSubdomains(iocRecord.getSource().getId().getValue()));
        } else {
            white.setRawId(iocRecord.getSource().getId().getValue());
        }
        Calendar expiresAt = Calendar.getInstance();
        expiresAt.add(Calendar.SECOND, iocRecord.getSource().getTTL().intValue());
        white.setExpiresAt(expiresAt);
        white.setSourceName(iocRecord.getFeed().getName());
        return white;
    }
}
