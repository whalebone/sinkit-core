package biz.karms.sinkit.eventlog;

import com.google.gson.annotations.SerializedName;

/**
 * Created by tkozel on 23.7.15.
 */
public enum EventLogAction {

    @SerializedName("block")
    BLOCK,

    @SerializedName("audit")
    AUDIT,

    @SerializedName("internal")
    INTERNAL
}
