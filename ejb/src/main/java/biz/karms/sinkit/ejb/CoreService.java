package biz.karms.sinkit.ejb;

import biz.karms.sinkit.exception.ArchiveException;
import biz.karms.sinkit.exception.IoCValidationException;
import biz.karms.sinkit.ioc.IoCRecord;

import javax.ejb.Remote;

/**
 * @author Michal Karm Babacek
 */
@Remote
public interface CoreService {
    IoCRecord processIoCRecord(IoCRecord receivedIoc) throws ArchiveException, IoCValidationException;

    int deactivateIocs() throws ArchiveException;

    boolean runCacheRebuilding();

    void enrich();
}
