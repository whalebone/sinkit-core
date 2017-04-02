package biz.karms.sinkit.ejb.protostream.marshallers;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.infinispan.protostream.MessageMarshaller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Michal Karm Babacek
 */
public class CoreCacheMarshaller implements MessageMarshaller<HashMap> {

    @Override
    public String getTypeName() {
        return "sinkitprotobuf.CoreCache";
    }

    @Override
    public Class<HashMap> getJavaClass() {
        return HashMap.class;
    }

    @Override
    public HashMap readFrom(ProtoStreamReader reader) throws IOException {
        final HashMap<String, Boolean> sources = new HashMap<>();
        reader.readCollection("record", new ArrayList<>(), ImmutablePair.class).forEach(p -> sources.put((String) p.getLeft(), (Boolean) p.getRight()));
        return sources;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, HashMap map) throws IOException {
        @SuppressWarnings("unchecked")
        final Stream<Map.Entry<String, Boolean>> map1 = map.entrySet().stream();
        final List<ImmutablePair> records = map1.map(e -> new ImmutablePair<>(e.getKey(), e.getValue())).collect(Collectors.toList());
        writer.writeCollection("record", records, ImmutablePair.class);
    }
}
