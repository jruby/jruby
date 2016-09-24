/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.format.convert;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.format.FormatNode;
import org.jruby.truffle.core.format.MissingValue;
import org.jruby.truffle.core.numeric.FixnumOrBignumNode;

import java.math.BigInteger;

@NodeChildren({
        @NodeChild(value = "value", type = FormatNode.class),
})
public abstract class ReinterpretAsUnsignedNode extends FormatNode {

    @Child private FixnumOrBignumNode fixnumOrBignumNode;

    public ReinterpretAsUnsignedNode(RubyContext context) {
        super(context);
    }

    @Specialization
    public MissingValue asUnsigned(MissingValue missingValue) {
        return missingValue;
    }

    @Specialization(guards = "isNil(nil)")
    public DynamicObject asUnsigned(DynamicObject nil) {
        return nil;
    }

    @Specialization
    public int asUnsigned(short value) {
        return value & 0xffff;
    }

    @Specialization
    public long asUnsigned(int value) {
        return value & 0xffffffffL;
    }

    @Specialization
    public Object asUnsigned(long value) {
        if (fixnumOrBignumNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            fixnumOrBignumNode = insert(FixnumOrBignumNode.create(getContext(), null));
        }

        return fixnumOrBignumNode.fixnumOrBignum(asUnsignedBigInteger(value));
    }

    private static final long UNSIGNED_LONG_MASK = 0x7fffffffffffffffL;

    @CompilerDirectives.TruffleBoundary
    private BigInteger asUnsignedBigInteger(long value) {
        // TODO CS 28-Mar-16 can't we work out if it would fit into a long, and not create a BigInteger?

        BigInteger bigIntegerValue = BigInteger.valueOf(value & UNSIGNED_LONG_MASK);

        if (value < 0) {
            bigIntegerValue = bigIntegerValue.setBit(Long.SIZE - 1);
        }

        return bigIntegerValue;
    }

}
