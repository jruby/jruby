/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.pack.nodes.read;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.truffle.pack.nodes.PackNode;
import org.jruby.truffle.pack.nodes.SourceNode;
import org.jruby.truffle.pack.nodes.type.ToStringNode;
import org.jruby.truffle.pack.nodes.type.ToStringNodeGen;
import org.jruby.truffle.pack.nodes.write.NullNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.util.ByteList;

/**
 * Read a string from the source, converting if needed.
 */
@NodeChildren({
        @NodeChild(value = "source", type = SourceNode.class),
})
public abstract class ReadStringNode extends PackNode {

    private final RubyContext context;
    private final boolean convertNumbersToStrings;

    @Child private ToStringNode toStringNode;

    public ReadStringNode(RubyContext context, boolean convertNumbersToStrings) {
        this.context = context;
        this.convertNumbersToStrings = convertNumbersToStrings;
    }

    @Specialization(guards = "isNull(source)")
    public long read(VirtualFrame frame, Object source) {
        CompilerDirectives.transferToInterpreter();

        // Advance will handle the error
        advanceSourcePosition(frame);

        throw new IllegalStateException();
    }

    @Specialization
    public Object read(VirtualFrame frame, int[] source) {
        return readAndConvert(frame, source[advanceSourcePosition(frame)]);
    }

    @Specialization
    public Object read(VirtualFrame frame, long[] source) {
        return readAndConvert(frame, source[advanceSourcePosition(frame)]);
    }

    @Specialization
    public Object read(VirtualFrame frame, double[] source) {
        return readAndConvert(frame, source[advanceSourcePosition(frame)]);
    }

    @Specialization(guards = "!isIRubyArray(source)")
    public Object read(VirtualFrame frame, Object[] source) {
        return readAndConvert(frame, source[advanceSourcePosition(frame)]);
    }

    private Object readAndConvert(VirtualFrame frame, Object value) {
        if (toStringNode == null) {
            CompilerDirectives.transferToInterpreter();
            toStringNode = insert(ToStringNodeGen.create(context, convertNumbersToStrings, new NullNode()));
        }

        return toStringNode.executeToString(frame, value);
    }

    @Specialization
    public ByteList read(VirtualFrame frame, IRubyObject[] source) {
        return toString(source[advanceSourcePosition(frame)]);
    }

    @CompilerDirectives.TruffleBoundary
    private ByteList toString(IRubyObject object) {
        return object.convertToString().getByteList();
    }

}
