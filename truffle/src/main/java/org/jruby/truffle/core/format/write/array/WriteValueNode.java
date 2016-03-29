/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.format.write.array;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.array.ArrayUtils;
import org.jruby.truffle.core.format.FormatNode;
import org.jruby.truffle.core.format.MissingValue;
import org.jruby.truffle.core.format.write.OutputNode;

@NodeChildren({
        @NodeChild(value = "output", type = OutputNode.class),
        @NodeChild(value = "value", type = Node.class)
})
public abstract class WriteValueNode extends FormatNode {

    public WriteValueNode(RubyContext context) {
        super(context);
    }

    @Specialization
    public Object doWrite(Object output, MissingValue value) {
        return null;
    }

    @Specialization(guards = "!isMissingValue(value)")
    public Object doWrite(VirtualFrame frame, Object[] output, Object value) {
        final Object[] outputWithEnoughSize = ensureCapacity(frame, output, 1);
        final int outputPosition = getOutputPosition(frame);
        outputWithEnoughSize[outputPosition] = value;
        setOutputPosition(frame, outputPosition + 1);
        return null;
    }

    private Object[] ensureCapacity(VirtualFrame frame, Object[] output, int length) {
        final int outputPosition = getOutputPosition(frame);
        final int neededLength = outputPosition + length;

        if (neededLength <= output.length) {
            return output;
        }

        CompilerDirectives.transferToInterpreter();

        final Object[] newOutput = new Object[ArrayUtils.capacity(getContext(), output.length, neededLength)];
        System.arraycopy(output, 0, newOutput, 0, outputPosition);
        setOutput(frame, newOutput);

        return newOutput;
    }

}
