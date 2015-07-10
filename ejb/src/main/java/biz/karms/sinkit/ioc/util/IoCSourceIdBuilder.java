package biz.karms.sinkit.ioc.util;

import biz.karms.sinkit.exception.IoCSourceIdException;
import biz.karms.sinkit.ioc.IoCRecord;
import biz.karms.sinkit.ioc.IoCSourceId;
import biz.karms.sinkit.ioc.IoCSourceIdType;

/**
 * Created by tkozel on 10.7.15.
 */
public class IoCSourceIdBuilder {

    public static IoCSourceId build(IoCRecord ioc) throws IoCSourceIdException {

        IoCSourceId sid = new IoCSourceId();
        if (ioc.getSource().getUrl() != null ) {
            sid.setValue(ioc.getSource().getUrl());
            sid.setType(IoCSourceIdType.URL);
        } else if (ioc.getSource().getFQDN() != null) {
            sid.setValue(ioc.getSource().getFQDN());
            sid.setType(IoCSourceIdType.FQDN);
        } else if (ioc.getSource().getIp() != null) {
            sid.setValue(ioc.getSource().getIp());
            sid.setType(IoCSourceIdType.IP);
        } else {
            throw new IoCSourceIdException("Can't build IoCSourceId: Unknown type of IoC source.");
        }

        return sid;
    }
}
