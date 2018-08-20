package biz.karms.sinkit.resolver;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Strategy implements Serializable {

    @SerializedName("type")
    private StrategyType strategyType;

    @SerializedName("params")
    private StrategyParams strategyParams;
}
