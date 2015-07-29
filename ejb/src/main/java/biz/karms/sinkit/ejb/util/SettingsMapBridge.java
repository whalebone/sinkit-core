package biz.karms.sinkit.ejb.util;

import com.google.common.base.Splitter;
import org.hibernate.search.bridge.TwoWayStringBridge;

import java.util.HashMap;

/**
 * @author Michal Karm Babacek
 */
public class SettingsMapBridge implements TwoWayStringBridge {

    public Object stringToObject(String stringValue) {
        return new HashMap<>(Splitter.on(",").withKeyValueSeparator(":").split(stringValue));
    }

    public String objectToString(Object object) {
        return object.toString();
    }
}
