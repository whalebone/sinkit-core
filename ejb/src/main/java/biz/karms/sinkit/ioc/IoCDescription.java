package biz.karms.sinkit.ioc;

import java.io.Serializable;

/**
 * Created by tkozel on 24.6.15.
 */
public class IoCDescription implements Serializable {

    private static final long serialVersionUID = -2234084703962020531L;

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
