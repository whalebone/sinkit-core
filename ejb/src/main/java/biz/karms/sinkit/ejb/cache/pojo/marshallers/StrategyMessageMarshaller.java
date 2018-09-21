package biz.karms.sinkit.ejb.cache.pojo.marshallers;


import biz.karms.sinkit.resolver.Strategy;
import biz.karms.sinkit.resolver.StrategyParams;
import biz.karms.sinkit.resolver.StrategyType;
import org.infinispan.protostream.MessageMarshaller;

import java.io.IOException;
import java.util.Optional;

public class StrategyMessageMarshaller implements MessageMarshaller<Strategy> {
    @Override
    public Class<? extends Strategy> getJavaClass() {
        return Strategy.class;
    }

    @Override
    public String getTypeName() {
        return "sinkitprotobuf.Strategy";
    }

    @Override
    public Strategy readFrom(ProtoStreamReader reader) throws IOException {
        final String strategyTypeStr = reader.readString("strategyType");
        final StrategyType strategyType = strategyTypeStr != null ? StrategyType.parse(strategyTypeStr) : null;

        final StrategyParams params = reader.readObject("strategyParams", StrategyParams.class);
        final Strategy strategy = new Strategy();
        strategy.setStrategyType(strategyType);
        strategy.setStrategyParams(params);
        return strategy;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, Strategy strategy) throws IOException {
        writer.writeString("strategyType", strategy.getStrategyType().name());
        writer.writeObject("strategyParams", Optional.ofNullable(strategy.getStrategyParams()).orElse(new StrategyParams()), StrategyParams.class);
    }


}
