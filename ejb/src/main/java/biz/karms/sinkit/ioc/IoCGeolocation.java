package biz.karms.sinkit.ioc;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author Tomas Kozel
 */
@Getter
@Setter
public class IoCGeolocation implements Serializable {

    private static final long serialVersionUID = -7300830254275899055L;

    private String cc;
    private String city;
    private Float latitude;
    private Float longitude;

    @Override
    public String toString() {
        return "IoCGeolocation{" +
                "cc='" + cc + '\'' +
                ", city='" + city + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }
}
