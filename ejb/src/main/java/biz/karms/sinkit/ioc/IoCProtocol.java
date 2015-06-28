package biz.karms.sinkit.ioc;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

/**
 * Created by tkozel on 25.6.15.
 */
@Indexed
public class IoCProtocol {

    private static final long serialVersionUID = 2184815523047755698L;

    @Field
    private String application;

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    @Override
    public String toString() {
        return "IoCProtocol{" +
                "application='" + application + '\'' +
                '}';
    }
}
