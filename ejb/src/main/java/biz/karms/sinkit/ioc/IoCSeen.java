package biz.karms.sinkit.ioc;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Tomas Kozel
 */
public class IoCSeen implements Serializable {

    private static final long serialVersionUID = -8503565375996995715L;

    private Date first;
    private Date last;

    public Date getFirst() {
        return first;
    }

    public void setFirst(Date first) {
        this.first = first;
    }

    public Date getLast() {
        return last;
    }

    public void setLast(Date last) {
        this.last = last;
    }

    @Override
    public String toString() {
        return "IoCSeen{" +
                "first=" + first +
                ", last=" + last +
                '}';
    }
}
