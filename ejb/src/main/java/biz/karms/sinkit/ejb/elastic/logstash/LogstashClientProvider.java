package biz.karms.sinkit.ejb.elastic.logstash;

import com.google.gson.Gson;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by tkozel on 5/29/16.
 */
@ApplicationScoped
public class LogstashClientProvider {
    @Inject
    private Logger log;

    @Inject
    private Gson gson;

    private LogstashClient logstashClient;

    public LogstashClient getLogstashClient() {
        if (logstashClient == null) {
            log.log(Level.INFO, "Logstash client doesn't exists, creating new one");
            logstashClient = new LogstashClient(gson);
        }
        return logstashClient;
    }
}
