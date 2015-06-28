package biz.karms.sinkit.ioc;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

import java.io.Serializable;

/**
 * Created by tkozel on 24.6.15.
 */
@Indexed
public class IoCFeed implements Serializable {

    private static final long serialVersionUID = 2184815523047755692L;

    @Field
    private String url;

    @Field
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
