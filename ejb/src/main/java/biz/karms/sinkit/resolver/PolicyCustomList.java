package biz.karms.sinkit.resolver;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class PolicyCustomList {
    @SerializedName("black")
    private Set<String> blackList;

    @SerializedName("drop")
    private Set<String> dropList;

    @SerializedName("white")
    private Set<String> whiteList;

    @SerializedName("audit")
    private Set<String> auditList;
}
