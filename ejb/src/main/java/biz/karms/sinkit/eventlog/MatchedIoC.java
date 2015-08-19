package biz.karms.sinkit.eventlog;

import biz.karms.sinkit.ioc.IoCRecord;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Created by tkozel on 16.8.15.
 */
public class MatchedIoC implements Serializable {

    private static final long serialVersionUID = 2929470294569599565L;

    @SerializedName("id")
    String documentId;

    @SerializedName("ioc")
    IoCRecord ioc;

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public IoCRecord getIoc() {
        return ioc;
    }

    public void setIoc(IoCRecord ioc) {
        this.ioc = ioc;
    }

    @Override
    public String toString() {
        return "MatchedIoC{" +
                "documentId='" + documentId + '\'' +
                ", ioc=" + ioc +
                '}';
    }
}
