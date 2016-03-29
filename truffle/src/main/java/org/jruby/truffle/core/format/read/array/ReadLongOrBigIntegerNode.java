/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.format.read.array;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.format.FormatGuards;
import org.jruby.truffle.core.format.FormatNode;
import org.jruby.truffle.core.format.read.SourceNode;
import org.jruby.truffle.core.format.convert.ToLongNode;
import org.jruby.truffle.core.format.convert.ToLongNodeGen;

import java.math.BigInteger;

/**
 * Read a {@code long} value from the source, or a {@link BigInteger} if the
 * value is that large. This is only used with BER - in all other cases
 * we would truncate a {@code Bignum}.
 */
@NodeChildren({
        @NodeChild(value = "source", type = SourceNode.class),
})
public abstract class ReadLongOrBigIntegerNode extends FormatNode {

    @Child private ToLongNode toLongNode;

    private final ConditionProfile bignumProfile = ConditionProfile.createBinaryProfile();

    public ReadLongOrBigIntegerNode(RubyContext context) {
        super(context);
    }

    @Specialization(guards = "isNull(source)")
    public void read(VirtualFrame frame, Object source) {
        CompilerDirectives.transferToInterpreter();

        // Advance will handle the error
        advanceSourcePosition(frame);

        throw new IllegalStateException();
    }

    @Specialization
    public int read(VirtualFrame frame, int[] source) {
        return source[advanceSourcePosition(frame)];
    }

    @Specialization
    public long read(VirtualFrame frame, long[] source) {
        return source[advanceSourcePosition(frame)];
    }

    @Specialization
    public Object read(VirtualFrame frame, Object[] source) {
        final Object value = source[advanceSourcePosition(frame)];

        if (bignumProfile.profile(FormatGuards.isRubyBignum(value))) {
            return value;
        } else {
            if (toLongNode == null) {
                CompilerDirectives.transferToInterpreter();
                toLongNode = insert(ToLongNodeGen.create(getContext(), false, null));
            }

            return toLongNode.executeToLong(frame, value);
        }
    }

}
