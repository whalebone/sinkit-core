package biz.karms.sinkit.eventlog;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Map;

/**
 * @author Michal Karm Babacek
 */
public class Accuracy implements Serializable {

    private static final long serialVersionUID = -3233507641590990917L;

    @SerializedName("accuracy")
    private Integer accuracy;
    @SerializedName("the_most_accurate_feed")
    private Map<String, Map<String, Integer>> theMostAccurateFeed;

    public Accuracy(Integer accuracy, Map<String, Map<String, Integer>> theMostAccurateFeed) {
        this.accuracy = accuracy;
        this.theMostAccurateFeed = theMostAccurateFeed;
    }

    public Integer getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Integer accuracy) {
        this.accuracy = accuracy;
    }

    public Map<String, Map<String, Integer>> getTheMostAccurateFeed() {
        return theMostAccurateFeed;
    }

    public void setTheMostAccurateFeed(Map<String, Map<String, Integer>> theMostAccurateFeed) {
        this.theMostAccurateFeed = theMostAccurateFeed;
    }

    @Override
    public String toString() {
        return "Accuracy{" +
                "accuracy=" + accuracy +
                ", theMostAccurateFeed=" + theMostAccurateFeed +
                '}';
    }
}
