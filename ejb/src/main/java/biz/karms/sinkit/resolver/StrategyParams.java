package biz.karms.sinkit.resolver;

import biz.karms.sinkit.ioc.IoCClassificationType;
import java.io.Serializable;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StrategyParams implements Serializable {
    private Integer audit;
    private Integer block;
    private Set<IoCClassificationType> types;
}
