package biz.karms.sinkit.resolver;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Set;

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
