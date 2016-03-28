/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.format.unpack;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.array.ArrayUtils;
import org.jruby.truffle.core.format.FormatNode;
import org.jruby.truffle.core.format.MissingValue;

import java.util.Arrays;

@NodeChildren({
        @NodeChild(value = "value", type = FormatNode.class),
})
public abstract class WriteValueNode extends FormatNode {

    public WriteValueNode(RubyContext context) {
        super(context);
    }

    @Specialization
    public Object doWrite(MissingValue value) {
        return null;
    }

    @Specialization(guards = "!isMissingValue(value)")
    public Object doWrite(VirtualFrame frame, Object value) {
        final Object[] output = ensureCapacity(frame, 1);
        final int outputPosition = getOutputPosition(frame);
        output[outputPosition] = value;
        setOutputPosition(frame, outputPosition + 1);
        return null;
    }

    private Object[] ensureCapacity(VirtualFrame frame, int length) {
        final Object[] output = (Object[]) getOutput(frame);
        final int outputPosition = getOutputPosition(frame);
        final int neededLength = outputPosition + length;

        if (neededLength <= output.length) {
            return output;
        }

        CompilerDirectives.transferToInterpreterAndInvalidate();
        final Object[] newOutput = Arrays.copyOf(output, ArrayUtils.capacity(getContext(), output.length, neededLength));
        setOutput(frame, newOutput);
        return newOutput;
    }

}
