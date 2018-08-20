package biz.karms.sinkit.ejb.util;

import biz.karms.sinkit.exception.IoCValidationException;
import biz.karms.sinkit.exception.TooOldIoCException;
import biz.karms.sinkit.ioc.IoCRecord;

import java.util.Calendar;
import java.util.Date;

/**
 * @author Tomas Kozel
 */
public class IoCValidator {

    public static final int MAX_BYTES_FOR_LUCENE = 32766;

    public static IoCRecord validateIoCRecord(final IoCRecord ioc, final int windowHours) throws IoCValidationException {
        if (ioc == null) {
            throw new IoCValidationException("IoC record is null");
        }
        if (ioc.getFeed() == null || ioc.getFeed().getName() == null) {
            throw new IoCValidationException("IoC record doesn't have mandatory field 'feed.name'");
        }
        if (ioc.getSource() == null || (
                ioc.getSource().getFqdn() == null && ioc.getSource().getIp() == null)) {
            throw new IoCValidationException("IoC can't have both IP and Domain set as null");
        }
        if (ioc.getClassification() == null || ioc.getClassification().getType() == null) {
            throw new IoCValidationException("IoC record doesn't have mandatory field 'classification.type'");
        }
        if (ioc.getTime() == null || ioc.getTime().getObservation() == null) {
            throw new IoCValidationException("IoC record doesn't have mandatory field 'time.observation'");
        }

        Date checkedTime;
        if (ioc.getTime().getSource() != null) {
            checkedTime = ioc.getTime().getSource();
        } else {
            checkedTime = ioc.getTime().getObservation();
        }
        if (DateUtils.addWindow(checkedTime, windowHours).before(Calendar.getInstance().getTime())) {
            String field = ioc.getTime().getSource() != null ? "source" : "observation";
            throw new TooOldIoCException("IoC record is too old. Field 'time." + field + "': " + checkedTime +
                    " shuld not be older than " + windowHours + " hours.");
        }

        // raw attribute too long for Elasticsearch upsert?
        if (ioc.getRaw() != null && ioc.getRaw().length() > MAX_BYTES_FOR_LUCENE) {
            ioc.setRaw(ioc.getRaw().substring(0, MAX_BYTES_FOR_LUCENE - 1));
        }

        return ioc;
    }

    public static IoCRecord validateWhitelistIoCRecord(final IoCRecord ioc) throws IoCValidationException {
        if (ioc == null) {
            throw new IoCValidationException("IoC record is null");
        }
        if (ioc.getFeed() == null || ioc.getFeed().getName() == null) {
            throw new IoCValidationException("IoC record doesn't have mandatory field 'feed.name'");
        }
        if (ioc.getSource() == null ||
                ioc.getSource().getFqdn() == null && ioc.getSource().getIp() == null) {
            throw new IoCValidationException("IoC can't have both IP and FQDN set as null");
        }
        return ioc;
    }
}
