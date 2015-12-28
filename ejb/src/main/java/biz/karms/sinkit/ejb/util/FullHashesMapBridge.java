package biz.karms.sinkit.ejb.util;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by tom on 11/27/15.
 *
 * @author Tomas Kozel
 */
public class FullHashesMapBridge {

    public Object stringToObject(String stringValue) {
        return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create().fromJson(stringValue, new TypeToken<HashMap<String, HashSet<String>>>() {
        }.getType());
    }

    public String objectToString(Object object) {
        return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create().toJson(object);
    }
}
