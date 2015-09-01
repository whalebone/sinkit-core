package biz.karms.sinkit.ioc;

/**
 * Created by tkozel on 24.6.15.
 */

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Date;

public class IoCTime implements Serializable {

    private static final long serialVersionUID = 3518598035766322842L;

    private Date source;
    private Date observation;
    @SerializedName("received_by_core")
    private Date receivedByCore;
    private Date deactivated;

    public Date getSource() {
        return source;
    }

    public void setSource(Date source) {
        this.source = source;
    }

    public Date getObservation() {
        return observation;
    }

    public void setObservation(Date observation) {
        this.observation = observation;
    }

    public Date getReceivedByCore() {
        return receivedByCore;
    }

    public void setReceivedByCore(Date receivedByCore) {
        this.receivedByCore = receivedByCore;
    }

    public Date getDeactivated() {
        return deactivated;
    }

    public void setDeactivated(Date deactivated) {
        this.deactivated = deactivated;
    }

    @Override
    public String toString() {
        return "IoCTime{" +
                "source=" + source +
                ", observation=" + observation +
                ", receivedByCore=" + receivedByCore +
                ", deactivated=" + deactivated +
                '}';
    }
}
