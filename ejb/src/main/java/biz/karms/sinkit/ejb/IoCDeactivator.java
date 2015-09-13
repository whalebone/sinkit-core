package biz.karms.sinkit.ejb;

import javax.ejb.Local;

/**
 * @author Michal Karm Babacek
 */
@Local
public interface IoCDeactivator {
    void initialize(String info);

    void stop();
}
