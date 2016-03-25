package biz.karms.sinkit.ioc;

import java.io.Serializable;

/**
 * Created by Tomas Kozel
 */
public class IoCFeed implements Serializable {

    private static final long serialVersionUID = -5066334431395905204L;

    private String url;
    private String name;

    public IoCFeed() {}

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "IoCFeed{" +
                "url='" + url + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
