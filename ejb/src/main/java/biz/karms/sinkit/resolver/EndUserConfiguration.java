package biz.karms.sinkit.resolver;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Set;

@Getter
@Setter
public class EndUserConfiguration implements Serializable {
    private Integer clientId;
    private String userId;

    @SerializedName("policy")
    private Integer policyId;

    private Set<String> identities;
    private Set<String> whitelist;
    private Set<String> blacklist;

    public String getId() {
        return new StringBuilder().append(clientId).append(":").append(userId).toString();
    }
}
