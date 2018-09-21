package biz.karms.sinkit.ejb.cache.pojo.marshallers;

import biz.karms.sinkit.resolver.Policy;
import biz.karms.sinkit.resolver.PolicyCustomList;
import biz.karms.sinkit.resolver.Strategy;
import org.hamcrest.Matchers;
import org.infinispan.protostream.MessageMarshaller;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

public class PolicyMessageMarshallerTest {

    private PolicyMessageMarshaller marshaller;

    @Mock
    private MessageMarshaller.ProtoStreamReader reader;

    @Mock
    private MessageMarshaller.ProtoStreamWriter writer;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        marshaller = new PolicyMessageMarshaller();
    }

    @Test
    public void getJavaClass() throws Exception {
        assertThat(marshaller.getJavaClass(), is(Policy.class));
    }

    @Test
    public void getTypeName() throws Exception {
        assertThat(marshaller.getTypeName(), is("sinkitprotobuf.Policy"));
    }

    @Test
    public void readFrom() throws Exception {
        // preparation
        final Strategy tmpStrategy = Mockito.mock(Strategy.class);
        final PolicyCustomList tmpCustomLists = Mockito.mock(PolicyCustomList.class);

        doReturn(123).when(reader).readInt("id");
        doReturn(new HashSet<>(Collections.singletonList("10.20.30.40/8"))).when(reader).readCollection(eq("ipRanges"), argThat(Matchers.instanceOf(
                LinkedHashSet.class)), eq(String.class));
        doReturn(new HashSet<>(Collections.singletonList("darkportal.com"))).when(reader).readCollection(eq("blacklistedFeeds"), argThat(Matchers.instanceOf(
                LinkedHashSet.class)), eq(String.class));
        doReturn(new HashSet<>(Collections.singletonList("whiteportal.com"))).when(reader).readCollection(eq("accuracyFeeds"), argThat(Matchers.instanceOf(
                LinkedHashSet.class)), eq(String.class));
        doReturn(tmpStrategy).when(reader).readObject("strategy", Strategy.class);
        doReturn(tmpCustomLists).when(reader).readObject("customlists", PolicyCustomList.class);

        // calling tested method
        final Policy policy = marshaller.readFrom(reader);

        //verification
        assertThat(policy, notNullValue());
        assertThat(policy.getId(), is(123));
        assertThat(policy.getIpRanges(), notNullValue());
        assertThat(policy.getIpRanges(), hasItems("10.20.30.40/8"));
        assertThat(policy.getBlacklistedFeeds(), hasItems("darkportal.com"));
        assertThat(policy.getAccuracyFeeds(), hasItems("whiteportal.com"));
        assertThat(policy.getStrategy(), is(tmpStrategy));
        assertThat(policy.getCustomlists(), is(tmpCustomLists));


    }

    @Test
    public void writeTo() throws Exception {

        // preparation
        final Strategy tmpStrategy = Mockito.mock(Strategy.class);
        final PolicyCustomList tmpCustomLists = Mockito.mock(PolicyCustomList.class);

        final Set<String> accuracySet = new HashSet<>(Collections.singletonList("whiteportal.com"));
        final Set<String> ipRanges = new HashSet<>(Collections.singletonList("10.20.30.40/8"));
        final Set<String> blackList = new HashSet<>(Collections.singletonList("blackportal.com"));

        final Policy policy = new Policy();
        policy.setId(123);
        policy.setCustomlists(tmpCustomLists);
        policy.setStrategy(tmpStrategy);
        policy.setIpRanges(ipRanges);
        policy.setBlacklistedFeeds(blackList);
        policy.setAccuracyFeeds(accuracySet);

        // calling tested method
        marshaller.writeTo(writer, policy);

        //verification
        verify(writer).writeInt("id", 123);
        verify(writer).writeCollection("accuracyFeeds", accuracySet, String.class);
        verify(writer).writeCollection("blacklistedFeeds", blackList, String.class);
        verify(writer).writeCollection("ipRanges", ipRanges, String.class);
        verify(writer).writeObject("customlists", tmpCustomLists, PolicyCustomList.class);
        verify(writer).writeObject("strategy", tmpStrategy, Strategy.class);

    }

    @Test
    public void writeToNullValues() throws Exception {
        // preparation
        final Strategy tmpStrategy = Mockito.mock(Strategy.class);
        final Policy policy = new Policy();
        policy.setId(123);
        policy.setStrategy(tmpStrategy);

        // calling tested method
        marshaller.writeTo(writer, policy);

        //verification
        verify(writer).writeInt("id", 123);
        verify(writer).writeCollection(eq("accuracyFeeds"), argThat(instanceOf(LinkedHashSet.class)), eq(String.class));
        verify(writer).writeCollection(eq("blacklistedFeeds"), argThat(instanceOf(LinkedHashSet.class)), eq(String.class));
        verify(writer).writeCollection(eq("ipRanges"), argThat(instanceOf(LinkedHashSet.class)), eq(String.class));
        verify(writer).writeObject(eq("customlists"), argThat(instanceOf(PolicyCustomList.class)), eq(PolicyCustomList.class));
        verify(writer).writeObject("strategy", tmpStrategy, Strategy.class);
    }

}
