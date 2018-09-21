package biz.karms.sinkit.ejb.cache.pojo.marshallers;

import biz.karms.sinkit.ioc.IoCClassificationType;
import biz.karms.sinkit.resolver.StrategyParams;
import org.infinispan.protostream.MessageMarshaller;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

public class StrategyParamsMessageMarshallerTest {

    private StrategyParamsMessageMarshaller marshaller;

    @Mock
    private MessageMarshaller.ProtoStreamReader reader;

    @Mock
    private MessageMarshaller.ProtoStreamWriter writer;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        marshaller = new StrategyParamsMessageMarshaller();
    }

    @Test
    public void getJavaClass() throws Exception {
        assertThat(marshaller.getJavaClass(), is(StrategyParams.class));
    }

    @Test
    public void getTypeName() throws Exception {
        assertThat(marshaller.getTypeName(), is("sinkitprotobuf.StrategyParams"));
    }

    @Test
    public void readFrom() throws Exception {
        // preparation
        final Set<String> types = new HashSet<>(Arrays.asList(IoCClassificationType.malware.getLabel(), IoCClassificationType.cc.getLabel()));
        doReturn(types).when(reader).readCollection(eq("types"), argThat(instanceOf(HashSet.class)), eq(String.class));
        doReturn(22).when(reader).readInt("audit");
        doReturn(55).when(reader).readInt("block");

        // calling tested method
        final StrategyParams strategyParams = marshaller.readFrom(reader);

        // verification
        assertThat(strategyParams, notNullValue());
        assertThat(strategyParams.getTypes(), hasItems(IoCClassificationType.malware, IoCClassificationType.cc));
        assertThat(strategyParams.getAudit(), is(22));
        assertThat(strategyParams.getBlock(), is(55));
    }

    @Test
    public void writeTo() throws Exception {

        // preparation
        final Set<IoCClassificationType> types = new HashSet<>(Arrays.asList(IoCClassificationType.malware, IoCClassificationType.cc));
        final StrategyParams strategyParams = new StrategyParams();
        strategyParams.setTypes(types);
        strategyParams.setAudit(33);
        strategyParams.setBlock(66);

        // calling tested method
        marshaller.writeTo(writer, strategyParams);

        // verification
        verify(writer).writeInt("audit", (Integer) 33);
        verify(writer).writeInt("block", (Integer) 66);
        verify(writer).writeCollection(eq("types"),
                (Set<String>) argThat(hasItems(IoCClassificationType.malware.getLabel(), IoCClassificationType.cc.getLabel())), eq(String.class));

    }

    @Test
    public void writeToParamsNull() throws Exception {
        // preparation
        final StrategyParams strategyParams = Mockito.mock(StrategyParams.class);

        // calling tested method
        marshaller.writeTo(writer, strategyParams);

        // verification
        verify(writer).writeCollection(eq("types"), (Set<String>) argThat(empty()), eq(String.class));

    }
}
