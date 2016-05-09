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
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.format.FormatNode;
import org.jruby.truffle.core.format.convert.ToDoubleNode;
import org.jruby.truffle.core.format.convert.ToDoubleNodeGen;
import org.jruby.truffle.core.format.read.SourceNode;

@NodeChildren({
        @NodeChild(value = "source", type = SourceNode.class),
})
public abstract class ReadDoubleNode extends FormatNode {

    @Child private ToDoubleNode toDoubleNode;

    public ReadDoubleNode(RubyContext context) {
        super(context);
    }

    @Specialization(guards = "isNull(source)")
    public double read(VirtualFrame frame, Object source) {
        advanceSourcePosition(frame);
        throw new IllegalStateException();
    }

    @Specialization
    public double read(VirtualFrame frame, int[] source) {
        return source[advanceSourcePosition(frame)];
    }

    @Specialization
    public double read(VirtualFrame frame, long[] source) {
        return source[advanceSourcePosition(frame)];
    }

    @Specialization
    public double read(VirtualFrame frame, double[] source) {
        return source[advanceSourcePosition(frame)];
    }

    @Specialization
    public double read(VirtualFrame frame, Object[] source) {
        if (toDoubleNode == null) {
            CompilerDirectives.transferToInterpreter();
            toDoubleNode = insert(ToDoubleNodeGen.create(getContext(), null));
        }

        return toDoubleNode.executeToDouble(frame, source[advanceSourcePosition(frame)]);
    }

}
