package biz.karms.sinkit.ioc;

/**
 * Created by tkozel on 24.6.15.
 */

import org.apache.solr.schema.FieldType;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

import java.io.Serializable;
import java.util.Date;

@Indexed
public class IoCTime implements Serializable {

    private static final long serialVersionUID = 2184815523047755696L;

    @Field
    private Date source;

    @Field
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
