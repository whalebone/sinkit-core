package biz.karms.sinkit.resolver;

import static java.lang.String.format;

public enum StrategyType {
    accuracy,
    whitelist,
    blacklist,
    drop;

    public static StrategyType parse(String strValue) {
        if (accuracy.name().equals(strValue)) {
            return accuracy;
        } else if (whitelist.name().equals(strValue)) {
            return whitelist;
        } else if (blacklist.name().equals(strValue)) {
            return blacklist;
        } else if (drop.name().equals(strValue)) {
            return drop;
        }
        throw new IllegalArgumentException(format("The value '%s' cannot be mapped into StrategyType enum", strValue));
    }
}
