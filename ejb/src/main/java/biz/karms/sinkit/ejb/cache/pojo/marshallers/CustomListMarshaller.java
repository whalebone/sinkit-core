package biz.karms.sinkit.ejb.cache.pojo.marshallers;

import biz.karms.sinkit.ejb.cache.pojo.CustomList;
import org.infinispan.protostream.MessageMarshaller;

import java.io.IOException;

/**
 * @author Michal Karm Babacek
 */
public class CustomListMarshaller implements MessageMarshaller<CustomList> {

    @Override
    public String getTypeName() {
        return "sinkitprotobuf.CustomList";
    }

    @Override
    public Class<CustomList> getJavaClass() {
        return CustomList.class;
    }

    @Override
    public CustomList readFrom(ProtoStreamReader reader) throws IOException {
        final CustomList customList = new CustomList();
        customList.setClientStartAddress(reader.readString("clientStartAddress"));
        customList.setClientEndAddress(reader.readString("clientEndAddress"));
        customList.setClientCidrAddress(reader.readString("clientCidrAddress"));
        customList.setCustomerId(reader.readInt("customerId"));
        customList.setFqdn(reader.readString("fqdn"));
        customList.setListStartAddress(reader.readString("listStartAddress"));
        customList.setListEndAddress(reader.readString("listEndAddress"));
        customList.setListCidrAddress(reader.readString("listCidrAddress"));
        customList.setWhiteBlackLog(reader.readString("whiteBlackLog"));
        return customList;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, CustomList customList) throws IOException {
        writer.writeString("clientStartAddress", customList.getClientStartAddress());
        writer.writeString("clientEndAddress", customList.getClientEndAddress());
        writer.writeString("clientCidrAddress", customList.getClientCidrAddress());
        writer.writeInt("customerId", customList.getCustomerId());
        writer.writeString("fqdn", customList.getFqdn());
        writer.writeString("listStartAddress", customList.getListStartAddress());
        writer.writeString("listEndAddress", customList.getListEndAddress());
        writer.writeString("listCidrAddress", customList.getListCidrAddress());
        writer.writeString("whiteBlackLog", customList.getWhiteBlackLog());
    }
}
