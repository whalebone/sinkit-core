package biz.karms.sinkit.resolver;

import java.io.Serializable;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResolverConfiguration implements Serializable{

    private Integer resolverId;
    private Integer clientId;
    private List<Policy> policies;

}
