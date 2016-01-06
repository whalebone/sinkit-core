package biz.karms.sinkit.ejb.hasingleton;

import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.singleton.election.Preference;

/**
 * @author Michal Karm Babacek
 */
public class LikenessNamePreference implements Preference {

    private final String unwantedNameSuffix;
    private final String cacheName;

    public LikenessNamePreference(String unwantedNameSuffix, String cacheName) {
        this.unwantedNameSuffix = unwantedNameSuffix;
        this.cacheName = cacheName;
    }

    /**
     * Node is preferred if it DOESN'T contain the unwanted string in its name.
     */
    @Override
    public boolean preferred(Node node) {
        return node.getName().contains(cacheName) && !node.getName().contains(unwantedNameSuffix);
    }
}
