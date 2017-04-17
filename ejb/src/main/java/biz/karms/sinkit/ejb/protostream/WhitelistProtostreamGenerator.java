package biz.karms.sinkit.ejb.protostream;

import biz.karms.sinkit.ejb.cache.annotations.SinkitCache;
import biz.karms.sinkit.ejb.cache.annotations.SinkitCacheName;
import biz.karms.sinkit.ejb.cache.pojo.WhitelistedRecord;
import biz.karms.sinkit.ejb.protostream.marshallers.ActionMarshaller;
import biz.karms.sinkit.ejb.protostream.marshallers.CoreCacheMarshaller;
import biz.karms.sinkit.ejb.protostream.marshallers.SinkitCacheEntryMarshaller;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.config.Configuration;

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
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

/**
 * @author Michal Karm Babacek
 */
@Singleton
@LocalBean
@Startup
public class WhitelistProtostreamGenerator {

    @Inject
    private Logger log;

    @Inject
    @SinkitCache(SinkitCacheName.infinispan_whitelist)
    private RemoteCache<String, WhitelistedRecord> whitelistCache;

    @Resource
    private TimerService timerService;

    /**
     * e.g. every day of the week, some time between 1 and 6 AM
     * "* 1-6 0 0"
     */
    public static final String[] SINKIT_WHITELIST_PROTOSTREAM_GENERATOR_D_H_M_S =
            (System.getenv().containsKey("SINKIT_WHITELIST_PROTOSTREAM_GENERATOR_D_H_M_S") &&
                    StringUtils.isNotEmpty(System.getenv("SINKIT_WHITELIST_PROTOSTREAM_GENERATOR_D_H_M_S")) &&
                    System.getenv("SINKIT_WHITELIST_PROTOSTREAM_GENERATOR_D_H_M_S").split(" ").length == 4) ?
                    System.getenv("SINKIT_WHITELIST_PROTOSTREAM_GENERATOR_D_H_M_S").split(" ") : null;
    public static final Set<OpenOption> options = Stream.of(APPEND, CREATE).collect(Collectors.toSet());
    public static final FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-r-----"));
    public static final String SINKIT_CACHE_PROTOBUF = "/sinkitprotobuf/sinkit-cache.proto";

    public static final String whiteListFilePath = System.getProperty("java.io.tmpdir") + "/whitelist.bin";
    public static final String whiteListFilePathTmp = System.getProperty("java.io.tmpdir") + "/whitelist.bin.tmp";
    public static final String whiteListFileMd5 = System.getProperty("java.io.tmpdir") + "/whitelist.bin.md5";
    public static final String whiteListFileMd5Tmp = System.getProperty("java.io.tmpdir") + "/whitelist.bin.md5.tmp";

    @PostConstruct
    private void initialize() {
        if (SINKIT_WHITELIST_PROTOSTREAM_GENERATOR_D_H_M_S != null) {
            timerService.createCalendarTimer(new ScheduleExpression()
                            .dayOfWeek(SINKIT_WHITELIST_PROTOSTREAM_GENERATOR_D_H_M_S[0])
                            .hour(SINKIT_WHITELIST_PROTOSTREAM_GENERATOR_D_H_M_S[1])
                            .minute(SINKIT_WHITELIST_PROTOSTREAM_GENERATOR_D_H_M_S[2])
                            .second(SINKIT_WHITELIST_PROTOSTREAM_GENERATOR_D_H_M_S[3])
                    , new TimerConfig("WhitelistProtostreamGenerator", false));
        } else {
            log.info("WhitelistProtostreamGenerator timer not activated.");
        }
    }

    @PreDestroy
    public void stop() {
        log.info("Stop all existing WhitelistProtostreamGenerator timers.");
        for (Timer timer : timerService.getTimers()) {
            log.fine("Stop WhitelistProtostreamGenerator timer: " + timer.getInfo());
            timer.cancel();
        }
    }

    @Timeout
    public void scheduler(Timer timer) throws IOException, InterruptedException {
        log.info("WhitelistProtostreamGenerator: Info=" + timer.getInfo());
        long start = System.currentTimeMillis();
        // TODO: Well, this hurts...  We wil probably need to use retrieve(...) and operate in chunks.
        // https://github.com/infinispan/infinispan/pull/4975
        final Map<String, Action> whitelist = whitelistCache.withFlags(Flag.SKIP_CACHE_LOAD).keySet().stream().collect(Collectors.toMap(Function.identity(), s -> Action.WHITE));
        log.info("WhitelistProtostreamGenerator: Pulling whitelist data took: " + (System.currentTimeMillis() - start) + " ms");
        start = System.currentTimeMillis();
        final SerializationContext ctx = ProtobufUtil.newSerializationContext(new Configuration.Builder().build());
        ctx.registerProtoFiles(FileDescriptorSource.fromResources(SINKIT_CACHE_PROTOBUF));
        ctx.registerMarshaller(new SinkitCacheEntryMarshaller());
        ctx.registerMarshaller(new CoreCacheMarshaller());
        ctx.registerMarshaller(new ActionMarshaller());

        final Path whiteListFilePathTmpP = Paths.get(whiteListFilePathTmp);
        final Path whiteListFilePathP = Paths.get(whiteListFilePath);
        Files.newByteChannel(whiteListFilePathTmpP, options, attr).write(ProtobufUtil.toByteBuffer(ctx, whitelist));
        log.info("WhitelistProtostreamGenerator: Serialization to " + whiteListFilePathTmp + " took: " + (System.currentTimeMillis() - start) + " ms");
        start = System.currentTimeMillis();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(new File(whiteListFilePathTmp));
            Files.write(Paths.get(whiteListFileMd5Tmp), DigestUtils.md5Hex(fis).getBytes());
            // There is a race condition when we swap files while REST API is reading them...
            Files.move(whiteListFilePathTmpP, whiteListFilePathP, REPLACE_EXISTING);
            Files.move(Paths.get(whiteListFileMd5Tmp), Paths.get(whiteListFileMd5), REPLACE_EXISTING);
        } catch (IOException e) {
            log.severe("WhitelistProtostreamGenerator: failed protofile manipulation");
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    log.severe("WhitelistProtostreamGenerator: Failed to close MD5 file stream.");
                }
            }
        }
        log.info("WhitelistProtostreamGenerator: MD5 sum and move took: " + (System.currentTimeMillis() - start) + " ms");
    }
}
