package biz.karms.sinkit.ioc;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

import java.io.Serializable;

/**
 * Created by tkozel on 24.6.15.
 */
@Indexed
public class IoCDescription implements Serializable {

    private static final long serialVersionUID = 2184815523047755693L;

    @Field
    private String text;

    public IoCDescription() {}

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "IoCDescription{" +
                "text='" + text + '\'' +
                '}';
    }
}
