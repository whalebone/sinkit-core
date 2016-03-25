package biz.karms.sinkit.eventlog;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Tomas Kozel
 */
public enum EventLogAction {

    @SerializedName("block")
    BLOCK,

    @SerializedName("audit")
    AUDIT,

    @SerializedName("internal")
    INTERNAL
}
