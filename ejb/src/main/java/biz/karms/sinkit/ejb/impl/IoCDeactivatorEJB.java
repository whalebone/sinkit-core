package biz.karms.sinkit.ejb.impl;

import biz.karms.sinkit.ejb.CoreService;
import biz.karms.sinkit.ejb.IoCDeactivator;
import biz.karms.sinkit.exception.ArchiveException;

import javax.annotation.Resource;
import javax.ejb.*;
import javax.inject.Inject;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Created by tkozel on 29.6.15.
 */
@Singleton
@AccessTimeout(value = 40, unit = TimeUnit.MINUTES)
public class IoCDeactivatorEJB implements IoCDeactivator {

    private AtomicBoolean busy = new AtomicBoolean(false);

    @EJB
    private CoreService coreService;

    @Inject
    private Logger log;

    @Resource
    private TimerService timerService;

    @Override
    public void initialize(String info) {
        ScheduleExpression sexpr = new ScheduleExpression();
        // Every hour
        sexpr.hour("*").minute("0").second("0");
        timerService.createCalendarTimer(sexpr, new TimerConfig(info, false));
    }

    @Override
    public void stop() {
        log.info("Stop all existing IoCDeactivator HASingleton timers.");
        for (Timer timer : timerService.getTimers()) {
            log.fine("Stop IoCDeactivator HASingleton timer: " + timer.getInfo());
            timer.cancel();
        }
    }

    @Timeout
    public void scheduler(Timer timer) throws InterruptedException, ArchiveException {
        log.info("IoCDeactivator HASingletonTimer: Info=" + timer.getInfo());

        if (!busy.compareAndSet(false, true)) {
            log.info("Deactivation still in progress -> skipping next run");
            return;
        }

        try {
            coreService.deactivateIocs();
        } finally {
            busy.set(false);
        }
    }
}
