package biz.karms.sinkit.ioc;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Tomas Kozel
 */
@Getter
@Setter
public class IoCDescription implements Serializable {

    private static final long serialVersionUID = -2234084703962020531L;

    private String text;

    @Override
    public String toString() {
        return "IoCDescription{" +
                "text='" + text + '\'' +
                '}';
    }
}
