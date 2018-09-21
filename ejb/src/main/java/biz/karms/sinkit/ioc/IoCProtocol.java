package biz.karms.sinkit.ioc;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author Tomas Kozel
 */
@Getter
@Setter
public class IoCProtocol implements Serializable {

    private static final long serialVersionUID = -5416774385239247512L;

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
