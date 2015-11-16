package biz.karms.sinkit.ejb;

import biz.karms.sinkit.exception.ArchiveException;
import biz.karms.sinkit.exception.IoCValidationException;
import biz.karms.sinkit.ioc.IoCRecord;

import javax.ejb.Local;

/**
 * @author Michal Karm Babacek
 */
@Local
public interface CoreService {
    IoCRecord processIoCRecord(IoCRecord receivedIoc) throws ArchiveException, IoCValidationException;

    int getIocActiveHours();

    int deactivateIocs() throws ArchiveException;

    boolean runCacheRebuilding();

    void enrich();
}
