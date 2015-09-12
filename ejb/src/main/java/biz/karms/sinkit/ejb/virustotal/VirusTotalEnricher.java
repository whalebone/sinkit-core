package biz.karms.sinkit.ejb.virustotal;

/**
 * @author Michal Karm Babacek
 */
public interface VirusTotalEnricher {
    void initialize(String info);
    void stop();
}
