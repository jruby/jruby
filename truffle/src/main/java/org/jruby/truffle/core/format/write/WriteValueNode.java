/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.format.write;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.RubyContext;
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
    public Object doWrite(VirtualFrame frame, MissingValue value) {
        return null;
    }

    @Specialization(guards = "!isMissingValue(value)")
    public Object doWrite(VirtualFrame frame, Object value) {
        Object[] output = ensureCapacity(frame, 1);
        final int outputPosition = getOutputPosition(frame);
        output[outputPosition] = value;
        setOutputPosition(frame, outputPosition + 1);
        return null;
    }

    private Object[] ensureCapacity(VirtualFrame frame, int length) {
        Object[] output = (Object[]) getOutput(frame);
        final int outputPosition = getOutputPosition(frame);

        if (outputPosition + length > output.length) {
            // If we ran out of output byte[], deoptimize and next time we'll allocate more

            CompilerDirectives.transferToInterpreterAndInvalidate();
            output = Arrays.copyOf(output, (output.length + length) * 2);
            setOutput(frame, output);
        }

        return output;
    }

    protected boolean isMissingValue(Object object) {
        return object instanceof MissingValue;
    }

}
