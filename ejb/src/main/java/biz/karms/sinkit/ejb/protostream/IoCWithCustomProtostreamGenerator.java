package biz.karms.sinkit.ejb.protostream;

import biz.karms.sinkit.ejb.cache.annotations.SinkitCache;
import biz.karms.sinkit.ejb.cache.annotations.SinkitCacheName;
import biz.karms.sinkit.ejb.cache.pojo.BlacklistedRecord;
import biz.karms.sinkit.ejb.cache.pojo.CustomList;
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
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class IoCWithCustomProtostreamGenerator {

    @Inject
    private Logger log;

    @Inject
    @SinkitCache(SinkitCacheName.cache_manager_indexable)
    private RemoteCacheManager cacheManagerForIndexableCaches;

    @Inject
    @SinkitCache(SinkitCacheName.infinispan_blacklist)
    private RemoteCache<String, BlacklistedRecord> blacklistCache;

    @Resource
    private TimerService timerService;

    /**
     * e.g. every 1 hour (without /)
     * "* *\/1 0 0"
     */
    public static final String[] SINKIT_ALL_IOC_PROTOSTREAM_GENERATOR_D_H_M_S =
            (System.getenv().containsKey("SINKIT_ALL_IOC_PROTOSTREAM_GENERATOR_D_H_M_S") &&
                    StringUtils.isNotEmpty(System.getenv("SINKIT_ALL_IOC_PROTOSTREAM_GENERATOR_D_H_M_S")) &&
                    System.getenv("SINKIT_ALL_IOC_PROTOSTREAM_GENERATOR_D_H_M_S").split(" ").length == 4) ?
                    System.getenv("SINKIT_ALL_IOC_PROTOSTREAM_GENERATOR_D_H_M_S").split(" ") : null;
    private static final String ALL_IOC_TAG = "IoC - All Protostream Generator";

    /**
     * e.g. every 2 to 5 minute:
     * "* * *\/2-5 0" (without \)
     */
    public static final String[] SINKIT_ALL_CUSTOM_PROTOSTREAM_GENERATOR_D_H_M_S =
            (System.getenv().containsKey("SINKIT_ALL_CUSTOM_PROTOSTREAM_GENERATOR_D_H_M_S") &&
                    StringUtils.isNotEmpty(System.getenv("SINKIT_ALL_CUSTOM_PROTOSTREAM_GENERATOR_D_H_M_S")) &&
                    System.getenv("SINKIT_ALL_CUSTOM_PROTOSTREAM_GENERATOR_D_H_M_S").split(" ").length == 4) ?
                    System.getenv("SINKIT_ALL_CUSTOM_PROTOSTREAM_GENERATOR_D_H_M_S").split(" ") : null;
    private static final String ALL_CUSTOM_TAG = "Custom - All Protostream Generator";


    public static final String iocWithCustomFilePath = GENERATED_PROTOFILES_DIRECTORY + "/iocWithCustom.bin";
    public static final String iocWithCustomFilePathTmp = GENERATED_PROTOFILES_DIRECTORY + "/iocWithCustom.bin.tmp";
    public static final String iocWithCustomFileMd5 = GENERATED_PROTOFILES_DIRECTORY + "/iocWithCustom.bin.md5";
    public static final String iocWithCustomFileMd5Tmp = GENERATED_PROTOFILES_DIRECTORY + "/iocWithCustom.bin.md5.tmp";

    @PostConstruct
    private void initialize() {
        log.info("IoCWithCustomProtostreamGenerator initialize...");

        // Don't care about the result
        new File(GENERATED_PROTOFILES_DIRECTORY).mkdirs();

        if (SINKIT_ALL_IOC_PROTOSTREAM_GENERATOR_D_H_M_S != null) {
            timerService.createCalendarTimer(new ScheduleExpression()
                            .dayOfWeek(SINKIT_ALL_IOC_PROTOSTREAM_GENERATOR_D_H_M_S[0])
                            .hour(SINKIT_ALL_IOC_PROTOSTREAM_GENERATOR_D_H_M_S[1])
                            .minute(SINKIT_ALL_IOC_PROTOSTREAM_GENERATOR_D_H_M_S[2])
                            .second(SINKIT_ALL_IOC_PROTOSTREAM_GENERATOR_D_H_M_S[3])
                    , new TimerConfig(ALL_IOC_TAG, false));
            log.info("IoCWithCustomProtostreamGenerator: All IoC Protostream Generator");
        }
        if (SINKIT_ALL_CUSTOM_PROTOSTREAM_GENERATOR_D_H_M_S != null) {
            timerService.createCalendarTimer(new ScheduleExpression()
                            .dayOfWeek(SINKIT_ALL_CUSTOM_PROTOSTREAM_GENERATOR_D_H_M_S[0])
                            .hour(SINKIT_ALL_CUSTOM_PROTOSTREAM_GENERATOR_D_H_M_S[1])
                            .minute(SINKIT_ALL_CUSTOM_PROTOSTREAM_GENERATOR_D_H_M_S[2])
                            .second(SINKIT_ALL_CUSTOM_PROTOSTREAM_GENERATOR_D_H_M_S[3])
                    , new TimerConfig(ALL_CUSTOM_TAG, false));
            log.info("IoCWithCustomProtostreamGenerator: All Custom Protostream Generator");
        }
    }

    @PreDestroy
    public void stop() {
        log.info("Stop all existing IoCWithCustomProtostreamGenerator timers.");
        for (Timer timer : timerService.getTimers()) {
            log.fine("Stop IoCWithCustomProtostreamGenerator timer: " + timer.getInfo());
            timer.cancel();
        }
    }

    @Timeout
    public void scheduler(Timer timer) throws IOException, InterruptedException {
        log.info("IoCWithCustom: Info=" + timer.getInfo());

        /*
        Expensive and should be less frequent, generates the whole file with all IoC from all feeds
        somebody has at last on audit level plus all custom lists domains.
         */
        if (timer.getInfo().toString().contains(ALL_IOC_TAG)) {

            long start = System.currentTimeMillis();
            final Map<String, Action> iocWithCustom = new HashMap<>();
            final QueryFactory qf = Search.getQueryFactory(cacheManagerForIndexableCaches.getCache(SinkitCacheName.infinispan_custom_lists.toString()).withFlags(Flag.SKIP_CACHE_LOAD));
            final Query query = qf.from(CustomList.class).build();
            final List<CustomList> result = query.list();
            result.forEach(cl -> {
                if (StringUtils.isNotEmpty(cl.getFqdn())) {
                    iocWithCustom.put(DigestUtils.md5Hex(cl.getFqdn()), Action.CHECK);
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
            log.info("IoCWithCustom: Pulling customLists data took: " + (System.currentTimeMillis() - start) + " ms, there are " + iocWithCustom.size() + " fqdns that are Blocked or Logged by some users.");
            start = System.currentTimeMillis();

            // TODO: Well, this hurts...  We wil probably need to use retrieve(...) and operate in chunks.
            // https://github.com/infinispan/infinispan/pull/4975
            // final Map<String, Action> iocWithCustomLists = blacklistCache.withFlags(Flag.SKIP_CACHE_LOAD).keySet().stream().filter(x -> !fqdnsOnCustomerBlackOrLog.contains(x)).collect(Collectors.toMap(Function.identity(), s -> Action.WHITE));

            iocWithCustom.putAll(blacklistCache.withFlags(Flag.SKIP_CACHE_LOAD).keySet().stream()
                    .filter(key -> !blacklistCache.withFlags(Flag.SKIP_CACHE_LOAD).get(key).isPresentOnWhiteList())
                    .collect(Collectors.toMap(Function.identity(), s -> Action.CHECK)));

            log.info("IoCWithCustom: Pulling and processing iocWithCustomLists data took: " + (System.currentTimeMillis() - start) + " ms, there are " + iocWithCustom.size() + " records to be saved.");
            start = System.currentTimeMillis();
            final SerializationContext ctx = ProtobufUtil.newSerializationContext(new Configuration.Builder().build());
            ctx.registerProtoFiles(FileDescriptorSource.fromResources(SINKIT_CACHE_PROTOBUF));
            ctx.registerMarshaller(new SinkitCacheEntryMarshaller());
            ctx.registerMarshaller(new CoreCacheMarshaller());
            ctx.registerMarshaller(new ActionMarshaller());
            final Path iocWithCustomFilePathTmpP = Paths.get(iocWithCustomFilePathTmp);
            final Path iocWithCustomFilePathP = Paths.get(iocWithCustomFilePath);
            Files.newByteChannel(iocWithCustomFilePathTmpP, options, attr).write(ProtobufUtil.toByteBuffer(ctx, iocWithCustom));
            log.info("IoCWithCustom: Serialization to " + iocWithCustomFilePathTmp + " took: " + (System.currentTimeMillis() - start) + " ms.");
            start = System.currentTimeMillis();
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(new File(iocWithCustomFilePathTmp));
                Files.write(Paths.get(iocWithCustomFileMd5Tmp), DigestUtils.md5Hex(fis).getBytes());
                // There is a race condition when we swap files while REST API is reading them...
                Files.move(iocWithCustomFilePathTmpP, iocWithCustomFilePathP, REPLACE_EXISTING);
                Files.move(Paths.get(iocWithCustomFileMd5Tmp), Paths.get(iocWithCustomFileMd5), REPLACE_EXISTING);
            } catch (IOException e) {
                log.severe("IoCWithCustom: failed protofile manipulation.");
                e.printStackTrace();
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        log.severe("IoCWithCustom: Failed to close MD5 file stream.");
                    }
                }
            }
            log.info("IoCWithCustom: MD5 sum and move took: " + (System.currentTimeMillis() - start) + " ms.");

        /*
        Inexpensive, loads the generated file if it exists and updates it with just custom lists data.
        if the file doesn't exist at all, it creates it and fills it with custom lists data. There are no
        IoCs present until the ALL_IOC_TAG logic runs as scheduled by SINKIT_ALL_IOC_PROTOSTREAM_GENERATOR_D_H_M_S.

        The drawback is that records don't get removed until ALL_IOC_TAG phase regenerates the file.
        */
        } else if (timer.getInfo().toString().contains(ALL_CUSTOM_TAG)) {

            final File iocWithCustomBinary = new File(iocWithCustomFilePath);
            final SerializationContext ctx = ProtobufUtil.newSerializationContext(new Configuration.Builder().build());
            ctx.registerProtoFiles(FileDescriptorSource.fromResources(SINKIT_CACHE_PROTOBUF));
            ctx.registerMarshaller(new SinkitCacheEntryMarshaller());
            ctx.registerMarshaller(new CoreCacheMarshaller());
            ctx.registerMarshaller(new ActionMarshaller());

            final Path iocWithCustomFilePathTmpP = Paths.get(iocWithCustomFilePathTmp);
            final Path iocWithCustomFilePathP = Paths.get(iocWithCustomFilePath);

            final Map<String, Action> iocWithCustom;

            long start = System.currentTimeMillis();

            if (iocWithCustomBinary.exists()) {
                InputStream is = null;
                try {
                    is = new FileInputStream(iocWithCustomBinary);
                } catch (IOException e) {
                    log.severe(iocWithCustomFilePath + " not found. This is unexpected, skipping IoCWithCustom altogether.");
                }

                // Deserialize from file to memory
                iocWithCustom = ProtobufUtil.readFrom(ctx, is, HashMap.class);

                log.info("IoCWithCustom: Deserialization of " + iocWithCustom.size() + " records took " + (System.currentTimeMillis() - start) + " ms.");
            } else {
                iocWithCustom = new HashMap<>();
            }

            start = System.currentTimeMillis();

            // Load custom lists from Infinispan and update what's in memory
            final QueryFactory qf = Search.getQueryFactory(cacheManagerForIndexableCaches.getCache(SinkitCacheName.infinispan_custom_lists.toString()).withFlags(Flag.SKIP_CACHE_LOAD));
            final Query query = qf.from(CustomList.class).build();
            final List<CustomList> result = query.list();
            result.forEach(cl -> {
                if (StringUtils.isNotEmpty(cl.getFqdn())) {
                    // We add, we don't remove
                    iocWithCustom.putIfAbsent(DigestUtils.md5Hex(cl.getFqdn()), Action.CHECK);
                }
            });

            // Serialize from memory to file
            Files.newByteChannel(iocWithCustomFilePathTmpP, options, attr).write(ProtobufUtil.toByteBuffer(ctx, iocWithCustom));

            FileInputStream fis = null;
            try {
                fis = new FileInputStream(new File(iocWithCustomFilePathTmp));
                Files.write(Paths.get(iocWithCustomFileMd5Tmp), DigestUtils.md5Hex(fis).getBytes());
                // There is a race condition when we swap files while REST API is reading them...
                Files.move(iocWithCustomFilePathTmpP, iocWithCustomFilePathP, REPLACE_EXISTING);
                Files.move(Paths.get(iocWithCustomFileMd5Tmp), Paths.get(iocWithCustomFileMd5), REPLACE_EXISTING);
                log.info("IoCWithCustom: Update from cache with " + result.size() + " custom list records and serialization of all " + iocWithCustom.size() + " records took " + (System.currentTimeMillis() - start) + " ms.");
            } catch (IOException e) {
                log.severe("IoCWithCustom: failed protofile manipulation.");
                e.printStackTrace();
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        log.severe("IoCWithCustom: Failed to close MD5 file stream.");
                    }
                }
            }
        } else {
            log.severe("Unknown timer. Either \"" + ALL_IOC_TAG + "\" or \"" + ALL_CUSTOM_TAG + "\" expected. Skipping IoCWithCustom altogether.");
        }
    }
}
