package biz.karms.sinkit.ioc;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Tomas Kozel
 */
@Getter
@Setter
public class IoCSourceId implements Serializable {

    private static final long serialVersionUID = 2510134193901542038L;

    private String value;
    private IoCSourceIdType type;

    @Override
    public String toString() {
        return "IoCSourceId{" +
                "value='" + value + '\'' +
                ", type=" + type +
                '}';
    }
}
