package biz.karms.sinkit.ioc;

import java.io.Serializable;

/**
 * Created by Tomas Kozel
 */
public class IoCGeolocation implements Serializable {

    private static final long serialVersionUID = -7300830254275899055L;

    private String cc;
    private String city;
    private Float latitude;
    private Float longitude;

    public String getCc() {
        return cc;
    }

    public void setCc(String cc) {
        this.cc = cc;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Float getLatitude() {
        return latitude;
    }

    public void setLatitude(Float latitude) {
        this.latitude = latitude;
    }

    public Float getLongitude() {
        return longitude;
    }

    public void setLongitude(Float longitude) {
        this.longitude = longitude;
    }

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
