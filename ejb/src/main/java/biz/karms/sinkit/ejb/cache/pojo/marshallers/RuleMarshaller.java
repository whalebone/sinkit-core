package biz.karms.sinkit.ejb.cache.pojo.marshallers;

import biz.karms.sinkit.ejb.cache.pojo.Rule;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.infinispan.protostream.MessageMarshaller;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Michal Karm Babacek
 */
public class RuleMarshaller implements MessageMarshaller<Rule> {

    @Override
    public String getTypeName() {
        return "sinkitprotobuf.Rule";
    }

    @Override
    public Class<Rule> getJavaClass() {
        return Rule.class;
    }

    @Override
    public Rule readFrom(ProtoStreamReader reader) throws IOException {
        final String startAddress = reader.readString("startAddress");
        final String endAddress = reader.readString("endAddress");
        final String cidrAddress = reader.readString("cidrAddress");
        final int name = reader.readInt("customerId");
        final HashMap<String, String> sources = new HashMap<>();
        reader.readCollection("sources", new HashSet<>(), ImmutablePair.class).forEach(p -> sources.put((String) p.getLeft(), (String) p.getRight()));
        final Rule rule = new Rule();
        rule.setStartAddress(startAddress);
        rule.setEndAddress(endAddress);
        rule.setCidrAddress(cidrAddress);
        rule.setCustomerId(name);
        rule.setSources(sources);
        return rule;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, Rule rule) throws IOException {
        writer.writeString("startAddress", rule.getStartAddress());
        writer.writeString("endAddress", rule.getEndAddress());
        writer.writeString("cidrAddress", rule.getCidrAddress());
        writer.writeInt("customerId", rule.getCustomerId());
        final List<ImmutablePair> sources = rule.getSources().entrySet().stream().map(e -> new ImmutablePair<>(e.getKey(), e.getValue())).collect(Collectors.toList());
        writer.writeCollection("sources", sources, ImmutablePair.class);
    }
}
