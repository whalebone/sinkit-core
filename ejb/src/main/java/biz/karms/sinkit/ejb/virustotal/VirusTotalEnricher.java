package biz.karms.sinkit.ejb.virustotal;

import javax.ejb.Local;

/**
 * @author Michal Karm Babacek
 */
@Local
public interface VirusTotalEnricher {
    void initialize(String info);

    void stop();
}
