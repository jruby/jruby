/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.RubyContext;

import java.math.BigInteger;

public class RubyBignum extends RubyBasicObject {

    private static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);
    private static final BigInteger LONG_MIN = BigInteger.valueOf(Long.MIN_VALUE);

    private final BigInteger value;

    public RubyBignum(RubyClass rubyClass, BigInteger value) {
        super(rubyClass);
        assert value.compareTo(LONG_MIN) < 0 || value.compareTo(LONG_MAX) > 0 : String.format("%s not in Bignum range", value);
        this.value = value;
    }

    @Override
    public boolean hasNoSingleton() {
        return true;
    }

    public BigInteger bigIntegerValue() {
        return value;
    }

    public static class BignumAllocator implements Allocator {

        @Override
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, Node currentNode) {
            return new RubyBignum(rubyClass, BigInteger.ZERO);
        }

    }

}
