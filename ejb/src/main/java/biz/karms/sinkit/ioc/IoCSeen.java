package biz.karms.sinkit.ioc;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Tomas Kozel
 */
@Getter
@Setter
public class IoCSeen implements Serializable {

    private static final long serialVersionUID = -8503565375996995715L;

    private Date first;
    private Date last;

    @Override
    public String toString() {
        return "IoCSeen{" +
                "first=" + first +
                ", last=" + last +
                '}';
    }
}
