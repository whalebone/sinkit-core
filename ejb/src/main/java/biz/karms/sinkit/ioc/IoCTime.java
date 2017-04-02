package biz.karms.sinkit.ioc;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Tomas Kozel
 */
public class IoCTime implements Serializable {

    private static final long serialVersionUID = 3518598035766322842L;

    private Date source;
    private Date observation;
    @SerializedName("received_by_core")
    private Date receivedByCore;
    private Date deactivated;
    private Date whitelisted;

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

    public Date getWhitelisted() {
        return whitelisted;
    }

    public void setWhitelisted(Date whitelisted) {
        this.whitelisted = whitelisted;
    }

    @Override
    public String toString() {
        return "IoCTime{" +
                "source=" + source +
                ", observation=" + observation +
                ", receivedByCore=" + receivedByCore +
                ", deactivated=" + deactivated +
                ", whitelisted=" + whitelisted +
                '}';
    }
}
