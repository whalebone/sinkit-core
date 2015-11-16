package biz.karms.sinkit.tests.util;

import biz.karms.sinkit.ejb.impl.ArchiveServiceEJB;
import biz.karms.sinkit.ioc.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @author Michal Karm Babacek
 */
public class IoCFactory {
    public static IoCRecord getIoCRecord(String taxonomy, String type, String documentId, String feedName, String feedURL,
                                         String fqdn, IoCSourceIdType sourceType, String sourceIdKey,
                                         String  sourceIPKey, String reverseDomainName) {
        IoCRecord ioCRecord = new IoCRecord();
        ioCRecord.setActive(true);
        IoCClassification ioCClassification = new IoCClassification();
        ioCClassification.setTaxonomy(taxonomy);
        ioCClassification.setType(type);
        ioCRecord.setClassification(ioCClassification);
        IoCDescription ioCDescription = new IoCDescription();
        ioCDescription.setText("test");
        ioCRecord.setDescription(ioCDescription);
        ioCRecord.setDocumentId(documentId);
        IoCFeed ioCFeed = new IoCFeed();
        ioCFeed.setName(feedName);
        ioCFeed.setUrl(feedURL);
        ioCRecord.setFeed(ioCFeed);
        IoCProtocol ioCProtocol = new IoCProtocol();
        ioCProtocol.setApplication("testx");
        ioCRecord.setProtocol(ioCProtocol);
        ioCRecord.setRaw("test_raw");
        IoCSeen ioCSeen = new IoCSeen();
        ioCSeen.setFirst(Calendar.getInstance().getTime());
        ioCSeen.setLast(Calendar.getInstance().getTime());
        ioCRecord.setSeen(ioCSeen);
        IoCSource ioCSource = new IoCSource();
        ioCSource.setAsn(666);
        ioCSource.setAsnName("DevilASN");
        ioCSource.setBgpPrefix("Meh");
        ioCSource.setFQDN(fqdn); //Nope, this is not the key
        IoCGeolocation ioCGeolocation = new IoCGeolocation();
        ioCGeolocation.setCc("CC_test");
        ioCGeolocation.setCity("Zion");
        ioCGeolocation.setLatitude(666.666f);
        ioCGeolocation.setLongitude(666.666f);
        ioCSource.setGeolocation(ioCGeolocation);
        IoCSourceId ioCSourceId = new IoCSourceId();
        ioCSourceId.setType(sourceType);
        ioCSourceId.setValue(sourceIdKey); // This counts for our Infinispan key
        ioCSource.setId(ioCSourceId);
        ioCSource.setIp(sourceIPKey);
        ioCSource.setReverseDomainName(reverseDomainName);
        ioCSource.setUrl("http://BlaBla");
        ioCRecord.setSource(ioCSource);
        IoCTime ioCTime = new IoCTime();
        ioCTime.setObservation(Calendar.getInstance().getTime());
        ioCTime.setSource(Calendar.getInstance().getTime());
        ioCRecord.setTime(ioCTime);

        return ioCRecord;
    }

    /**
     * Creates ioc record object as ti would be recieved from IntelMQ
     */
    public static IoCRecord getIoCRecordAsRecieved(String feedName, String taxonomyType, String sourceId,
                                                   IoCSourceIdType sourcetype, Date observationTime, Date sourceTime)
            throws Exception {

        IoCRecord ioc = new IoCRecord();
        IoCFeed feed = new IoCFeed();
        feed.setName(feedName);
        ioc.setFeed(feed);
        IoCClassification classification = new IoCClassification();
        classification.setType(taxonomyType);
        ioc.setClassification(classification);
        IoCSource source = new IoCSource();
        if (sourcetype == IoCSourceIdType.FQDN) source.setFQDN(sourceId);
        else if (sourcetype == IoCSourceIdType.IP) source.setIp(sourceId);
        else if (sourcetype == IoCSourceIdType.URL) source.setUrl(sourceId);
        else throw new Exception("Unknown source type: " + sourcetype);
        ioc.setSource(source);
        IoCTime time = new IoCTime();
        time.setObservation(observationTime);
        time.setSource(sourceTime);
        ioc.setTime(time);

        return ioc;
    }

    public static String getLogIndex() {
        DateFormat df = new SimpleDateFormat("YYYY-MM-dd");
        return ArchiveServiceEJB.ELASTIC_LOG_INDEX + "-" + df.format(new Date());
    }
}
