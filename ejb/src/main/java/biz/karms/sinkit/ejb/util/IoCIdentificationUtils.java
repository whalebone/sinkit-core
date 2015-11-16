package biz.karms.sinkit.ejb.util;

import biz.karms.sinkit.ioc.IoCRecord;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

/**
 * Created by tom on 11/7/15.
 */
public class IoCIdentificationUtils {

    private static final String HASH_ALG = "MD5";
    private static final Random RANDOM = new SecureRandom();

    public static String computeHashedId(IoCRecord ioc) {

        String idString = "";
        if (ioc.getFeed() != null && ioc.getFeed().getName() != null) {
            idString += ioc.getFeed().getName();
        }

        if (ioc.getClassification() != null && ioc.getClassification().getType() != null) {
            idString += ioc.getClassification().getType();
        }

        if (ioc.getSource() != null && ioc.getSource().getId() != null && ioc.getSource().getId().getValue() != null) {
            idString += ioc.getSource().getId().getValue();
        }

        if (ioc.getTime() != null && ioc.getTime().getDeactivated() != null) {
            idString += ioc.getTime().getDeactivated().getTime();
        }

        if (idString.equals("")) {
            return null;
        }

        return IoCIdentificationUtils.hashString(idString.getBytes());
    }

    public static String computeUniqueReference(IoCRecord ioc) {

        String uniqueRef = "";
        if (ioc.getFeed() != null && ioc.getFeed().getName() != null) {
            uniqueRef += ioc.getFeed().getName();
        }

        if (ioc.getClassification() != null && ioc.getClassification().getType() != null) {
            uniqueRef += ioc.getClassification().getType();
        }

        if (ioc.getSource() != null && ioc.getSource().getId() != null && ioc.getSource().getId().getValue() != null) {
            uniqueRef += ioc.getSource().getId().getValue();
        }

        if (ioc.getTime() != null && ioc.getTime().getReceivedByCore() != null) {
            uniqueRef += ioc.getTime().getReceivedByCore().getTime();
        }

        if (uniqueRef.equals("")) return null;

        byte[] salt = getNextSalt();
        byte[] uniqueRefBytes = uniqueRef.getBytes();
        byte[] toBeHashed = new byte[uniqueRefBytes.length + salt.length];
        System.arraycopy(uniqueRefBytes, 0, toBeHashed, 0, uniqueRefBytes.length);
        System.arraycopy(salt, 0, toBeHashed, uniqueRefBytes.length, salt.length);
        return hashString(toBeHashed);
    }

    private static String hashString(byte[] toBeHashed) {
        String hashString = null;
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(HASH_ALG);
            md.update(toBeHashed);
            hashString = new BigInteger(1, md.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            //should never be throw since HASH_ALG is hardcoded
            e.printStackTrace();
        }
        return hashString;
    }

    /**
     * Returns a random salt to be used to hash a password.
     *
     * @return a 16 bytes random salt
     */
    public static byte[] getNextSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return salt;
    }
}
