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
import org.jruby.truffle.core.format.LiteralFormatNode;
import org.jruby.truffle.core.format.convert.ToStringNode;
import org.jruby.truffle.core.format.convert.ToStringNodeGen;
import org.jruby.truffle.core.format.read.SourceNode;
import org.jruby.truffle.core.format.write.bytes.WriteByteNodeGen;

@NodeChildren({
        @NodeChild(value = "source", type = SourceNode.class),
})
public abstract class ReadStringNode extends FormatNode {

    private final boolean convertNumbersToStrings;
    private final String conversionMethod;
    private final boolean inspectOnConversionFailure;
    private final Object valueOnNil;

    @Child private ToStringNode toStringNode;

    public ReadStringNode(RubyContext context, boolean convertNumbersToStrings,
                          String conversionMethod, boolean inspectOnConversionFailure,
                          Object valueOnNil) {
        super(context);
        this.convertNumbersToStrings = convertNumbersToStrings;
        this.conversionMethod = conversionMethod;
        this.inspectOnConversionFailure = inspectOnConversionFailure;
        this.valueOnNil = valueOnNil;
    }

    @Specialization(guards = "isNull(source)")
    public Object read(VirtualFrame frame, Object source) {
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

    @Specialization
    public Object read(VirtualFrame frame, Object[] source) {
        return readAndConvert(frame, source[advanceSourcePosition(frame)]);
    }

    private Object readAndConvert(VirtualFrame frame, Object value) {
        if (toStringNode == null) {
            CompilerDirectives.transferToInterpreter();
            toStringNode = insert(ToStringNodeGen.create(getContext(),
                    convertNumbersToStrings,
                    conversionMethod,
                    inspectOnConversionFailure,
                    valueOnNil,
                    WriteByteNodeGen.create(getContext(), new LiteralFormatNode(getContext(), (byte) 0))));
        }

        return toStringNode.executeToString(frame, value);
    }

}
