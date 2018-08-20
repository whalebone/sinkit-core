package biz.karms.sinkit.ioc.util;

import biz.karms.sinkit.exception.IoCSourceIdException;
import biz.karms.sinkit.ioc.IoCRecord;
import biz.karms.sinkit.ioc.IoCSourceId;
import biz.karms.sinkit.ioc.IoCSourceIdType;

/**
 * @author Tomas Kozel
 */
public class IoCSourceIdBuilder {

    public static IoCSourceId build(IoCRecord ioc) throws IoCSourceIdException {

        final IoCSourceId sid = new IoCSourceId();
        if (ioc.getSource().getFqdn() != null) {
            sid.setValue(ioc.getSource().getFqdn());
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
