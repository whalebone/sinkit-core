package biz.karms.sinkit.ejb.cache.pojo.marshallers;

import biz.karms.sinkit.resolver.Policy;
import biz.karms.sinkit.resolver.ResolverConfiguration;
import org.infinispan.protostream.MessageMarshaller;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

public class ResolverConfigurationMessageMarshallerTest {

    private ResolverConfigurationMessageMarshaller marshaller;

    @Mock
    private MessageMarshaller.ProtoStreamReader reader;

    @Mock
    private MessageMarshaller.ProtoStreamWriter writer;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        marshaller = new ResolverConfigurationMessageMarshaller();
    }

    @Test
    public void getJavaClass() throws Exception {
        assertThat(marshaller.getJavaClass(), is(ResolverConfiguration.class));
    }

    @Test
    public void getTypeName() throws Exception {
        assertThat(marshaller.getTypeName(), is("sinkitprotobuf.ResolverConfiguration"));
    }

    @Test
    public void readFrom() throws Exception {
        // preparation
        final List<Policy> policies = Collections.singletonList(Mockito.mock(Policy.class));
        doReturn(123).when(reader).readInt("resolverId");
        doReturn(2).when(reader).readInt("clientId");
        doReturn(policies).when(reader).readCollection(eq("policies"), argThat(instanceOf(List.class)), eq(Policy.class));

        // calling tested method
        final ResolverConfiguration resolverConfiguration = marshaller.readFrom(reader);

        // verification
        assertThat(resolverConfiguration, notNullValue());
        assertThat(resolverConfiguration.getClientId(), is(2));
        assertThat(resolverConfiguration.getResolverId(), is(123));
        assertThat(resolverConfiguration.getPolicies(), is(policies));
    }

    @Test
    public void writeTo() throws Exception {
        // preparation
        final List<Policy> policies = Collections.singletonList(Mockito.mock(Policy.class));
        final ResolverConfiguration resolverConfiguration = new ResolverConfiguration();
        resolverConfiguration.setResolverId(123);
        resolverConfiguration.setClientId(23);
        resolverConfiguration.setPolicies(policies);

        // calling tested method
        marshaller.writeTo(writer, resolverConfiguration);

        // verification
        verify(writer).writeInt("resolverId", (Integer) 123);
        verify(writer).writeInt("clientId", (Integer) 23);
        verify(writer).writeCollection("policies", policies, Policy.class);

    }

}
