package biz.karms.sinkit.ejb.cache.pojo.marshallers;


import biz.karms.sinkit.resolver.Policy;
import biz.karms.sinkit.resolver.PolicyCustomList;
import biz.karms.sinkit.resolver.Strategy;
import org.infinispan.protostream.MessageMarshaller;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public class PolicyMessageMarshaller implements MessageMarshaller<Policy> {

    @Override
    public Class<? extends Policy> getJavaClass() {
        return Policy.class;
    }

    @Override
    public String getTypeName() {
        return "sinkitprotobuf.Policy";
    }

    @Override
    public Policy readFrom(ProtoStreamReader reader) throws IOException {
        final int id = reader.readInt("id");
        final Set<String> ipRanges = reader.readCollection("ipRanges", new LinkedHashSet<>(), String.class);
        final Set<String> accuracyFeeds = reader.readCollection("accuracyFeeds", new LinkedHashSet<>(), String.class);
        final Set<String> blacklistedFeeds = reader.readCollection("blacklistedFeeds", new LinkedHashSet<>(), String.class);
        final Strategy strategy = reader.readObject("strategy", Strategy.class);
        final PolicyCustomList customList = reader.readObject("customlists", PolicyCustomList.class);
        final Policy policy = new Policy();

        policy.setId(id);
        policy.setIpRanges(ipRanges);
        policy.setAccuracyFeeds(accuracyFeeds);
        policy.setBlacklistedFeeds(blacklistedFeeds);
        policy.setStrategy(strategy);
        policy.setCustomlists(customList);
        return policy;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, Policy policy) throws IOException {
        writer.writeInt("id", policy.getId());
        writer.writeCollection("ipRanges", Optional.ofNullable(policy.getIpRanges()).orElse(new LinkedHashSet<>()), String.class);
        writer.writeCollection("accuracyFeeds", Optional.ofNullable(policy.getAccuracyFeeds()).orElse(new LinkedHashSet<>()), String.class);
        writer.writeCollection("blacklistedFeeds", Optional.ofNullable(policy.getBlacklistedFeeds()).orElse(new LinkedHashSet<>()), String.class);
        writer.writeObject("strategy", policy.getStrategy(), Strategy.class);
        writer.writeObject("customlists", Optional.ofNullable(policy.getCustomlists()).orElse(new PolicyCustomList()), PolicyCustomList.class);
    }


}
