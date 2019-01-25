package biz.karms.sinkit.ejb.impl;

import biz.karms.sinkit.ejb.elastic.ElasticService;
import biz.karms.sinkit.ejb.elastic.logstash.LogstashClient;
import biz.karms.sinkit.ejb.util.IoCIdentificationUtils;
import biz.karms.sinkit.eventlog.EventLogRecord;
import biz.karms.sinkit.ioc.IoCClassification;
import biz.karms.sinkit.ioc.IoCFeed;
import biz.karms.sinkit.ioc.IoCRecord;
import biz.karms.sinkit.ioc.IoCSource;
import biz.karms.sinkit.ioc.IoCTime;
import org.elasticsearch.index.query.FilteredQueryBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author Krystof Kolar
 * @author Tomas Kozel
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

    @Test
    public void findIoCsForDeactivationTest() throws Exception {
        ArgumentCaptor<FilteredQueryBuilder> queryCaptor = ArgumentCaptor.forClass(FilteredQueryBuilder.class);
        int hoursInactive = 1;

        long rangeStart = ZonedDateTime.now().minusHours(hoursInactive).withNano(0).toEpochSecond();
        archiveService.findIoCsForDeactivation(hoursInactive);
        long rangeEnd = ZonedDateTime.now().minusHours(hoursInactive).withNano(0).toEpochSecond();

        verify(elasticService).search(queryCaptor.capture(), (SortBuilder) isNull(),
                eq(ArchiveServiceEJB.ELASTIC_IOC_INDEX_ACTIVE),
                eq(ArchiveServiceEJB.ELASTIC_IOC_TYPE), eq(IoCRecord.class));
        // ugly way how to assert the search parameters
        // FilteredQueryBuilder doesn't provide getters or any other way how to access the parameters
        String query = queryCaptor.getValue().toString();
        assertThat(query, is(not(nullValue())));
        //(?s) for Pattern.DOTALL to make . matches new lines, (?i) for insensitive case
        assertThat(query, matchesPattern("(?s).*\"active\"\\s*:\\s*(?i)true.*"));
        Pattern pattern = Pattern.compile("(?s).*\"to\"\\s*:\\s*\"([^\"]+)\".*");
        Matcher matcher = pattern.matcher(query);
        assertTrue(matcher.find());
        long to = ZonedDateTime.parse(matcher.group(1),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXXX")).toEpochSecond();
        assertTrue(to >= rangeStart);
        assertTrue(to <= rangeEnd);

        verifyNoMoreInteractions(elasticService);
    }

    /**
     * Test of the deactivateRecord method of archiveService
     * @throws Exception
     */
    @Test
    public void deactivateRecordTest() throws Exception {
        //prepare
        IoCRecord ioc= createActiveIoC("irrelevantId", null, "gooddomain.malware.com","SomeFeed", 1, "malware");

        // verify that ioc to be deactivated is active (just for sure)
        assertTrue(ioc.getActive());

        long beforeDeactivation = new Date().getTime();
        archiveService.deactivateRecord(ioc);
        long afterDeactivation = new Date().getTime();

        assertThat(ioc.getTime().getDeactivated(), is(not(nullValue())));
        assertThat(ioc.getTime().getDeactivated().getTime(), is(greaterThanOrEqualTo(beforeDeactivation)));
        assertThat(ioc.getTime().getDeactivated().getTime(), is(lessThanOrEqualTo(afterDeactivation)));
        assertFalse(ioc.getActive());

        // assert that document id has been recomputed
        assertThat(ioc.getDocumentId(), is(not("irrelevantId")));
        assertThat(ioc.getDocumentId(), is(IoCIdentificationUtils.computeHashedId(ioc)));

        verify(elasticService).delete(ioc,
                ArchiveServiceEJB.ELASTIC_IOC_INDEX_ACTIVE,
                ArchiveServiceEJB.ELASTIC_IOC_TYPE);
        verify(elasticService).index(ioc,
                ArchiveServiceEJB.ELASTIC_IOC_INDEX_INACTIVE
                        + "-" + new SimpleDateFormat(ArchiveServiceEJB.ELASTIC_IOC_INDEX_INACTIVE_SUFFIX_FORMAT)
                        .format(ioc.getTime().getDeactivated()),
                ArchiveServiceEJB.ELASTIC_IOC_TYPE);
        verifyNoMoreInteractions(elasticService);
    }

    @Test
    public void archiveEventLogRecord() throws Exception {
        EventLogRecord logRecord = new EventLogRecord();
        logRecord.setLogged(Date.from(LocalDateTime.of(1918, Month.OCTOBER, 28, 12, 0, 0).atZone(ZoneId.systemDefault()).toInstant()));

        archiveService.archiveEventLogRecord(logRecord);

        verify(elasticService).index(logRecord,
                ArchiveServiceEJB.ELASTIC_LOG_INDEX + "-" + "1918-10-28",
                ArchiveServiceEJB.ELASTIC_LOG_TYPE);
        verifyNoMoreInteractions(elasticService);
    }

    /**
     * creates IoCRecord observed ageHours in the past
     * @param documentId ioc identification
     * @param ip
     * @param fqdn
     * @param sourceName feed name
     * @param ageHours sets time.observation to now - ageHours
     * @return created IoC object
     */
    private static IoCRecord createActiveIoC(String documentId, String ip, String fqdn, String sourceName, int ageHours,
                                             String classificationType) {
        IoCRecord ioc = new IoCRecord();

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR,-ageHours);
        ioc.setTime(new IoCTime());
        ioc.getTime().setObservation(cal.getTime());

        ioc.setClassification(new IoCClassification());
        ioc.getClassification().setType(classificationType);

        ioc.setSource(new IoCSource());
        ioc.getSource().setIp(ip);
        ioc.getSource().setFqdn(fqdn);

        ioc.setFeed(new IoCFeed());
        ioc.getFeed().setName(sourceName);

        ioc.setActive(true);

        ioc.setDocumentId(documentId);

        return ioc;
    }
}
