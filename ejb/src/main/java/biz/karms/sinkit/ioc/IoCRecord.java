package biz.karms.sinkit.ioc;

import biz.karms.sinkit.ejb.elastic.Indexable;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
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
    private HashMap<String, String> metadata;


    public HashMap<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(HashMap<String, String> meta) {
        this.metadata = meta;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
