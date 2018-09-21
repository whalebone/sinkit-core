package biz.karms.sinkit.ejb.cache.pojo.marshallers;


import biz.karms.sinkit.resolver.Policy;
import biz.karms.sinkit.resolver.ResolverConfiguration;
import org.infinispan.protostream.MessageMarshaller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ResolverConfigurationMessageMarshaller implements MessageMarshaller<ResolverConfiguration> {

    @Override
    public Class<? extends ResolverConfiguration> getJavaClass() {
        return ResolverConfiguration.class;
    }

    @Override
    public String getTypeName() {
        return "sinkitprotobuf.ResolverConfiguration";
    }

    @Override
    public ResolverConfiguration readFrom(ProtoStreamReader reader) throws IOException {
        final int resolverId = reader.readInt("resolverId");
        final int clientId = reader.readInt("clientId");
        List<Policy> policies = reader.readCollection("policies", new ArrayList<>(), Policy.class);

        final ResolverConfiguration configuration = new ResolverConfiguration();
        configuration.setResolverId(resolverId);
        configuration.setClientId(clientId);
        configuration.setPolicies(policies);
        return configuration;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, ResolverConfiguration resolverConfiguration) throws IOException {
        writer.writeInt("resolverId", resolverConfiguration.getResolverId());
        writer.writeInt("clientId", resolverConfiguration.getClientId());
        writer.writeCollection("policies", resolverConfiguration.getPolicies(), Policy.class);
    }

}
