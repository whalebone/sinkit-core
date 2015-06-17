package biz.karms.sinkit.ejb.util;

import org.infinispan.query.Transformable;
import org.infinispan.query.Transformer;

import java.math.BigInteger;
import java.util.Random;

/**
 * Created by karm on 6/23/15.
 */
@Transformable(transformer = BigIntegerTransformable.BigIntegerTransformer.class)
public class BigIntegerTransformable extends BigInteger{
    public BigIntegerTransformable(byte[] val) {
        super(val);
    }

    public BigIntegerTransformable(int signum, byte[] magnitude) {
        super(signum, magnitude);
    }

    public BigIntegerTransformable(String val, int radix) {
        super(val, radix);
    }

    public BigIntegerTransformable(String val) {
        super(val);
    }

    public BigIntegerTransformable(int numBits, Random rnd) {
        super(numBits, rnd);
    }

    public BigIntegerTransformable(int bitLength, int certainty, Random rnd) {
        super(bitLength, certainty, rnd);
    }

    public static class BigIntegerTransformer implements Transformer {



        @Override
        public Object fromString(String s) {
            return new BigInteger(s);
        }

        @Override
        public String toString(Object customType) {
            return customType.toString();
        }
    }
}
