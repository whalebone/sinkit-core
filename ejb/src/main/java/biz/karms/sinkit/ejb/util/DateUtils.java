package biz.karms.sinkit.ejb.util;

import java.util.Calendar;
import java.util.Date;

/**
 * @author Tomas Kozel
 */
public class DateUtils {

    public static Date addWindow(Date date, int hoursWindow) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.HOUR, hoursWindow);
        return c.getTime();
    }
}
