package biz.karms.sinkit.ioc;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

import java.io.Serializable;

/**
 * Created by tkozel on 10.7.15.
 */
@Indexed
public class IoCSourceId implements Serializable {

    private static final long serialVersionUID = 2184815523040755395L;

    @Field
    private String value;

    @Field
    private IoCSourceIdType type;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public IoCSourceIdType getType() {
        return type;
    }

    public void setType(IoCSourceIdType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "IoCSourceId{" +
                "value='" + value + '\'' +
                ", type=" + type +
                '}';
    }
}
