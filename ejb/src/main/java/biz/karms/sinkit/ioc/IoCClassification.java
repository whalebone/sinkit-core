package biz.karms.sinkit.ioc;

import java.io.Serializable;

/**
 * Created by Tomas Kozel
 */
public class IoCClassification implements Serializable {

    private static final long serialVersionUID = -5212807838160280916L;

    private String type;
    private String taxonomy;
    private String identifier;

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

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public String toString() {
        return "IoCClassification{" +
                "type='" + type + '\'' +
                ", taxonomy='" + taxonomy + '\'' +
                ", identifier='" + identifier + '\'' +
                '}';
    }
}
