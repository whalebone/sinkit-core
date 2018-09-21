package biz.karms.sinkit.ejb;

import biz.karms.sinkit.ejb.cache.annotations.SinkitCache;
import biz.karms.sinkit.ejb.cache.annotations.SinkitCacheName;
import biz.karms.sinkit.ejb.cache.pojo.BlacklistedRecord;
import biz.karms.sinkit.ejb.cache.pojo.CustomList;
import biz.karms.sinkit.ejb.cache.pojo.GSBRecord;
import biz.karms.sinkit.ejb.cache.pojo.Rule;
import biz.karms.sinkit.ejb.cache.pojo.WhitelistedRecord;
import biz.karms.sinkit.ejb.cache.pojo.marshallers.CustomListMarshaller;
import biz.karms.sinkit.ejb.cache.pojo.marshallers.EndUserConfigurationMessageMarshaller;
import biz.karms.sinkit.ejb.cache.pojo.marshallers.ImmutablePairMarshaller;
import biz.karms.sinkit.ejb.cache.pojo.marshallers.PolicyCustomListsMarshaller;
import biz.karms.sinkit.ejb.cache.pojo.marshallers.PolicyMessageMarshaller;
import biz.karms.sinkit.ejb.cache.pojo.marshallers.ResolverConfigurationMessageMarshaller;
import biz.karms.sinkit.ejb.cache.pojo.marshallers.RuleMarshaller;
import biz.karms.sinkit.ejb.cache.pojo.marshallers.StrategyMessageMarshaller;
import biz.karms.sinkit.ejb.cache.pojo.marshallers.StrategyParamsMessageMarshaller;
import biz.karms.sinkit.resolver.EndUserConfiguration;
import biz.karms.sinkit.resolver.ResolverConfiguration;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.exceptions.TransportException;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.commons.api.BasicCacheContainer;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.eviction.EvictionType;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;
import org.infinispan.util.concurrent.IsolationLevel;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Michal Karm Babacek
 */
@ApplicationScoped
public class MyCacheManagerProvider {

    private static final long SINKIT_LOCAL_CACHE_SIZE = (System.getenv().containsKey("SINKIT_LOCAL_CACHE_SIZE")) ? Integer.parseInt(System.getenv("SINKIT_LOCAL_CACHE_SIZE")) : 10000;
    private static final long SINKIT_LOCAL_CACHE_LIFESPAN_MS = (System.getenv().containsKey("SINKIT_LOCAL_CACHE_LIFESPAN_MS")) ? Integer.parseInt(System.getenv("SINKIT_LOCAL_CACHE_LIFESPAN_MS")) : 180000;
    // This call fails if the property is undefined
    private static final String SINKIT_HOTROD_HOST = System.getenv("SINKIT_HOTROD_HOST");
    private static final int SINKIT_HOTROD_PORT = Integer.parseInt(System.getenv("SINKIT_HOTROD_PORT"));
    private static final String RULE_PROTOBUF_DEFINITION_RESOURCE = "/sinkitprotobuf/rule.proto";
    private static final String CUSTOM_LIST_PROTOBUF_DEFINITION_RESOURCE = "/sinkitprotobuf/customlist.proto";
    private static final long SINKIT_HOTROD_CONN_TIMEOUT_S = (System.getenv().containsKey("SINKIT_HOTROD_CONN_TIMEOUT_S")) ? Integer.parseInt(System.getenv("SINKIT_HOTROD_CONN_TIMEOUT_S")) : 300;
    private static final String RESOLVER_CONFIGURATION_PROTOBUF_DEFINITION_RESOURCE = "/sinkitprotobuf/resolver_configuration.proto";
    private static final String END_USER_CONFIGURATION_PROTOBUF_DEFINITION_RESOURCE = "/sinkitprotobuf/end_user_configuration.proto";

    @Inject
    private Logger log;

    private BasicCacheContainer localCacheManager;
    private RemoteCacheManager cacheManagerForIndexableCaches;
    private RemoteCacheManager cacheManager;

    public void init(@Observes @Initialized(ApplicationScoped.class) Object init) throws IOException, InterruptedException {

        log.log(Level.INFO, "Constructing caches...");

        if (localCacheManager == null) {
            final GlobalConfiguration glob = new GlobalConfigurationBuilder()
                    .nonClusteredDefault()
                    .globalJmxStatistics().allowDuplicateDomains(true)
                    .build();
            final Configuration loc = new ConfigurationBuilder()
                    .jmxStatistics().disable()
                    .clustering().cacheMode(CacheMode.LOCAL)
                    .locking().isolationLevel(IsolationLevel.READ_UNCOMMITTED)
                    .eviction().strategy(EvictionStrategy.LRU)
                    .type(EvictionType.COUNT).size(SINKIT_LOCAL_CACHE_SIZE)
                    .expiration().lifespan(SINKIT_LOCAL_CACHE_LIFESPAN_MS, TimeUnit.MILLISECONDS)
                    .build();
            localCacheManager = new DefaultCacheManager(glob, loc, true);
            log.info("Local cache manager initialized.\n");
        }

        if (cacheManagerForIndexableCaches == null) {
            org.infinispan.client.hotrod.configuration.ConfigurationBuilder builder = new org.infinispan.client.hotrod.configuration.ConfigurationBuilder();
            builder.addServer()
                    .host(SINKIT_HOTROD_HOST)
                    .port(SINKIT_HOTROD_PORT)
                    .marshaller(new ProtoStreamMarshaller());
            cacheManagerForIndexableCaches = new RemoteCacheManager(builder.build());
        }

        if (cacheManager == null) {
            org.infinispan.client.hotrod.configuration.ConfigurationBuilder builder = new org.infinispan.client.hotrod.configuration.ConfigurationBuilder();
            builder.addServer()
                    .host(SINKIT_HOTROD_HOST)
                    .port(SINKIT_HOTROD_PORT);
            cacheManager = new RemoteCacheManager(builder.build());
        }

        final SerializationContext ctx = ProtoStreamMarshaller.getSerializationContext(cacheManagerForIndexableCaches);
        ctx.registerProtoFiles(FileDescriptorSource.fromResources(RULE_PROTOBUF_DEFINITION_RESOURCE));
        ctx.registerMarshaller(new RuleMarshaller());
        ctx.registerMarshaller(new ImmutablePairMarshaller());
        ctx.registerProtoFiles(FileDescriptorSource.fromResources(CUSTOM_LIST_PROTOBUF_DEFINITION_RESOURCE));
        ctx.registerMarshaller(new CustomListMarshaller());
        ctx.registerProtoFiles(FileDescriptorSource.fromResources(RESOLVER_CONFIGURATION_PROTOBUF_DEFINITION_RESOURCE));
        ctx.registerMarshaller(new ResolverConfigurationMessageMarshaller());
        ctx.registerMarshaller(new PolicyMessageMarshaller());
        ctx.registerMarshaller(new StrategyMessageMarshaller());
        ctx.registerMarshaller(new StrategyParamsMessageMarshaller());
        ctx.registerMarshaller(new PolicyCustomListsMarshaller());
        ctx.registerProtoFiles(FileDescriptorSource.fromResources(END_USER_CONFIGURATION_PROTOBUF_DEFINITION_RESOURCE));
        ctx.registerMarshaller(new EndUserConfigurationMessageMarshaller());


        long timestamp = System.currentTimeMillis();
        while (!setupMetadataCache() && System.currentTimeMillis() - timestamp < SINKIT_HOTROD_CONN_TIMEOUT_S) {
            log.log(Level.INFO, "XXX");

            Thread.sleep(1000L);
            log.log(Level.INFO, "Waiting for the Hot Rod server on " + SINKIT_HOTROD_HOST + ":" + SINKIT_HOTROD_PORT + " to come up until " + (System.currentTimeMillis() - timestamp) + " < " + SINKIT_HOTROD_CONN_TIMEOUT_S);
        }

        log.log(Level.INFO, "Managers started.");
    }

    private boolean setupMetadataCache() throws IOException {
        log.log(Level.INFO, "setupMetadataCache");

        try {
            final RemoteCache<String, String> metadataCache = cacheManagerForIndexableCaches.getCache(ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME);
            metadataCache.put(RULE_PROTOBUF_DEFINITION_RESOURCE, readResource(RULE_PROTOBUF_DEFINITION_RESOURCE));
            metadataCache.put(CUSTOM_LIST_PROTOBUF_DEFINITION_RESOURCE, readResource(CUSTOM_LIST_PROTOBUF_DEFINITION_RESOURCE));
            metadataCache.put(RESOLVER_CONFIGURATION_PROTOBUF_DEFINITION_RESOURCE, readResource(RESOLVER_CONFIGURATION_PROTOBUF_DEFINITION_RESOURCE));
            metadataCache.put(END_USER_CONFIGURATION_PROTOBUF_DEFINITION_RESOURCE, readResource(END_USER_CONFIGURATION_PROTOBUF_DEFINITION_RESOURCE));
            final String errors = metadataCache.get(ProtobufMetadataManagerConstants.ERRORS_KEY_SUFFIX);
            if (errors != null) {
                log.log(Level.SEVERE, "Protobuffer files, (Rule, CustomLists or ResolverConfiguration) contained errors:\n" + errors);
                return false;
            }
        } catch (TransportException ex) {
            return false;
        }
        return true;
    }

    @Produces
    @ApplicationScoped
    @SinkitCache(SinkitCacheName.cache_manager_indexable)
    public RemoteCacheManager getCacheManagerForIndexableCaches() {
        if (cacheManagerForIndexableCaches == null) {
            throw new IllegalArgumentException("cacheManagerForIndexableCaches must not be null, check init");
        }
        return cacheManagerForIndexableCaches;
    }

    @Produces
    @ApplicationScoped
    @SinkitCache(SinkitCacheName.infinispan_blacklist)
    public RemoteCache<String, BlacklistedRecord> getBlacklistCache() {
        if (cacheManager == null) {
            throw new IllegalArgumentException("Manager must not be null.");
        }
        log.log(Level.INFO, "getBlacklistCache called.");
        return cacheManager.getCache(SinkitCacheName.infinispan_blacklist.toString());
    }

    @Produces
    @ApplicationScoped
    @SinkitCache(SinkitCacheName.infinispan_whitelist)
    public RemoteCache<String, WhitelistedRecord> getWhitelistCache() {
        if (cacheManager == null) {
            throw new IllegalArgumentException("Manager must not be null.");
        }
        log.log(Level.INFO, "getWhitelistCache called.");
        return cacheManager.getCache(SinkitCacheName.infinispan_whitelist.toString());
    }

    @Produces
    @ApplicationScoped
    @SinkitCache(SinkitCacheName.infinispan_gsb)
    public RemoteCache<String, GSBRecord> getGsbCache() {
        if (cacheManager == null) {
            throw new IllegalArgumentException("Manager must not be null.");
        }
        log.log(Level.INFO, "getGsbCache called.");
        return cacheManager.getCache(SinkitCacheName.infinispan_gsb.toString());
    }

    @Produces
    @ApplicationScoped
    @SinkitCache(SinkitCacheName.custom_lists_local_cache)
    public BasicCache<String, List<CustomList>> getCustomListLocalCache() {
        if (localCacheManager == null) {
            throw new IllegalArgumentException("Manager must not be null.");
        }
        log.log(Level.INFO, "getCustomListLocalCache called.");
        return localCacheManager.getCache(SinkitCacheName.custom_lists_local_cache.toString());
    }

    @Produces
    @ApplicationScoped
    @SinkitCache(SinkitCacheName.rules_local_cache)
    public BasicCache<String, List<Rule>> getRuleLocalCache() {
        if (localCacheManager == null) {
            throw new IllegalArgumentException("Manager must not be null.");
        }
        log.log(Level.INFO, "getRuleLocalCache called.");
        return localCacheManager.getCache(SinkitCacheName.rules_local_cache.toString());
    }

    @Produces
    @ApplicationScoped
    @SinkitCache(SinkitCacheName.resolver_configuration)
    public RemoteCache<Integer, ResolverConfiguration> getResolverConfigurationCache() {
        if (cacheManagerForIndexableCaches == null) {
            throw new IllegalArgumentException("Manager must not be null.");
        }
        log.log(Level.INFO, "getResolverConfigurationCache called.");
        return cacheManagerForIndexableCaches.getCache(SinkitCacheName.resolver_configuration.name());
    }

    @Produces
    @ApplicationScoped
    @SinkitCache(SinkitCacheName.end_user_configuration)
    public RemoteCache<String, EndUserConfiguration> getEndUserConfigurationCache() {
        if (localCacheManager == null) {
            throw new IllegalArgumentException("Manager must not be null.");
        }
        log.log(Level.INFO, "getEndUserConfigurationCache called.");
        return cacheManagerForIndexableCaches.getCache(SinkitCacheName.end_user_configuration.name());
    }


    public void destroy(@Observes @Destroyed(ApplicationScoped.class) Object init) {
        if (localCacheManager != null) {
            localCacheManager.stop();
        }
        if (cacheManager != null) {
            cacheManager.stop();
        }
        if (cacheManagerForIndexableCaches != null) {
            cacheManagerForIndexableCaches.stop();
        }
    }

    private String readResource(String resourcePath) throws IOException {
        try (final InputStream is = getClass().getResourceAsStream(resourcePath)) {
            final Reader r = new InputStreamReader(is, "UTF-8");
            final StringWriter w = new StringWriter();
            char[] buf = new char[1024];
            int len;
            while ((len = r.read(buf)) != -1) {
                w.write(buf, 0, len);
            }
            is.close();
            return w.toString();
        }
    }
}

