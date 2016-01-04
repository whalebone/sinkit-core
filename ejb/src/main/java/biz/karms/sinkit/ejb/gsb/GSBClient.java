package biz.karms.sinkit.ejb.gsb;

import biz.karms.sinkit.ejb.gsb.dto.FullHashLookupResponse;

import javax.ejb.Remote;

/**
 * Created by tom on 12/5/15.
 *
 * @author Tomas Kozel
 */
@Remote
public interface GSBClient {
    FullHashLookupResponse getFullHashes(byte[] hashPrefix);
}
