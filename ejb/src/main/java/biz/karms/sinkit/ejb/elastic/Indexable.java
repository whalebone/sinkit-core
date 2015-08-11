package biz.karms.sinkit.ejb.elastic;

import java.io.Serializable;

/**
 * Created by tkozel on 4.8.15.
 */
public interface Indexable extends Serializable {

    public String getDocumentId();

    public void setDocumentId(String documentId);
}
