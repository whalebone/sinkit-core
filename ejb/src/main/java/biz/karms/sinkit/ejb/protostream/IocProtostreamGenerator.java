package biz.karms.sinkit.ejb.protostream;

import biz.karms.sinkit.ejb.cache.annotations.SinkitCache;
import biz.karms.sinkit.ejb.cache.annotations.SinkitCacheName;
import biz.karms.sinkit.ejb.cache.pojo.BlacklistedRecord;
import biz.karms.sinkit.ejb.cache.pojo.Rule;
import biz.karms.sinkit.ejb.protostream.marshallers.ActionMarshaller;
import biz.karms.sinkit.ejb.protostream.marshallers.CoreCacheMarshaller;
import biz.karms.sinkit.ejb.protostream.marshallers.SinkitCacheEntryMarshaller;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.Search;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.ScheduleExpression;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static biz.karms.sinkit.ejb.protostream.WhitelistProtostreamGenerator.SINKIT_CACHE_PROTOBUF;
import static biz.karms.sinkit.ejb.protostream.WhitelistProtostreamGenerator.attr;
import static biz.karms.sinkit.ejb.protostream.WhitelistProtostreamGenerator.options;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * @author Michal Karm Babacek
 */
@Singleton
@LocalBean
@Startup
public class IocProtostreamGenerator {

    @Inject
    private Logger log;

    @Inject
    @SinkitCache(SinkitCacheName.infinispan_blacklist)
    private RemoteCache<String, BlacklistedRecord> blacklistCache;

    @Inject
    @SinkitCache(SinkitCacheName.cache_manager_indexable)
    private RemoteCacheManager cacheManagerForIndexableCaches;

    @Resource
    private TimerService timerService;

    /**
     * e.g. every 4 hours (without /)
     * "* *\/4 0 0"
     */
    public static final String[] SINKIT_IOC_PROTOSTREAM_GENERATOR_D_H_M_S =
            (System.getenv().containsKey("SINKIT_IOC_PROTOSTREAM_GENERATOR_D_H_M_S") &&
                    StringUtils.isNotEmpty(System.getenv("SINKIT_IOC_PROTOSTREAM_GENERATOR_D_H_M_S")) &&
                    System.getenv("SINKIT_IOC_PROTOSTREAM_GENERATOR_D_H_M_S").split(" ").length == 4) ?
                    System.getenv("SINKIT_IOC_PROTOSTREAM_GENERATOR_D_H_M_S").split(" ") : null;

    public static final String iocListFilePath = System.getProperty("java.io.tmpdir") + "/ioclist.bin";
    public static final String iocListFilePathTmp = System.getProperty("java.io.tmpdir") + "/ioclist.bin.tmp";
    public static final String iocListFileMd5 = System.getProperty("java.io.tmpdir") + "/ioclist.bin.md5";
    public static final String iocListFileMd5Tmp = System.getProperty("java.io.tmpdir") + "/ioclist.bin.md5.tmp";


    // TODO: I didn't want to read the file again in JVM to compute MD5 sum, but this probably just adds too much hair.
    public static final String[] iocListFileMD5TmpCmd = {
            "/bin/sh",
            "-c",
            "/usr/bin/md5sum -b " + iocListFilePathTmp + " | cut -d ' ' -f1 > " + iocListFileMd5Tmp
    };

    @PostConstruct
    private void initialize() {
        if (SINKIT_IOC_PROTOSTREAM_GENERATOR_D_H_M_S != null) {
            timerService.createCalendarTimer(new ScheduleExpression()
                            .dayOfWeek(SINKIT_IOC_PROTOSTREAM_GENERATOR_D_H_M_S[0])
                            .hour(SINKIT_IOC_PROTOSTREAM_GENERATOR_D_H_M_S[1])
                            .minute(SINKIT_IOC_PROTOSTREAM_GENERATOR_D_H_M_S[2])
                            .second(SINKIT_IOC_PROTOSTREAM_GENERATOR_D_H_M_S[3])
                    , new TimerConfig("IOCListProtostreamGenerator", false));
        } else {
            log.info("IOCListProtostreamGenerator timer not activated.");
        }
    }

    @PreDestroy
    public void stop() {
        log.info("Stop all existing IOCListProtostreamGenerator timers.");
        for (Timer timer : timerService.getTimers()) {
            log.fine("Stop IOCListProtostreamGenerator timer: " + timer.getInfo());
            timer.cancel();
        }
    }

    @Timeout
    public void scheduler(Timer timer) throws IOException, InterruptedException, FileNotFoundException {
        log.info("IOCListProtostreamGenerator: Info=" + timer.getInfo());
        long start = System.currentTimeMillis();
        // TODO: Well, this hurts...  We wil probably need to use retrieve(...) and operate in chunks.
        // TODO: DistributedExecutorService, DistributedTaskBuilder and DistributedTask API ?
        // https://github.com/infinispan/infinispan/pull/4975
        // final Map<String, Action> ioclist = ioclistCache.withFlags(Flag.SKIP_CACHE_LOAD).keySet().stream().collect(Collectors.toMap(Function.identity(), s -> Action.BLACK));
        final RemoteCache<String, Rule> rulesCache = cacheManagerForIndexableCaches.getCache(SinkitCacheName.infinispan_rules.toString());
        final QueryFactory qf = Search.getQueryFactory(rulesCache);
        final Query querySink = qf.from(Rule.class).having("sources.mode").eq("S").toBuilder().build();
        final List<Rule> resultsSink = querySink.list();
        final Query queryLog = qf.from(Rule.class).having("sources.mode").eq("L").toBuilder().build();
        final List<Rule> resultsLog = queryLog.list();
        // These are not numerous < hundreds
        final Map<Integer, Set<String>> custIdFeedUidsSink = new HashMap<>();
        resultsSink.forEach(r -> {
            if (custIdFeedUidsSink.containsKey(r.getCustomerId())) {
                custIdFeedUidsSink.get(r.getCustomerId()).addAll(r.getSources().keySet());
            } else {
                custIdFeedUidsSink.put(r.getCustomerId(), new HashSet<>(r.getSources().keySet()));
            }
        });
        log.info("IOCListProtostreamGenerator: Will process IoCs for " + custIdFeedUidsSink.size() + " customer Sink ids.");
        final Map<Integer, Set<String>> custIdFeedUidsLog = new HashMap<>();
        resultsLog.forEach(r -> {
            if (custIdFeedUidsLog.containsKey(r.getCustomerId())) {
                custIdFeedUidsLog.get(r.getCustomerId()).addAll(r.getSources().keySet());
            } else {
                custIdFeedUidsLog.put(r.getCustomerId(), new HashSet<>(r.getSources().keySet()));
            }
        });
        log.info("IOCListProtostreamGenerator: Will process IoCs for " + custIdFeedUidsLog.size() + " customer Log ids.");

        // final List<String> feeduids = results.stream().map(Rule::getSources).collect(Collectors.toList()).stream().map(Map::keySet).flatMap(Set::stream).collect(Collectors.toList());
        final Map<Integer, Map<String, Action>> preparedHashes = new HashMap<>();

        // TODO: bulk? Super slow and inefficient?
        blacklistCache.withFlags(Flag.SKIP_CACHE_LOAD).keySet().forEach(key -> blacklistCache.withFlags(Flag.SKIP_CACHE_LOAD).get(key).getSources().keySet().forEach(feeduid -> {
            custIdFeedUidsLog.entrySet().stream().filter(e -> e.getValue().contains(feeduid)).forEach(found -> {
                if (preparedHashes.containsKey(found.getKey())) {
                    preparedHashes.get(found.getKey()).put(key, Action.LOG);
                } else {
                    final Map<String, Action> newHashes = new HashMap<>();
                    newHashes.put(key, Action.LOG);
                    preparedHashes.put(found.getKey(), newHashes);
                }
            });
            custIdFeedUidsSink.entrySet().stream().filter(e -> e.getValue().contains(feeduid)).forEach(found -> {
                if (preparedHashes.containsKey(found.getKey())) {
                    preparedHashes.get(found.getKey()).put(key, Action.BLACK);
                } else {
                    final Map<String, Action> newHashes = new HashMap<>();
                    newHashes.put(key, Action.BLACK);
                    preparedHashes.put(found.getKey(), newHashes);
                }
            });
        }));

        // TODO: 8000 is a magic number. We should profile that.
        /* Keeps getting: https://gist.github.com/Karm/2bc7bee4f71027ebea993dbf38efef7b
        final CloseableIterator<Map.Entry<Object, Object>> unfilteredIterator = blacklistCache.retrieveEntries(null, null, 8000);
        unfilteredIterator.forEachRemaining(ioc -> {
            // There are usually dozens sources
            ((BlacklistedRecord) ioc.getValue()).getSources().keySet().forEach(feeduid -> {
                final Optional<Map.Entry<Integer, Set<String>>> found = custIdFeedUids.entrySet().stream().filter(e -> e.getValue().contains(feeduid)).findFirst();
                if (found != null && found.isPresent()) {
                    if (preparedHashes.containsKey(found.get().getKey())) {
                        preparedHashes.get(found.get().getKey()).put((String) ioc.getKey(), Action.BLACK);
                    } else {
                        final Map<String, Action> newHashes = new HashMap<>();
                        newHashes.put((String) ioc.getKey(), Action.BLACK);
                        preparedHashes.put(found.get().getKey(), newHashes);
                    }
                }
            });
        });
        */

        /*
        final List<String> preparedHashes = blacklistCache.getBulk().entrySet().parallelStream()
                .filter(e -> {
                    for (String feeduid : e.getValue().getSources().keySet()) {
                        if (feeduids.contains(feeduid)) {
                            return true;
                        } // else {
                            //System.out.println("feeduid: "+feeduid);
                        //}
                    }
                    return false;
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
       */

        log.info("IOCListProtostreamGenerator: Pulling ioclist data took: " + (System.currentTimeMillis() - start) + " ms");
        start = System.currentTimeMillis();
        final SerializationContext ctx = ProtobufUtil.newSerializationContext(new Configuration.Builder().build());
        ctx.registerProtoFiles(FileDescriptorSource.fromResources(SINKIT_CACHE_PROTOBUF));
        ctx.registerMarshaller(new SinkitCacheEntryMarshaller());
        ctx.registerMarshaller(new CoreCacheMarshaller());
        ctx.registerMarshaller(new ActionMarshaller());

        final AtomicInteger workCounter = new AtomicInteger(1);
        preparedHashes.entrySet().forEach(r -> {
            log.info("IOCListProtostreamGenerator: processing Ioc data file for serialization " + workCounter.getAndIncrement() + "/" + preparedHashes.size() + ", customer id: " + r.getKey());
            final Path iocListFilePathTmpP = Paths.get(iocListFilePathTmp + r.getKey());
            final Path iocListFilePathP = Paths.get(iocListFilePath + r.getKey());
            try {
                Files.newByteChannel(iocListFilePathTmpP, options, attr).write(ProtobufUtil.toByteBuffer(ctx, r.getValue()));
            } catch (IOException e) {
                log.severe("IOCListProtostreamGenerator: failed protobuffer serialization for customer id " + r.getKey());
                e.printStackTrace();
            }
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(new File(iocListFilePathTmp + r.getKey()));
                Files.write(Paths.get(iocListFileMd5Tmp + r.getKey()), DigestUtils.md5Hex(fis).getBytes());
                // There is a race condition when we swap files while REST API is reading them...
                Files.move(iocListFilePathTmpP, iocListFilePathP, REPLACE_EXISTING);
                Files.move(Paths.get(iocListFileMd5Tmp + r.getKey()), Paths.get(iocListFileMd5 + r.getKey()), REPLACE_EXISTING);
            } catch (IOException e) {
                log.severe("IOCListProtostreamGenerator: failed protofile manipulation for customer id " + r.getKey());
                e.printStackTrace();
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        log.severe("IOCListProtostreamGenerator: Failed to close MD5 file stream.");
                    }
                }
            }
        });

        log.info("IOCListProtostreamGenerator: Serialization of ioc lists took: " + (System.currentTimeMillis() - start) + " ms.");

    }
}
