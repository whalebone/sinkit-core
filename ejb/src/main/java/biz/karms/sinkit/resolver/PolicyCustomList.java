package biz.karms.sinkit.resolver;

import com.google.gson.annotations.SerializedName;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

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
