package biz.karms.sinkit.tests.util;

import biz.karms.sinkit.ejb.cache.pojo.BlacklistedRecord;
import biz.karms.sinkit.ejb.cache.pojo.CustomList;
import biz.karms.sinkit.ejb.cache.pojo.GSBRecord;
import biz.karms.sinkit.ejb.cache.pojo.Rule;
import biz.karms.sinkit.ejb.cache.pojo.WhitelistedRecord;
import biz.karms.sinkit.ejb.cache.pojo.marshallers.CustomListMarshaller;
import biz.karms.sinkit.ejb.cache.pojo.marshallers.ImmutablePairMarshaller;
import biz.karms.sinkit.ejb.cache.pojo.marshallers.RuleMarshaller;
import org.apache.commons.io.IOUtils;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;

import java.io.IOException;

/**
 * @author Michal Karm Babacek
 */
public class InfinispanManager {
    private static final String RULE_PROTOBUF_DEFINITION_RESOURCE = "/sinkitprotobuf/rule.proto";
    private static final String CUSTOM_LIST_PROTOBUF_DEFINITION_RESOURCE = "/sinkitprotobuf/customlist.proto";
    private static final String SINKIT_CACHE_PROTOBUF = "/sinkitprotobuf/sinkit-cache.proto";

    private RemoteCacheManager cacheManagerForIndexableCaches;
    private RemoteCacheManager cacheManager;

    private RemoteCache<String, Rule> ruleRemoteCache;
    private RemoteCache<String, CustomList> customListRemoteCache;
    private RemoteCache<String, BlacklistedRecord> blacklistRemoteCache;
    private RemoteCache<String, WhitelistedRecord> whitelistRemoteCache;
    private RemoteCache<String, GSBRecord> gsbRemoteCache;


    private static InfinispanManager infinispanManager;

    private InfinispanManager() {
        final String hotrodHost = System.getProperty("hotrod_host");
        final int hotrodPort = Integer.parseInt(System.getProperty("hotrod_port"));
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.addServer()
                .host(hotrodHost)
                .port(hotrodPort)
                .marshaller(new ProtoStreamMarshaller());
        cacheManagerForIndexableCaches = new RemoteCacheManager(builder.build());
        builder = new ConfigurationBuilder();
        builder.addServer()
                .host(hotrodHost)
                .port(hotrodPort);
        cacheManager = new RemoteCacheManager(builder.build());

        try {
            SerializationContext ctx = ProtoStreamMarshaller.getSerializationContext(cacheManagerForIndexableCaches);
            ctx.registerProtoFiles(FileDescriptorSource.fromResources(RULE_PROTOBUF_DEFINITION_RESOURCE));
            ctx.registerMarshaller(new RuleMarshaller());
            ctx.registerMarshaller(new ImmutablePairMarshaller());
            ctx.registerProtoFiles(FileDescriptorSource.fromResources(CUSTOM_LIST_PROTOBUF_DEFINITION_RESOURCE));
            ctx.registerMarshaller(new CustomListMarshaller());
            RemoteCache<String, String> metadataCache = cacheManagerForIndexableCaches.getCache(ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME);
            metadataCache.put(RULE_PROTOBUF_DEFINITION_RESOURCE, IOUtils.toString(getClass().getResourceAsStream(RULE_PROTOBUF_DEFINITION_RESOURCE), "UTF-8"));
            metadataCache.put(CUSTOM_LIST_PROTOBUF_DEFINITION_RESOURCE, IOUtils.toString(getClass().getResourceAsStream(CUSTOM_LIST_PROTOBUF_DEFINITION_RESOURCE), "UTF-8"));
            String errors = metadataCache.get(ProtobufMetadataManagerConstants.ERRORS_KEY_SUFFIX);
            if (errors != null) {
                throw new IllegalStateException("Some Protobuf schema files contain errors:\n" + errors);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to register protofiles.");
        }

        ruleRemoteCache = cacheManagerForIndexableCaches.getCache("infinispan_rules");
        if (ruleRemoteCache == null) {
            throw new RuntimeException("Cache 'infinispan_rules' not found.");
        }
        customListRemoteCache = cacheManagerForIndexableCaches.getCache("infinispan_custom_lists");
        if (customListRemoteCache == null) {
            throw new RuntimeException("Cache 'infinispan_custom_lists' not found.");
        }
        blacklistRemoteCache = cacheManager.getCache("infinispan_blacklist");
        if (blacklistRemoteCache == null) {
            throw new RuntimeException("Cache 'infinispan_blacklist' not found.");
        }
        whitelistRemoteCache = cacheManager.getCache("infinispan_whitelist");
        if (whitelistRemoteCache == null) {
            throw new RuntimeException("Cache 'infinispan_whitelist' not found.");
        }
        gsbRemoteCache = cacheManager.getCache("infinispan_gsb");
        if (gsbRemoteCache == null) {
            throw new RuntimeException("Cache 'infinispan_gsb' not found.");
        }
    }

    public static InfinispanManager getInfinispanManager() {
        if (infinispanManager == null) {
            infinispanManager = new InfinispanManager();
        }
        return infinispanManager;
    }

    public RemoteCache<String, Rule> getRuleRemoteCache() {
        return ruleRemoteCache;
    }

    public RemoteCache<String, CustomList> getCustomListRemoteCache() {
        return customListRemoteCache;
    }

    public RemoteCache<String, BlacklistedRecord> getBlacklistRemoteCache() {
        return blacklistRemoteCache;
    }

    public RemoteCache<String, WhitelistedRecord> getWhitelistRemoteCache() {
        return whitelistRemoteCache;
    }

    public RemoteCache<String, GSBRecord> getGsbRemoteCache() {
        return gsbRemoteCache;
    }
}

