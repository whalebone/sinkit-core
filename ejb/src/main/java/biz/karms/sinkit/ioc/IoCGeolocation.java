package biz.karms.sinkit.ioc;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.NumericField;

import java.io.Serializable;

/**
 * Created by tkozel on 25.6.15.
 */
@Indexed
public class IoCGeolocation implements Serializable {

    private static final long serialVersionUID = 2184815523047755698L;

    @Field
    private String cc;

    @Field
    private String city;

    @Field
    @NumericField
    private Float latitude;

    @Field
    @NumericField
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
