package biz.karms.sinkit.ejb;

import biz.karms.sinkit.exception.ArchiveException;
import biz.karms.sinkit.ioc.IoCRecord;

import javax.ejb.*;
import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Created by tkozel on 29.6.15.
 */
@Startup
@Singleton
@TransactionManagement(TransactionManagementType.BEAN)
public class IoCDeactivatorEJB {

    private AtomicBoolean busy = new AtomicBoolean(false);

    @Inject
    private CoreServiceEJB coreServiceEJB;

    @Inject
    private Logger log;

    @Lock(LockType.WRITE)
    @Schedule(hour = "*", minute = "0", second = "0", persistent = false)
    @AccessTimeout(value = 40, unit = TimeUnit.MINUTES)
    public void run() throws InterruptedException, ArchiveException {

        if (!busy.compareAndSet(false, true)) {
            log.info("Deactivation still in progress -> skipping next run");
            return;
        }

        try {
            coreServiceEJB.deactivateIocs();
        } finally {
            busy.set(false);
        }
    }
}
