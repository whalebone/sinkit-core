package biz.karms.sinkit.ejb.cache.pojo.marshallers;

import biz.karms.sinkit.resolver.Strategy;
import biz.karms.sinkit.resolver.StrategyParams;
import biz.karms.sinkit.resolver.StrategyType;
import org.infinispan.protostream.MessageMarshaller;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

public class StrategyMessageMarshallerTest {

    private StrategyMessageMarshaller marshaller;

    @Mock
    private MessageMarshaller.ProtoStreamReader reader;

    @Mock
    private MessageMarshaller.ProtoStreamWriter writer;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        marshaller = new StrategyMessageMarshaller();
    }

    @Test
    public void getJavaClass() throws Exception {
        assertThat(marshaller.getJavaClass(), is(Strategy.class));
    }

    @Test
    public void getTypeName() throws Exception {
        assertThat(marshaller.getTypeName(), is("sinkitprotobuf.Strategy"));
    }

    @Test
    public void readFrom() throws Exception {
        // preparation
        final StrategyParams tmpStrategy = Mockito.mock(StrategyParams.class);
        doReturn(StrategyType.whitelist.name()).when(reader).readString("strategyType");
        doReturn(tmpStrategy).when(reader).readObject("strategyParams", StrategyParams.class);

        // calling tested method
        final Strategy strategy = marshaller.readFrom(reader);

        // verification
        assertThat(strategy, notNullValue());
        assertThat(strategy.getStrategyType(), is(StrategyType.whitelist));
        assertThat(strategy.getStrategyParams(), is(tmpStrategy));
    }

    @Test
    public void writeTo() throws Exception {

        // preparation
        final StrategyParams tmpStrategyParams = Mockito.mock(StrategyParams.class);
        final Strategy strategy = new Strategy();
        strategy.setStrategyType(StrategyType.whitelist);
        strategy.setStrategyParams(tmpStrategyParams);

        // calling tested method
        marshaller.writeTo(writer, strategy);

        // verification
        verify(writer).writeString("strategyType", StrategyType.whitelist.name());
        verify(writer).writeObject("strategyParams", tmpStrategyParams, StrategyParams.class);

    }

    @Test
    public void writeToParamsNull() throws Exception {
        // preparation
        final Strategy strategy = new Strategy();
        strategy.setStrategyType(StrategyType.whitelist);

        // calling tested method
        marshaller.writeTo(writer, strategy);

        // verification
        verify(writer).writeString("strategyType", StrategyType.whitelist.name());
        verify(writer).writeObject(eq("strategyParams"), argThat(instanceOf(StrategyParams.class)), eq(StrategyParams.class));
    }
}