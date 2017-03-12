package biz.karms.sinkit.ejb.elastic;

import java.io.Serializable;

/**
 * Created by Tomas Kozel
 */
public interface Indexable extends Serializable {

    String getDocumentId();

    void setDocumentId(String documentId);
}
