package biz.karms.sinkit.ejb.gsb.dto;


import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by tom on 12/5/15.
 *
 * @author Tomas Kozel
 */
public class FullHashLookupResponse implements Serializable {

    private static final long serialVersionUID = 21808366131140791L;

    private int validSeconds;

    // blacklistName -> {fullhashString -> [fullhash, metadata(optional)]}
    private HashMap<String, ArrayList<AbstractMap.SimpleEntry<byte[], byte[]>>> fullHashes;

    public FullHashLookupResponse() {
        fullHashes = new HashMap<>();
    }

    public int getValidSeconds() {
        return validSeconds;
    }

    public void setValidSeconds(int validSeconds) {
        this.validSeconds = validSeconds;
    }

    public HashMap<String, ArrayList<AbstractMap.SimpleEntry<byte[], byte[]>>> getFullHashes() {
        return fullHashes;
    }

    public void setFullHashes(HashMap<String, ArrayList<AbstractMap.SimpleEntry<byte[], byte[]>>> fullHashes) {
        this.fullHashes = fullHashes;
    }
}
