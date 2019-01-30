package biz.karms.sinkit.ioc;

import biz.karms.sinkit.ejb.elastic.Indexable;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;

/**
 * @author Tomas Kozel
 */
@Getter
@Setter
public class IoCRecord implements Indexable {

    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";

    private static final long serialVersionUID = -4129116350560247954L;

    @SerializedName("document_id")
    private String documentId;
    @SerializedName("unique_ref")
    private String uniqueRef;
    private IoCFeed feed;
    private IoCDescription description;
    private IoCClassification classification;
    private IoCProtocol protocol;
    private String raw;
    private IoCSource source;
    private IoCTime time;
    private IoCSeen seen;
    private Boolean active;
    @SerializedName("whitelist_name")
    private String whitelistName;
    private HashMap<String, Integer> accuracy;
    @Setter(AccessLevel.NONE)
    private Integer accuracySum;
    private HashMap<String, IoCAccuCheckerMetadata> metadata;

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    public void setAccuracy(HashMap<String, Integer> accuracy) {
        this.accuracy = accuracy;
        if(accuracy != null){
            this.accuracySum=accuracy.values().stream().reduce(0,Integer::sum);
        }

    }

    public void updateWithAccuCheckerReport(IoCAccuCheckerReport report){
        HashMap<String, Integer> combinedAccuracy = new HashMap<>();
        if (this.accuracy != null) { //nullity shouldn't happen
            combinedAccuracy.putAll(this.accuracy);
        }
        // report gets inserted after original ioc.accuracy to update old values corresponding to the given accuchecker feed
        combinedAccuracy.putAll(report.getAccuracy());
        setAccuracy(combinedAccuracy);

        HashMap<String, IoCAccuCheckerMetadata> combinedMetadata = new HashMap<>();
        if (this.metadata != null) { //nullity shouldn't happen
            combinedMetadata.putAll(this.metadata);
        }
        combinedMetadata.putAll(report.getMetadata());
        setMetadata(combinedMetadata);

    }
}
