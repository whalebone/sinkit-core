package biz.karms.sinkit.resolver;

import biz.karms.sinkit.ioc.IoCClassificationType;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Set;

@Getter
@Setter
public class StrategyParams implements Serializable {
    private Integer audit;
    private Integer block;
    private Set<IoCClassificationType> types;
}
