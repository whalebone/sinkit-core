package biz.karms.sinkit.eventlog;

import java.util.Calendar;
import java.util.Date;

/**
 * @author Michal Karm Babacek
 */
public class EventLogFactory {
    public static EventLogRecord getEventLogRecord(EventLogAction action, String clientIPAddress, String[] matchedIoCs, boolean isFQDN, String fqdnOrIp, String documentId) {
        EventLogRecord eventLogRecord = new EventLogRecord();
        eventLogRecord.setAction(action);
        eventLogRecord.setClient(clientIPAddress);
        eventLogRecord.setDocumentId(documentId);
        eventLogRecord.setLogged(Calendar.getInstance().getTime());
        // Why Array? It's always gonna be 1 IoC
        eventLogRecord.setMatchedIocs(matchedIoCs);
        EventReason eventReason = new EventReason();
        if (isFQDN) {
            eventReason.setFqdn(fqdnOrIp);
        } else {
            eventReason.setIp(fqdnOrIp);
        }
        eventLogRecord.setReason(eventReason);
        EventDNSRequest eventDNSRequest = new EventDNSRequest();
        // Why? What? How is this different from  eventLogRecord.setClient()? :-(
        //eventDNSRequest.setIp();
        //eventDNSRequest.setRaw();
        eventLogRecord.setRequest(eventDNSRequest);
        // No idea...
        //eventLogRecord.setVirusTotalRequest();
        return eventLogRecord;
    }
}
