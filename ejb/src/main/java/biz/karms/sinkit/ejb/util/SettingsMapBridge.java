package biz.karms.sinkit.ejb.util;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hibernate.search.bridge.TwoWayStringBridge;

import java.util.HashMap;

/**
 * @author Michal Karm Babacek
 */
public class SettingsMapBridge implements TwoWayStringBridge {

    public Object stringToObject(String stringValue) {
        return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create().fromJson(stringValue, new TypeToken<HashMap<String, ImmutablePair<String, String>>>() {
        }.getType());
    }

    public String objectToString(Object object) {
        return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create().toJson(object);
    }
}
