/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.numeric;

import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;

import java.math.BigInteger;

public class BignumOperations {

    private static final BigInteger LONG_MIN_BIGINT = BigInteger.valueOf(Long.MIN_VALUE);
    private static final BigInteger LONG_MAX_BIGINT = BigInteger.valueOf(Long.MAX_VALUE);

    public static DynamicObject createBignum(RubyContext context, BigInteger value) {
        assert value.compareTo(LONG_MIN_BIGINT) < 0 || value.compareTo(LONG_MAX_BIGINT) > 0 : "Bignum in long range : " + value;
        return Layouts.BIGNUM.createBignum(context.getCoreLibrary().getBignumFactory(), value);
    }

}
