package biz.karms.sinkit.ioc;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author Tomas Kozel
 */
@Getter
@Setter
public class IoCFeed implements Serializable {

    private static final long serialVersionUID = -5066334431395905204L;

    private String url;
    private String name;
    private Integer accuracy;

    @Override
    public String toString() {
        return "IoCFeed{" +
                "url='" + url + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
