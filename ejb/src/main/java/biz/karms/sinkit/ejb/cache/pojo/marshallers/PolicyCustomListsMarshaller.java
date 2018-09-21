package biz.karms.sinkit.ejb.cache.pojo.marshallers;

import biz.karms.sinkit.resolver.PolicyCustomList;
import org.infinispan.protostream.MessageMarshaller;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public class PolicyCustomListsMarshaller implements MessageMarshaller<PolicyCustomList> {

    @Override
    public String getTypeName() {
        return "sinkitprotobuf.PolicyCustomList";
    }

    @Override
    public Class<PolicyCustomList> getJavaClass() {
        return PolicyCustomList.class;
    }

    @Override
    public PolicyCustomList readFrom(ProtoStreamReader reader) throws IOException {
        final Set<String> blackList = reader.readCollection("blackList", new LinkedHashSet<>(), String.class);
        final Set<String> whiteList = reader.readCollection("whiteList", new LinkedHashSet<>(), String.class);
        final Set<String> auditList = reader.readCollection("auditList", new LinkedHashSet<>(), String.class);
        final Set<String> dropList = reader.readCollection("dropList", new LinkedHashSet<>(), String.class);

        final PolicyCustomList customLists = new PolicyCustomList();
        customLists.setBlackList(blackList);
        customLists.setWhiteList(whiteList);
        customLists.setAuditList(auditList);
        customLists.setDropList(dropList);
        return customLists;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, PolicyCustomList customLists) throws IOException {
        writer.writeCollection("blackList", Optional.ofNullable(customLists.getBlackList()).orElse(new HashSet<>()), String.class);
        writer.writeCollection("whiteList", Optional.ofNullable(customLists.getWhiteList()).orElse(new HashSet<>()), String.class);
        writer.writeCollection("auditList", Optional.ofNullable(customLists.getAuditList()).orElse(new HashSet<>()), String.class);
        writer.writeCollection("dropList", Optional.ofNullable(customLists.getDropList()).orElse(new HashSet<>()), String.class);
    }
}
