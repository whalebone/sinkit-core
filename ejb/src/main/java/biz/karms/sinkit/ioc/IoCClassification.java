package biz.karms.sinkit.ioc;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

import java.io.Serializable;

/**
 * Created by tkozel on 24.6.15.
 */
@Indexed
public class IoCClassification implements Serializable {

    private static final long serialVersionUID = 2184815523047755694L;

    @Field
    private String type;

    @Field
    private String taxonomy;

    public IoCClassification() {}

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTaxonomy() {
        return taxonomy;
    }

    public void setTaxonomy(String taxonomy) {
        this.taxonomy = taxonomy;
    }

    @Override
    public String toString() {
        return "IoCClassification{" +
                "type='" + type + '\'' +
                ", taxonomy='" + taxonomy + '\'' +
                '}';
    }
}
