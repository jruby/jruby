package org.jruby.truffle.core.numeric;

import com.oracle.truffle.api.object.DynamicObject;
import java.math.BigInteger;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;

public class BignumOperations {

    private static final BigInteger LONG_MIN_BIGINT = BigInteger.valueOf(Long.MIN_VALUE);
    private static final BigInteger LONG_MAX_BIGINT = BigInteger.valueOf(Long.MAX_VALUE);

    public static DynamicObject createBignum(RubyContext context, BigInteger value) {
        assert value.compareTo(LONG_MIN_BIGINT) < 0 || value.compareTo(LONG_MAX_BIGINT) > 0 : "Bignum in long range : " + value;
        return Layouts.BIGNUM.createBignum(context.getCoreLibrary().getBignumFactory(), value);
    }

}
