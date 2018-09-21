package biz.karms.sinkit.ioc;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Tomas Kozel
 */
@Getter
@Setter
public class IoCTime implements Serializable {

    private static final long serialVersionUID = 3518598035766322842L;

    private Date source;
    private Date observation;
    @SerializedName("received_by_core")
    private Date receivedByCore;
    private Date deactivated;
    private Date whitelisted;

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
