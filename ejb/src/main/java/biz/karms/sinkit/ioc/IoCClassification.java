package biz.karms.sinkit.ioc;

import java.io.Serializable;

/**
 * Created by Tomas Kozel
 */
public class IoCClassification implements Serializable {

    private static final long serialVersionUID = -5807317040203145173L;

    private String type;
    private String taxonomy;

    public IoCClassification() {
    }

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
