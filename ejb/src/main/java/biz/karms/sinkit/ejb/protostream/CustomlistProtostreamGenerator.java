package biz.karms.sinkit.ejb.protostream;

import biz.karms.sinkit.ejb.cache.annotations.SinkitCache;
import biz.karms.sinkit.ejb.cache.annotations.SinkitCacheName;
import biz.karms.sinkit.ejb.cache.pojo.CustomList;
import biz.karms.sinkit.ejb.protostream.marshallers.CoreCacheMarshaller;
import biz.karms.sinkit.ejb.protostream.marshallers.SinkitCacheEntryMarshaller;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.infinispan.client.hotrod.Flag;
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
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
public class CustomlistProtostreamGenerator {

    @Inject
    private Logger log;

    @Inject
    @SinkitCache(SinkitCacheName.cache_manager_indexable)
    private RemoteCacheManager cacheManagerForIndexableCaches;

    @Resource
    private TimerService timerService;

    /**
     * e.g. every 5 to 10 minutes:
     * "* * *\/5-10 0" (without \)
     */
    public static final String[] SINKIT_CUSTOMLIST_PROTOSTREAM_GENERATOR_D_H_M_S =
            (System.getenv().containsKey("SINKIT_CUSTOMLIST_PROTOSTREAM_GENERATOR_D_H_M_S") &&
                    StringUtils.isNotEmpty(System.getenv("SINKIT_CUSTOMLIST_PROTOSTREAM_GENERATOR_D_H_M_S")) &&
                    System.getenv("SINKIT_CUSTOMLIST_PROTOSTREAM_GENERATOR_D_H_M_S").split(" ").length == 4) ?
                    System.getenv("SINKIT_CUSTOMLIST_PROTOSTREAM_GENERATOR_D_H_M_S").split(" ") : null;

    public static final String customListFilePath = System.getProperty("java.io.tmpdir") + "/customlist.bin";
    public static final String customListFilePathTmp = System.getProperty("java.io.tmpdir") + "/customlist.bin.tmp";
    public static final String customListFileMd5 = System.getProperty("java.io.tmpdir") + "/customlist.bin.md5";
    public static final String customListFileMd5Tmp = System.getProperty("java.io.tmpdir") + "/customlist.bin.md5.tmp";

    @PostConstruct
    private void initialize() {
        if (SINKIT_CUSTOMLIST_PROTOSTREAM_GENERATOR_D_H_M_S != null) {
            timerService.createCalendarTimer(new ScheduleExpression()
                            .dayOfWeek(SINKIT_CUSTOMLIST_PROTOSTREAM_GENERATOR_D_H_M_S[0])
                            .hour(SINKIT_CUSTOMLIST_PROTOSTREAM_GENERATOR_D_H_M_S[1])
                            .minute(SINKIT_CUSTOMLIST_PROTOSTREAM_GENERATOR_D_H_M_S[2])
                            .second(SINKIT_CUSTOMLIST_PROTOSTREAM_GENERATOR_D_H_M_S[3])
                    , new TimerConfig("CustomlistProtostreamGenerator", false));
        } else {
            log.info("CustomlistProtostreamGenerator timer not activated.");
        }
    }

    @PreDestroy
    public void stop() {
        log.info("Stop all existing CustomlistProtostreamGenerator timers.");
        for (Timer timer : timerService.getTimers()) {
            log.fine("Stop CustomlistProtostreamGenerator timer: " + timer.getInfo());
            timer.cancel();
        }
    }

    @Timeout
    public void scheduler(Timer timer) throws IOException, InterruptedException {
        log.info("CustomlistProtostreamGenerator: Info=" + timer.getInfo());
        long start = System.currentTimeMillis();
        final Map<Integer, Map<String, Boolean>> customerIdDomainData = new HashMap<>();
        final QueryFactory qf = Search.getQueryFactory(cacheManagerForIndexableCaches.getCache(SinkitCacheName.infinispan_custom_lists.toString()).withFlags(Flag.SKIP_CACHE_LOAD));
        final Query query = qf.from(CustomList.class)
                .having("whiteBlackLog").eq("B")
                .or()
                .having("whiteBlackLog").eq("W")
                .toBuilder().build();
        final List<CustomList> result = query.list();
        result.forEach(cl -> {
            if (StringUtils.isNotEmpty(cl.getFqdn())) {
                if (!customerIdDomainData.containsKey(cl.getCustomerId())) {
                    customerIdDomainData.put(cl.getCustomerId(), new HashMap<>());
                }
                if ("B".equals(cl.getWhiteBlackLog())) {
                    customerIdDomainData.get(cl.getCustomerId()).put(DigestUtils.md5Hex(cl.getFqdn()), Boolean.FALSE);
                } else {
                    customerIdDomainData.get(cl.getCustomerId()).put(DigestUtils.md5Hex(cl.getFqdn()), Boolean.TRUE);
                }
            }
        });
        /*
        // TODO: revisit .getBulk() with ISPN 9
        cacheManagerForIndexableCaches.getCache(SinkitCacheName.infinispan_custom_lists.toString()).withFlags(Flag.SKIP_CACHE_LOAD).getBulk().values().forEach(e -> {
            final CustomList cl = (CustomList) e;
            if (StringUtils.isNotEmpty(cl.getFqdn())) {
                if (!customerIdDomainData.containsKey(cl.getCustomerId())) {
                    customerIdDomainData.put(cl.getCustomerId(), new HashMap<>());
                }
                if ("B".equals(cl.getWhiteBlackLog())) {
                    customerIdDomainData.get(cl.getCustomerId()).put(DigestUtils.md5Hex(cl.getFqdn()), Boolean.FALSE);
                } else if ("W".equals(cl.getWhiteBlackLog())) {
                    customerIdDomainData.get(cl.getCustomerId()).put(DigestUtils.md5Hex(cl.getFqdn()), Boolean.TRUE);
                } else {
                    // We don't serialize L, i.e. "Log only"
                }
            }
        });
        */

        log.info("CustomlistProtostreamGenerator: Pulling customlist data took: " + (System.currentTimeMillis() - start) + " ms");
        start = System.currentTimeMillis();
        final SerializationContext ctx = ProtobufUtil.newSerializationContext(new Configuration.Builder().build());
        ctx.registerProtoFiles(FileDescriptorSource.fromResources(SINKIT_CACHE_PROTOBUF));
        ctx.registerMarshaller(new SinkitCacheEntryMarshaller());
        ctx.registerMarshaller(new CoreCacheMarshaller());

        customerIdDomainData.entrySet().forEach(r -> {
            final Path whiteListFilePathTmpP = Paths.get(customListFilePathTmp + r.getKey());
            final Path whiteListFilePathP = Paths.get(customListFilePath + r.getKey());
            try {
                Files.newByteChannel(whiteListFilePathTmpP, options, attr).write(ProtobufUtil.toByteBuffer(ctx, r.getValue()));
            } catch (IOException e) {
                log.severe("CustomlistProtostreamGenerator: failed protobuffer serialization for customer id " + r.getKey());
                e.printStackTrace();
            }
            final String[] customListFileMD5TmpCmd = {"/bin/sh", "-c", "/usr/bin/md5sum -b " + customListFilePathTmp + r.getKey() + " | cut -d ' ' -f1 > " + customListFileMd5Tmp + r.getKey()};
            try {
                // TODO Timeout?
                if (Runtime.getRuntime().exec(customListFileMD5TmpCmd).waitFor() != 0) {
                    log.severe("CustomlistProtostreamGenerator: md5sum of " + customListFilePathTmp + r.getKey() + " failed with command: " + ArrayUtils.toString(customListFileMD5TmpCmd));
                }
                // There is a race condition when we swap files while REST API is reading them...
                Files.move(whiteListFilePathTmpP, whiteListFilePathP, REPLACE_EXISTING);
                Files.move(Paths.get(customListFileMd5Tmp + r.getKey()), Paths.get(customListFileMd5 + r.getKey()), REPLACE_EXISTING);
            } catch (IOException | InterruptedException e) {
                log.severe("CustomlistProtostreamGenerator: failed protofile manipulation for customer id " + r.getKey());
                e.printStackTrace();
            }
        });

        log.info("CustomlistProtostreamGenerator: Serialization of custom lists for " + customerIdDomainData.size() + " customer ids took: " + (System.currentTimeMillis() - start) + " ms.");
    }
}
