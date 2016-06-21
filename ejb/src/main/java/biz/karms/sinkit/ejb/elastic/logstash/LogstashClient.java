package biz.karms.sinkit.ejb.elastic.logstash;

import biz.karms.sinkit.ejb.elastic.Indexable;
import biz.karms.sinkit.exception.ArchiveException;
import com.google.gson.Gson;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by tkozel on 5/29/16.
 */
public class LogstashClient {

    public static final String LOGSTASH_URL_ENV = "SINKIT_LOGSTASH_URL";
    private static final String LOGSTASH_URL = (System.getenv().containsKey(LOGSTASH_URL_ENV)) ? System.getenv(LOGSTASH_URL_ENV) : "http://localhost:9090/";
    private static final String RESPONSE_OK = "ok";

    private Gson gson;

    public LogstashClient(Gson gson) {
        this.gson = gson;
    }

    public <T extends Indexable> boolean sentToLogstash(final T object, String index, String type) throws ArchiveException {
        final Client client = ClientBuilder.newClient();
        Response response = client.target(LOGSTASH_URL + "/" + index + "/" + type + "/")
                .request()
                .accept(MediaType.TEXT_PLAIN_TYPE)
                .post(Entity.entity(gson.toJson(object), MediaType.APPLICATION_JSON_TYPE));
        String responseMessage = response.readEntity(String.class);
        if (!RESPONSE_OK.equals(responseMessage)) {
            throw new ArchiveException("Unexpected Logstash response: " + responseMessage);
        }
        return true;
    }
}
