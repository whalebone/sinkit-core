package biz.karms.sinkit.ejb.cache.pojo.marshallers;

import biz.karms.sinkit.resolver.EndUserConfiguration;
import org.infinispan.protostream.MessageMarshaller;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

public class EndUserConfigurationMessageMarshallerTest {

    private EndUserConfigurationMessageMarshaller marshaller;

    @Mock
    private MessageMarshaller.ProtoStreamReader reader;

    @Mock
    private MessageMarshaller.ProtoStreamWriter writer;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        marshaller = new EndUserConfigurationMessageMarshaller();
    }


    @Test
    public void getJavaClass() throws Exception {
        assertThat(marshaller.getJavaClass(), is(EndUserConfiguration.class));
    }

    @Test
    public void getTypeName() throws Exception {
        assertThat(marshaller.getTypeName(), is("sinkitprotobuf.EndUserConfiguration"));
    }


    @Test
    public void readFrom() throws Exception {
        // preparation
        doReturn(2).when(reader).readInt("clientId");
        doReturn("user123").when(reader).readString("userId");
        doReturn(1).when(reader).readInt("policyId");

        final Set<String> identities = Collections.singleton("identity1");
        doReturn(identities).when(reader).readCollection(eq("identities"), argThat(instanceOf(HashSet.class)), eq(String.class));

        final Set<String> whitelist = Collections.singleton("whitelist.org");
        doReturn(whitelist).when(reader).readCollection(eq("whitelist"), argThat(instanceOf(HashSet.class)), eq(String.class));

        final Set<String> blacklist = Collections.singleton("blacklist.org");
        doReturn(blacklist).when(reader).readCollection(eq("blacklist"), argThat(instanceOf(HashSet.class)), eq(String.class));

        // calling tested method
        final EndUserConfiguration configuration = marshaller.readFrom(reader);

        // verification
        assertThat(configuration, notNullValue());
        assertThat(configuration.getClientId(), is(2));
        assertThat(configuration.getPolicyId(), is(1));
        assertThat(configuration.getUserId(), is("user123"));
        assertThat(configuration.getId(), is("2:user123"));
        assertThat(configuration.getIdentities(), is(identities));
        assertThat(configuration.getWhitelist(), is(whitelist));
        assertThat(configuration.getBlacklist(), is(blacklist));
    }

    @Test
    public void writeTo() throws Exception {

        // preparation
        final EndUserConfiguration configuration = new EndUserConfiguration();
        configuration.setUserId("user123");
        configuration.setClientId(33);
        configuration.setPolicyId(4);

        final Set<String> blacklist = Collections.singleton("blacklist.org");
        configuration.setBlacklist(blacklist);

        final Set<String> whitelist = Collections.singleton("whitelist.org");
        configuration.setWhitelist(whitelist);

        final Set<String> identities = Collections.singleton("identity1");
        configuration.setIdentities(identities);

        // calling tested method
        marshaller.writeTo(writer, configuration);

        // verification
        verify(writer).writeInt("clientId", (Integer) 33);
        verify(writer).writeInt("policyId", (Integer) 4);
        verify(writer).writeString("userId", "user123");
        verify(writer).writeCollection("identities", identities, String.class);
        verify(writer).writeCollection("whitelist", whitelist, String.class);
        verify(writer).writeCollection("blacklist", blacklist, String.class);

    }

    @Test
    public void writeToParamsNull() throws Exception {
        // preparation
        final EndUserConfiguration configuration = new EndUserConfiguration();

        // calling tested method
        marshaller.writeTo(writer, configuration);

        // verification
        verify(writer).writeCollection(eq("identities"), (Set<String>) argThat(empty()), eq(String.class));
        verify(writer).writeCollection(eq("whitelist"), (Set<String>) argThat(empty()), eq(String.class));
        verify(writer).writeCollection(eq("blacklist"), (Set<String>) argThat(empty()), eq(String.class));

    }

}
