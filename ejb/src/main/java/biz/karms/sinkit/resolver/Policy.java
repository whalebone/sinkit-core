package biz.karms.sinkit.resolver;

import java.io.Serializable;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Policy implements Serializable {
    private int id;
    private Set<String> ipRanges;
    private Strategy strategy;

    private Set<String> accuracyFeeds;
    private Set<String> blacklistedFeeds;
    private PolicyCustomList customlists;
}
