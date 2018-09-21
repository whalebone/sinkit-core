package biz.karms.sinkit.resolver;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class Strategy implements Serializable {

    @SerializedName("type")
    private StrategyType strategyType;

    @SerializedName("params")
    private StrategyParams strategyParams;
}
