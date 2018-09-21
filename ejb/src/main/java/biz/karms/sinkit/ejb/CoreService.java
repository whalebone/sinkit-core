package biz.karms.sinkit.ejb;

import biz.karms.sinkit.ejb.cache.pojo.WhitelistedRecord;
import biz.karms.sinkit.exception.ArchiveException;
import biz.karms.sinkit.exception.IoCValidationException;
import biz.karms.sinkit.ioc.IoCAccuCheckerReport;
import biz.karms.sinkit.ioc.IoCRecord;

import javax.ejb.Local;

/**
 * @author Michal Karm Babacek
 */
@Local
public interface CoreService {
    IoCRecord processIoCRecord(IoCRecord receivedIoc) throws ArchiveException, IoCValidationException;

    boolean updateWithAccuCheckerReport(IoCAccuCheckerReport report) throws ArchiveException, IoCValidationException;

    int getIocActiveHours();

    int deactivateIocs() throws ArchiveException;

    boolean runCacheRebuilding();

    void enrich();

    boolean processWhitelistIoCRecord(final IoCRecord white) throws IoCValidationException, ArchiveException;

    void setWhitelistValidSeconds(long whitelistValidSeconds);

    WhitelistedRecord getWhitelistedRecord(String id);

    boolean removeWhitelistedRecord(String id);

    boolean isWhitelistEmpty();
}
