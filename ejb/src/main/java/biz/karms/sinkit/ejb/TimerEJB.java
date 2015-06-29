package biz.karms.sinkit.ejb;

import biz.karms.sinkit.ioc.IoCRecord;

import javax.ejb.*;

/**
 * Created by tkozel on 29.6.15.
 */
@Singleton
public class TimerEJB {

    @EJB
    private IoCDeactivatorEJB iocDeactivator;

    @Lock(LockType.READ)
//    @Schedules({
//        @Schedule(hour = "*", minute = "*", second = "0", persistent = false),
//        @Schedule(hour = "*", minute = "*", second = "30", persistent = false)
//    })
    @Schedule(hour = "*", minute = "0", second = "0", persistent = false)
    public void fireIoCDeactiovation() {
        try {
            iocDeactivator.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
