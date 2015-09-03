package biz.karms.sinkit.ejb.hasingleton;

import java.io.Serializable;

/**
 * @author Michal Karm Babacek
 */
public class NodeName implements Serializable {
    private static final long serialVersionUID = -28452590384271843L;
    private final String nodeName;

    public NodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getNodeName() {
        return this.nodeName;
    }
}
