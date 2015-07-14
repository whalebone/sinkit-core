package biz.karms.sinkit.ioc;

import com.google.gson.annotations.SerializedName;

/**
 * Created by tkozel on 10.7.15.
 */
public enum IoCSourceIdType {

    @SerializedName("url")
    URL,

    @SerializedName("ip")
    IP,

    @SerializedName("fqdn")
    FQDN
}
