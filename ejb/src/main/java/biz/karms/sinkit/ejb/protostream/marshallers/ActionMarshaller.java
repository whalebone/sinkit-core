package biz.karms.sinkit.ejb.protostream.marshallers;


import biz.karms.sinkit.ejb.protostream.Action;
import org.infinispan.protostream.EnumMarshaller;

/**
 * @author Michal Karm Babacek
 */
public class ActionMarshaller implements EnumMarshaller<Action> {

    @Override
    public Class<? extends Action> getJavaClass() {
        return Action.class;
    }

    @Override
    public String getTypeName() {
        return "sinkitprotobuf.Action";
    }

    @Override
    public Action decode(int enumValue) {
        switch (enumValue) {
            case 0:
                return Action.BLACK;
            case 1:
                return Action.WHITE;
            case 2:
                return Action.LOG;
            case 3:
                return Action.CHECK;
        }
        return null;  // unknown value
    }

    @Override
    public int encode(Action action) {
        switch (action) {
            case BLACK:
                return 0;
            case WHITE:
                return 1;
            case LOG:
                return 2;
            case CHECK:
                return 3;
            default:
                throw new IllegalArgumentException("Unexpected Action value : " + action);
        }
    }
}
