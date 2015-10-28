package biz.karms.sinkit.ejb;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.Index;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.util.concurrent.IsolationLevel;

import javax.annotation.ManagedBean;
import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Michal Karm Babacek
 */
@ApplicationScoped
@ManagedBean
public class MyCacheManagerProvider implements Serializable {

    private static final long serialVersionUID = 452168391425257496L;

    private static final long ENTRY_LIFESPAN = 4 * 24 * 60 * 60 * 1000; //ms
    private static final long ENTRY_LIFESPAN_NEVER = -1;
    private static final long MAX_ENTRIES_IOC = 10000000;
    private static final long MAX_ENTRIES_RULES = 5000;

    @Inject
    private Logger log;

    @Resource(lookup="java:jboss/infinispan/container/sinkitcontainer")
    private EmbeddedCacheManager manager;
    //private Cache<?, ?> cache;

    //private DefaultCacheManager manager;

    public void init(@Observes @Initialized(ApplicationScoped.class) Object init) {
        log.info("\n\n Constructing caches...\n\n");
        Configuration loc = new ConfigurationBuilder().jmxStatistics().enable() // Enable JMX statistics
                //.clustering().cacheMode(CacheMode.DIST_ASYNC)
                .clustering().cacheMode(CacheMode.REPL_ASYNC)
                .stateTransfer().awaitInitialTransfer(true)
                .timeout(5, TimeUnit.MINUTES)
                        //.chunkSize(512)
                .async()//.useReplQueue(true).replQueueInterval(30, TimeUnit.SECONDS)
                        //.hash()//.numOwners(2)
                .locking().lockAcquisitionTimeout(1, TimeUnit.MINUTES)//.writeSkewCheck(false).useLockStriping(false)
                .concurrencyLevel(3000)
                .isolationLevel(IsolationLevel.READ_COMMITTED)
                .expiration()
                .lifespan(ENTRY_LIFESPAN_NEVER) // Set expiration - cache entries expire after some time (given by
                        // the lifespan parameter) and are removed from the cache (cluster-wide).
                .disableReaper()
                .indexing().index(Index.ALL)
                .addProperty("hibernate.search.default.indexwriter.merge_factor", "30")
                .addProperty("hibernate.search.default.indexmanager", "org.infinispan.query.indexmanager.InfinispanIndexManager")
                .addProperty("hibernate.search.default.worker.execution", "async")
                .eviction().strategy(EvictionStrategy.NONE)
                        //.maxEntries(MAX_ENTRIES_IOC)
                        // .transaction().lockingMode(LockingMode.OPTIMISTIC).transactionManagerLookup(tml)
                        //.transaction().transactionMode(TransactionMode.TRANSACTIONAL).lockingMode(LockingMode.OPTIMISTIC)
                        //.transaction().lockingMode(LockingMode.OPTIMISTIC).transactionMode(TransactionMode.NON_TRANSACTIONAL).completedTxTimeout(300000)
                .transaction().transactionMode(TransactionMode.NON_TRANSACTIONAL)
                        //Very evil, but fast...
                .unsafe().unreliableReturnValues(true)
                        // TODO: Really? Autocommit? -- Yes, autocommit is true by default.
                        //.transactionManagerLookup(new GenericTransactionManagerLookup()).autoCommit(true)
                .persistence().addSingleFileStore()
                /*.persistence().addStore(JdbcStringBasedStoreConfigurationBuilder.class)
                        //.persistence().addStore(JdbcBinaryStoreConfigurationBuilder.class)
                .fetchPersistentState(true)
                .ignoreModifications(false)
                .purgeOnStartup(false)
                .table()
                .dropOnExit(false)
                .createOnStart(true)
                .tableNamePrefix("ISPN_STRING_TABLE")
                .idColumnName("ID_COLUMN").idColumnType("VARCHAR(255)")
                .dataColumnName("DATA_COLUMN").dataColumnType("BYTEA")
                .timestampColumnName("TIMESTAMP_COLUMN").timestampColumnType("BIGINT")
                .connectionPool()
                .connectionUrl("jdbc:postgresql://" + System.getenv("SINKIT_POSTGRESQL_DB_HOST") + ":" + System.getenv("SINKIT_POSTGRESQL_DB_PORT") + "/" + System.getenv("SINKIT_POSTGRESQL_DB_NAME"))
                .driverClass("org.postgresql.Driver")
                .password(System.getenv("SINKIT_POSTGRESQL_PASS"))
                .username(System.getenv("SINKIT_POSTGRESQL_USER"))
                .async()
                .enabled(true)
                .threadPoolSize(15)*/
                .build();

        //this.manager.defineConfiguration("mycache", loc);
        //this.cache = this.manager.getCache("mycache");
        //this.cache.start();

        manager.defineConfiguration("BLACKLIST_CACHE", loc);
        manager.defineConfiguration("RULES_CACHE", loc);
        manager.defineConfiguration("CUSTOM_LISTS_CACHE", loc);
        manager.getCache("BLACKLIST_CACHE").start();
        manager.getCache("RULES_CACHE").start();
        manager.getCache("CUSTOM_LISTS_CACHE").start();
        log.log(Level.INFO, "Caches defined.");
    }

    public <K, V> Cache<K, V> getCache(String cacheName) {
        if (manager == null || cacheName == null) {
            throw new IllegalArgumentException("Both manager and cacheName must not be null.");
        }
        return manager.getCache(cacheName);
    }

    public void destroy(@Observes @Destroyed(ApplicationScoped.class) Object init) {
        if (manager != null) {
            //TODO
            manager.getCache("BLACKLIST_CACHE").stop();
            manager.getCache("RULES_CACHE").stop();
            manager.getCache("CUSTOM_LISTS_CACHE").stop();
            manager.undefineConfiguration("BLACKLIST_CACHE");
            manager.undefineConfiguration("RULES_CACHE");
            manager.undefineConfiguration("CUSTOM_LISTS_CACHE");
            manager.stop();
            manager = null;
        }
    }
}

