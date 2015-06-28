package biz.karms.sinkit.ioc;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by tkozel on 25.6.15.
 */

@Indexed
public class IoCSeen implements Serializable {

    private static final long serialVersionUID = 2184815523047755699L;

    @Field
    private Date first;

    @Field
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
