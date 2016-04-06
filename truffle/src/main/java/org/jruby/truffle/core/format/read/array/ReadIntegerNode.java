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
import org.jruby.truffle.core.format.FormatNode;
import org.jruby.truffle.core.format.convert.ToIntegerNode;
import org.jruby.truffle.core.format.convert.ToIntegerNodeGen;
import org.jruby.truffle.core.format.read.SourceNode;

@NodeChildren({
        @NodeChild(value = "source", type = SourceNode.class),
})
public abstract class ReadIntegerNode extends FormatNode {

    @Child private ToIntegerNode toIntegerNode;

    private final ConditionProfile convertedTypeProfile = ConditionProfile.createBinaryProfile();

    public ReadIntegerNode(RubyContext context) {
        super(context);
    }

    @Specialization(guards = "isNull(source)")
    public double read(VirtualFrame frame, Object source) {
        advanceSourcePosition(frame);
        throw new IllegalStateException();
    }

    @Specialization
    public int read(VirtualFrame frame, int[] source) {
        return source[advanceSourcePosition(frame)];
    }

    @Specialization
    public int read(VirtualFrame frame, long[] source) {
        return (int) source[advanceSourcePosition(frame)];
    }

    @Specialization
    public int read(VirtualFrame frame, double[] source) {
        return (int) source[advanceSourcePosition(frame)];
    }

    @Specialization
    public int read(VirtualFrame frame, Object[] source) {
        if (toIntegerNode == null) {
            CompilerDirectives.transferToInterpreter();
            toIntegerNode = insert(ToIntegerNodeGen.create(getContext(), null));
        }

        final Object value = toIntegerNode.executeToInteger(frame, source[advanceSourcePosition(frame)]);

        if (convertedTypeProfile.profile(value instanceof Long)) {
            return (int) (long) value;
        } else {
            return (int) value;
        }
    }

}
