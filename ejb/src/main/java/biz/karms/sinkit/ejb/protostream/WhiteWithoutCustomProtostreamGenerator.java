package biz.karms.sinkit.ejb.protostream;

import biz.karms.sinkit.ejb.cache.annotations.SinkitCache;
import biz.karms.sinkit.ejb.cache.annotations.SinkitCacheName;
import biz.karms.sinkit.ejb.cache.pojo.CustomList;
import biz.karms.sinkit.ejb.cache.pojo.WhitelistedRecord;
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static biz.karms.sinkit.ejb.protostream.CustomlistProtostreamGenerator.GENERATED_PROTOFILES_DIRECTORY;
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
public class WhiteWithoutCustomProtostreamGenerator {

    @Inject
    private Logger log;

    @Inject
    @SinkitCache(SinkitCacheName.cache_manager_indexable)
    private RemoteCacheManager cacheManagerForIndexableCaches;

    @Inject
    @SinkitCache(SinkitCacheName.infinispan_whitelist)
    private RemoteCache<String, WhitelistedRecord> whitelistCache;

    @Resource
    private TimerService timerService;

    /**
     * e.g. every 10 to 50 minutes:
     * "* * *\/10-50 0" (without \)
     */
    public static final String[] SINKIT_WHITE_WITHOUT_CUSTOM_PROTOSTREAM_GENERATOR_D_H_M_S =
            (System.getenv().containsKey("SINKIT_WHITE_WITHOUT_CUSTOM_PROTOSTREAM_GENERATOR_D_H_M_S") &&
                    StringUtils.isNotEmpty(System.getenv("SINKIT_WHITE_WITHOUT_CUSTOM_PROTOSTREAM_GENERATOR_D_H_M_S")) &&
                    System.getenv("SINKIT_WHITE_WITHOUT_CUSTOM_PROTOSTREAM_GENERATOR_D_H_M_S").split(" ").length == 4) ?
                    System.getenv("SINKIT_WHITE_WITHOUT_CUSTOM_PROTOSTREAM_GENERATOR_D_H_M_S").split(" ") : null;

    public static final String whiteWithoutCustomFilePath = GENERATED_PROTOFILES_DIRECTORY + "/whiteWithoutCustom.bin";
    public static final String whiteWithoutCustomFilePathTmp = GENERATED_PROTOFILES_DIRECTORY + "/whiteWithoutCustom.bin.tmp";
    public static final String whiteWithoutCustomFileMd5 = GENERATED_PROTOFILES_DIRECTORY + "/whiteWithoutCustom.bin.md5";
    public static final String whiteWithoutCustomFileMd5Tmp = GENERATED_PROTOFILES_DIRECTORY + "/whiteWithoutCustom.bin.md5.tmp";

    @PostConstruct
    private void initialize() {

        new File(GENERATED_PROTOFILES_DIRECTORY).mkdirs();

        if (SINKIT_WHITE_WITHOUT_CUSTOM_PROTOSTREAM_GENERATOR_D_H_M_S != null) {
            timerService.createCalendarTimer(new ScheduleExpression()
                            .dayOfWeek(SINKIT_WHITE_WITHOUT_CUSTOM_PROTOSTREAM_GENERATOR_D_H_M_S[0])
                            .hour(SINKIT_WHITE_WITHOUT_CUSTOM_PROTOSTREAM_GENERATOR_D_H_M_S[1])
                            .minute(SINKIT_WHITE_WITHOUT_CUSTOM_PROTOSTREAM_GENERATOR_D_H_M_S[2])
                            .second(SINKIT_WHITE_WITHOUT_CUSTOM_PROTOSTREAM_GENERATOR_D_H_M_S[3])
                    , new TimerConfig("WhiteWithoutCustomProtostreamGenerator", false));
        } else {
            log.info("WhiteWithoutCustomProtostreamGenerator timer not activated.");
        }
    }

    @PreDestroy
    public void stop() {
        log.info("Stop all existing WhiteWithoutCustomProtostreamGenerator timers.");
        for (Timer timer : timerService.getTimers()) {
            log.fine("Stop WhiteWithoutCustomProtostreamGenerator timer: " + timer.getInfo());
            timer.cancel();
        }
    }

    @Timeout
    public void scheduler(Timer timer) throws IOException, InterruptedException {
        log.info("WhiteWithoutCustom: Info=" + timer.getInfo());
        long start = System.currentTimeMillis();
        final Set<String> fqdnsOnCustomerBlackOrLog = new HashSet<>();
        final QueryFactory qf = Search.getQueryFactory(cacheManagerForIndexableCaches.getCache(SinkitCacheName.infinispan_custom_lists.toString()).withFlags(Flag.SKIP_CACHE_LOAD));
        final Query query = qf.from(CustomList.class)
                .having("whiteBlackLog").eq("B")
                .or()
                .having("whiteBlackLog").eq("L")
                .toBuilder().build();
        final List<CustomList> result = query.list();
        result.forEach(cl -> {
            if (StringUtils.isNotEmpty(cl.getFqdn())) {
                fqdnsOnCustomerBlackOrLog.add(DigestUtils.md5Hex(cl.getFqdn()));
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
                    customerIdDomainData.get(cl.getCustomerId()).put(DigestUtils.md5Hex(cl.getFqdn()), Action.BLACK);
                } else if ("W".equals(cl.getWhiteBlackLog())) {
                    customerIdDomainData.get(cl.getCustomerId()).put(DigestUtils.md5Hex(cl.getFqdn()), Action.WHITE);
                } else {
                    // We don't serialize L, i.e. "Log only"
                }
            }
        });
        */
        log.info("WhiteWithoutCustom: Pulling customLists data took: " + (System.currentTimeMillis() - start) + " ms, there are " + fqdnsOnCustomerBlackOrLog.size() + " fqdns that are Blocked or Logged by some users.");
        start = System.currentTimeMillis();

        // TODO: Well, this hurts...  We wil probably need to use retrieve(...) and operate in chunks.
        // https://github.com/infinispan/infinispan/pull/4975

        // The point is to take all white list records without those some users have set for Logging or Blocking
        final Map<String, Action> whitelistWithoutCustomLists = whitelistCache.withFlags(Flag.SKIP_CACHE_LOAD).keySet().stream().filter(x -> !fqdnsOnCustomerBlackOrLog.contains(x)).collect(Collectors.toMap(Function.identity(), s -> Action.WHITE));

        log.info("WhiteWithoutCustom: Pulling and processing whitelistWithoutCustomLists data took: " + (System.currentTimeMillis() - start) + " ms, there are " + whitelistWithoutCustomLists.size() + " records to be saved.");
        start = System.currentTimeMillis();
        final SerializationContext ctx = ProtobufUtil.newSerializationContext(new Configuration.Builder().build());
        ctx.registerProtoFiles(FileDescriptorSource.fromResources(SINKIT_CACHE_PROTOBUF));
        ctx.registerMarshaller(new SinkitCacheEntryMarshaller());
        ctx.registerMarshaller(new CoreCacheMarshaller());
        ctx.registerMarshaller(new ActionMarshaller());
        final Path whiteWithoutCustomFilePathTmpP = Paths.get(whiteWithoutCustomFilePathTmp);
        final Path whiteWithoutCustomFilePathP = Paths.get(whiteWithoutCustomFilePath);
        Files.newByteChannel(whiteWithoutCustomFilePathTmpP, options, attr).write(ProtobufUtil.toByteBuffer(ctx, whitelistWithoutCustomLists));
        log.info("WhiteWithoutCustom: Serialization to " + whiteWithoutCustomFilePathTmp + " took: " + (System.currentTimeMillis() - start) + " ms");
        start = System.currentTimeMillis();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(new File(whiteWithoutCustomFilePathTmp));
            Files.write(Paths.get(whiteWithoutCustomFileMd5Tmp), DigestUtils.md5Hex(fis).getBytes());
            // There is a race condition when we swap files while REST API is reading them...
            Files.move(whiteWithoutCustomFilePathTmpP, whiteWithoutCustomFilePathP, REPLACE_EXISTING);
            Files.move(Paths.get(whiteWithoutCustomFileMd5Tmp), Paths.get(whiteWithoutCustomFileMd5), REPLACE_EXISTING);
        } catch (IOException e) {
            log.severe("WhiteWithoutCustom: failed protofile manipulation");
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    log.severe("WhiteWithoutCustom: Failed to close MD5 file stream.");
                }
            }
        }
        log.info("WhiteWithoutCustom: MD5 sum and move took: " + (System.currentTimeMillis() - start) + " ms");
    }
}
