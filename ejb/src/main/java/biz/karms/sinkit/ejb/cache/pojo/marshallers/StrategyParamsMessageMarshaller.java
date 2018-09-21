package biz.karms.sinkit.ejb.cache.pojo.marshallers;


import biz.karms.sinkit.ioc.IoCClassificationType;
import biz.karms.sinkit.resolver.StrategyParams;
import org.infinispan.protostream.MessageMarshaller;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StrategyParamsMessageMarshaller implements MessageMarshaller<StrategyParams> {
    @Override
    public Class<? extends StrategyParams> getJavaClass() {
        return StrategyParams.class;
    }

    @Override
    public String getTypeName() {
        return "sinkitprotobuf.StrategyParams";
    }

    @Override
    public StrategyParams readFrom(ProtoStreamReader reader) throws IOException {
        final Integer audit = reader.readInt("audit");
        final Integer block = reader.readInt("block");
        final Set<String> typesValues = reader.readCollection("types", new HashSet<>(), String.class);
        final Set<IoCClassificationType> types = typesValues.stream().map(IoCClassificationType::parse).collect(Collectors.toSet());

        final StrategyParams params = new StrategyParams();
        params.setAudit(audit);
        params.setBlock(block);
        params.setTypes(types);
        return params;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, StrategyParams strategyParams) throws IOException {
        writer.writeInt("audit", strategyParams.getAudit());
        writer.writeInt("block", strategyParams.getBlock());

        final Set<String> typesValues = Optional.ofNullable(strategyParams.getTypes()).map(Collection::stream).orElse(Stream.empty())
                .map(IoCClassificationType::getLabel).collect(Collectors.toSet());
        writer.writeCollection("types", typesValues, String.class);
    }
}
