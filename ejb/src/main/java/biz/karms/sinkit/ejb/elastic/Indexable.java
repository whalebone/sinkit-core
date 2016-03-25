package biz.karms.sinkit.ejb.elastic;

import java.io.Serializable;

/**
 * Created by Tomas Kozel
 */
public interface Indexable extends Serializable {

    public String getDocumentId();

    public void setDocumentId(String documentId);
}
