package biz.karms.sinkit.ejb.protostream.marshallers;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.infinispan.protostream.MessageMarshaller;

import java.io.IOException;

/**
 * @author Michal Karm Babacek
 */
public class SinkitCacheEntryMarshaller implements MessageMarshaller<ImmutablePair> {

    @Override
    public String getTypeName() {
        return "sinkitprotobuf.Pair";
    }

    @Override
    public Class<ImmutablePair> getJavaClass() {
        return ImmutablePair.class;
    }

    @Override
    public ImmutablePair readFrom(ProtoStreamReader reader) throws IOException {
        return new ImmutablePair<>(reader.readString("key"), reader.readBoolean("value"));
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, ImmutablePair pair) throws IOException {
        writer.writeString("key", (String) pair.getLeft());
        writer.writeBoolean("value", (Boolean) pair.getRight());
    }
}
