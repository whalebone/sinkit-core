package biz.karms.sinkit.resolver;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class ResolverConfiguration implements Serializable {

    private Integer resolverId;
    private Integer clientId;
    private List<Policy> policies;

}
