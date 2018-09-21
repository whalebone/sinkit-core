package biz.karms.sinkit.ejb.cache.pojo.marshallers;


import biz.karms.sinkit.resolver.EndUserConfiguration;
import org.infinispan.protostream.MessageMarshaller;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class EndUserConfigurationMessageMarshaller implements MessageMarshaller<EndUserConfiguration> {

    @Override
    public Class<? extends EndUserConfiguration> getJavaClass() {
        return EndUserConfiguration.class;
    }

    @Override
    public String getTypeName() {
        return "sinkitprotobuf.EndUserConfiguration";
    }

    @Override
    public EndUserConfiguration readFrom(ProtoStreamReader reader) throws IOException {
        final int clientId = reader.readInt("clientId");
        final String userId = reader.readString("userId");
        final int policyId = reader.readInt("policyId");
        final Set<String> identities = reader.readCollection("identities", new HashSet<>(), String.class);
        final Set<String> whitelist = reader.readCollection("whitelist", new HashSet<>(), String.class);
        final Set<String> blacklist = reader.readCollection("blacklist", new HashSet<>(), String.class);

        final EndUserConfiguration configuration = new EndUserConfiguration();
        configuration.setClientId(clientId);
        configuration.setUserId(userId);
        configuration.setPolicyId(policyId);
        configuration.setIdentities(identities);
        configuration.setWhitelist(whitelist);
        configuration.setBlacklist(blacklist);
        return configuration;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, EndUserConfiguration endUserConfiguration) throws IOException {
        writer.writeInt("clientId", endUserConfiguration.getClientId());
        writer.writeString("userId", endUserConfiguration.getUserId());
        writer.writeInt("policyId", endUserConfiguration.getPolicyId());
        writer.writeCollection("identities", Optional.ofNullable(endUserConfiguration.getIdentities()).orElse(new HashSet<>()), String.class);
        writer.writeCollection("whitelist", Optional.ofNullable(endUserConfiguration.getWhitelist()).orElse(new HashSet<>()), String.class);
        writer.writeCollection("blacklist", Optional.ofNullable(endUserConfiguration.getBlacklist()).orElse(new HashSet<>()), String.class);
    }

}
