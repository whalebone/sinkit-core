package biz.karms.sinkit.ejb.impl;

import biz.karms.sinkit.ejb.elastic.ElasticService;
import biz.karms.sinkit.ejb.elastic.logstash.LogstashClient;
import biz.karms.sinkit.ejb.util.IoCIdentificationUtils;

import biz.karms.sinkit.ioc.IoCClassification;
import biz.karms.sinkit.ioc.IoCFeed;
import biz.karms.sinkit.ioc.IoCRecord;
import biz.karms.sinkit.ioc.IoCSource;
import biz.karms.sinkit.ioc.IoCTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Calendar;
import java.util.Date;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author Krystof Kolar
 */
@RunWith(MockitoJUnitRunner.class)
public class ArchiveServiceEJBTest {

    @Mock
    private Logger log;

    @Mock
    private ElasticService elasticService;

    @Mock
    private LogstashClient logstashClient;

    @InjectMocks
    private ArchiveServiceEJB archiveService;

    /**
     * Test of the deactivateRecord method of archiveService
     * @throws Exception
     */
    @Test
    public void deactivateRecordTest() throws Exception{
        //prepare
        IoCRecord ioc= getIoCRecord(null, "gooddomain.malware.com","SomeFeed",
                1, "malware");

        //run tested method
        archiveService.deactivateRecord(ioc);

        //verify
        verify(elasticService).delete(ioc,
                archiveService.ELASTIC_IOC_INDEX,
                archiveService.ELASTIC_IOC_TYPE);
        verify(elasticService).index(ioc,
                archiveService.ELASTIC_IOC_INDEX,
                archiveService.ELASTIC_IOC_TYPE);
        verifyNoMoreInteractions(elasticService);
        assertEquals(false,ioc.getActive());
        // important for documentId to be recomputed
        assertEquals(IoCIdentificationUtils.computeHashedId(ioc),
                ioc.getDocumentId());
    }

    /**
     * creates IoCRecord observed ageHours in the past
     * @param ip
     * @param fqdn
     * @param sourceName feed name
     * @param ageHours sets time.observation to current
     * @return
     */
    public IoCRecord getIoCRecord(String ip, String fqdn, String sourceName, int ageHours, String classificationType) {
        IoCRecord ioc = new IoCRecord();

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR,-ageHours);
        Date observation = cal.getTime();
        IoCTime time = new IoCTime();
        ioc.setTime(time);
        ioc.getTime().setObservation(observation);

        IoCClassification classification = new IoCClassification();
        classification.setType(classificationType);
        ioc.setClassification(classification);

        ioc.setSource(new IoCSource());
        ioc.getSource().setIp(ip);
        ioc.getSource().setFqdn(fqdn);

        ioc.setFeed(new IoCFeed());
        ioc.getFeed().setName(sourceName);

        return ioc;
    }
}
