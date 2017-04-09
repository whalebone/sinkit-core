package biz.karms.sinkit.ejb.impl;

import biz.karms.sinkit.ejb.CoreService;
import biz.karms.sinkit.exception.ArchiveException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.ScheduleExpression;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.inject.Inject;
import java.util.logging.Logger;

/**
 * @author Tomas Kozel
 * @author Michal Karm Babacek
 */
@Singleton
@LocalBean
@Startup
public class IoCDeactivatorEJB {

    @EJB
    private CoreService coreService;

    @Inject
    private Logger log;

    @Resource
    private TimerService timerService;

    public static final boolean SINKIT_IOC_DEACTIVATOR_SKIP = (System.getenv().containsKey("SINKIT_IOC_DEACTIVATOR_SKIP")) && Boolean.parseBoolean(System.getenv("SINKIT_IOC_DEACTIVATOR_SKIP"));

    @PostConstruct
    private void initialize() {
        if (!SINKIT_IOC_DEACTIVATOR_SKIP) {
            timerService.createCalendarTimer(new ScheduleExpression().hour("*").minute("0").second("0"), new TimerConfig("IoCDeactivator", false));
        }
    }

    @PreDestroy
    public void stop() {
        log.info("Stop all existing IoCDeactivator timers.");
        for (Timer timer : timerService.getTimers()) {
            log.fine("Stop IoCDeactivator timer: " + timer.getInfo());
            timer.cancel();
        }
    }

    @Timeout
    public void scheduler(Timer timer) throws InterruptedException, ArchiveException {
        log.info("IoCDeactivator: Info=" + timer.getInfo());
        coreService.deactivateIocs();
    }
}
