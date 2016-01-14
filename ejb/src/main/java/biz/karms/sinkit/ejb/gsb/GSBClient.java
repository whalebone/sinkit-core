package biz.karms.sinkit.ejb.gsb;

import biz.karms.sinkit.ejb.gsb.dto.FullHashLookupResponse;

import javax.ejb.Local;

/**
 * Created by tom on 12/5/15.
 *
 * @author Tomas Kozel
 */
@Local
public interface GSBClient {
    FullHashLookupResponse getFullHashes(byte[] hashPrefix);
}
