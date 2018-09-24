package biz.karms.sinkit.ioc;

import java.io.Serializable;
import java.util.HashMap;
import lombok.Getter;
import lombok.Setter;

/**
 * Class representing AccuCheckerReport
 * @author Krystof Kolar
 */
@Getter
@Setter
public class IoCAccuCheckerReport implements Serializable {


    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";

    private IoCSource source;
    private HashMap<String, Integer> accuracy;
    private HashMap<String, IoCAccuCheckerMetadata> metadata;

    public IoCAccuCheckerReport(IoCRecord ioc) {
        source = ioc.getSource();
        accuracy = ioc.getAccuracy();
        metadata = ioc.getMetadata();
    }

}
