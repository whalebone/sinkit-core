package biz.karms.sinkit.ioc;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Class representing AccuCheckerReport
 */
public class IoCAccuCheckerReport implements Serializable {


    private IoCSource source;
    private HashMap<String, Integer> accuracy;
    private HashMap<String, String> metadata;

    public IoCAccuCheckerReport(IoCRecord ioc) {
        source = ioc.getSource();
        accuracy = ioc.getAccuracy();
        metadata = ioc.getMetadata();
    }

    public IoCSource getSource() {
        return source;
    }

    public void setSource(IoCSource source) {
        this.source = source;
    }

    public HashMap<String, Integer> getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(HashMap<String, Integer> accuracy) {
        this.accuracy = accuracy;
    }

    public HashMap<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(HashMap<String, String> meta) {
        this.metadata = meta;
    }


}
