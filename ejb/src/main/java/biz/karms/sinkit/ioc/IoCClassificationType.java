package biz.karms.sinkit.ioc;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

import static java.lang.String.format;

@Getter
public enum IoCClassificationType {

    @SerializedName("malware")
    malware("malware"),

    @SerializedName("c&c")
    cc("c&c"),

    @SerializedName("blacklist")
    blacklist("blacklist"),

    @SerializedName("phishing")
    phishing("phishing"),

    @SerializedName("exploit")
    exploit("exploit"),

    @SerializedName("content")
    content("content");

    private final String label;

    IoCClassificationType(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }

    public static IoCClassificationType parse(String strValue) {
        if (malware.label.equals(strValue)) {
            return malware;
        } else if (cc.label.equals(strValue)) {
            return cc;
        } else if (blacklist.label.equals(strValue)) {
            return blacklist;
        } else if (phishing.label.equals(strValue)) {
            return phishing;
        } else if (exploit.label.equals(strValue)) {
            return exploit;
        } else if (content.label.equals(strValue)) {
            return content;
        }
        throw new IllegalArgumentException(format("The value '%s' cannot be mapped into IoCClassificationType enum", strValue));
    }
}
