package biz.karms.sinkit.ejb.util;

import biz.karms.sinkit.ioc.IoCRecord;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.Md5Crypt;

/**
 * Created by tom on 11/7/15.
 */
public class IoCIdentificationUtils {

    public static String computeHashedId(final IoCRecord ioc) {

        final StringBuilder idString = new StringBuilder();
        if (ioc.getFeed() != null && ioc.getFeed().getName() != null) {
            idString.append(ioc.getFeed().getName());
        }

        if (ioc.getClassification() != null && ioc.getClassification().getType() != null) {
            idString.append(ioc.getClassification().getType());
        }

        if (ioc.getSource() != null && ioc.getSource().getId() != null && ioc.getSource().getId().getValue() != null) {
            idString.append(ioc.getSource().getId().getValue());
        }

        if (ioc.getTime() != null && ioc.getTime().getDeactivated() != null) {
            idString.append(ioc.getTime().getDeactivated().getTime());
        }

        if (idString.length() == 0) {
            return null;
        }

        return DigestUtils.md5Hex(Md5Crypt.md5Crypt(idString.toString().getBytes()));
    }

    public static String computeUniqueReference(final IoCRecord ioc) {

        final StringBuilder uniqueRef = new StringBuilder();
        if (ioc.getFeed() != null && ioc.getFeed().getName() != null) {
            uniqueRef.append(ioc.getFeed().getName());
        }

        if (ioc.getClassification() != null && ioc.getClassification().getType() != null) {
            uniqueRef.append(ioc.getClassification().getType());
        }

        if (ioc.getSource() != null && ioc.getSource().getId() != null && ioc.getSource().getId().getValue() != null) {
            uniqueRef.append(ioc.getSource().getId().getValue());
        }

        if (ioc.getTime() != null && ioc.getTime().getReceivedByCore() != null) {
            uniqueRef.append(ioc.getTime().getReceivedByCore().getTime());
        }

        if (uniqueRef.length() == 0) {
            return null;
        }

        return DigestUtils.md5Hex(Md5Crypt.md5Crypt(uniqueRef.toString().getBytes()));
    }
}
