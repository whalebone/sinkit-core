package biz.karms.sinkit.ioc;

/**
 * Created by tkozel on 24.6.15.
 */

import java.io.Serializable;
import java.util.Date;

public class IoCTime implements Serializable {

    private static final long serialVersionUID = 3518598035766322842L;

    private Date source;
    private Date observation;

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

    @Override
    public String toString() {
        return "IoCTime{" +
                "source='" + source + '\'' +
                ", observation='" + observation + '\'' +
                '}';
    }
}
