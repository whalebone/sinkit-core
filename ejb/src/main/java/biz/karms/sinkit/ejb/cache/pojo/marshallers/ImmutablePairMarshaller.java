package biz.karms.sinkit.ejb.cache.pojo.marshallers;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.infinispan.protostream.MessageMarshaller;

import java.io.IOException;

/**
 * @author Michal Karm Babacek
 */
public class ImmutablePairMarshaller implements MessageMarshaller<ImmutablePair> {

    @Override
    public String getTypeName() {
        return "sinkitprotobuf.Rule.Map";
    }

    @Override
    public Class<ImmutablePair> getJavaClass() {
        return ImmutablePair.class;
    }

    @Override
    public ImmutablePair readFrom(ProtoStreamReader reader) throws IOException {
        return new ImmutablePair<>(reader.readString("feedUid"), reader.readString("mode"));
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, ImmutablePair pair) throws IOException {
        writer.writeString("feedUid", (String) pair.getLeft());
        writer.writeString("mode", (String) pair.getRight());
    }
}
