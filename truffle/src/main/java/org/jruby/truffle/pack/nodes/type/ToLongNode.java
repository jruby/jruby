/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.pack.nodes.type;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.nodes.core.BignumNodes;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.nodes.dispatch.DispatchNode;
import org.jruby.truffle.nodes.dispatch.MissingBehavior;
import org.jruby.truffle.pack.nodes.PackNode;
import org.jruby.truffle.pack.runtime.exceptions.CantConvertException;
import org.jruby.truffle.pack.runtime.exceptions.NoImplicitConversionException;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyBignum;

/**
 * Convert a value to a {@code long}.
 */
@NodeChildren({
        @NodeChild(value = "value", type = PackNode.class),
})
public abstract class ToLongNode extends PackNode {

    @Child private CallDispatchHeadNode toIntNode;

    @CompilerDirectives.CompilationFinal private boolean seenInt;
    @CompilerDirectives.CompilationFinal private boolean seenLong;
    @CompilerDirectives.CompilationFinal private boolean seenBignum;

    public ToLongNode(RubyContext context) {
        super(context);
    }

    public abstract long executeToLong(VirtualFrame frame, Object object);

    @Specialization
    public long toLong(VirtualFrame frame, boolean object) {
        CompilerDirectives.transferToInterpreter();
        throw new NoImplicitConversionException(object, "Integer");
    }

    @Specialization
    public long toLong(VirtualFrame frame, int object) {
        return object;
    }

    @Specialization
    public long toLong(VirtualFrame frame, long object) {
        return object;
    }

    @Specialization(guards = "isRubyBignum(object)")
    public long toLong(VirtualFrame frame, RubyBasicObject object) {
        // A truncated value is exactly what we want
        return BignumNodes.getBigIntegerValue(object).longValue();
    }

    @Specialization(guards = "isNil(nil)")
    public long toLongNil(VirtualFrame frame, Object nil) {
        CompilerDirectives.transferToInterpreter();
        throw new NoImplicitConversionException(nil, "Integer");
    }

    @Specialization(guards = {"!isBoolean(object)", "!isInteger(object)", "!isLong(object)", "!isBigInteger(object)", "!isRubyBignum(object)", "!isNil(object)"})
    public long toLong(VirtualFrame frame, Object object) {
        if (toIntNode == null) {
            CompilerDirectives.transferToInterpreter();
            toIntNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext(), true, MissingBehavior.RETURN_MISSING));
        }

        final Object value = toIntNode.call(frame, object, "to_int", null);

        if (seenInt && value instanceof Integer) {
            return toLong(frame, (int) value);
        }

        if (seenLong && value instanceof Long) {
            return toLong(frame, (long) value);
        }

        if (seenBignum && value instanceof RubyBignum) {
            return toLong(frame, (RubyBignum) value);
        }

        CompilerDirectives.transferToInterpreterAndInvalidate();

        if (value == DispatchNode.MISSING) {
            throw new NoImplicitConversionException(object, "Integer");
        }

        if (value instanceof Integer) {
            seenInt = true;
            return toLong(frame, (int) value);
        }

        if (value instanceof Long) {
            seenLong = true;
            return toLong(frame, (long) value);
        }

        if (value instanceof RubyBignum) {
            seenBignum = true;
            return toLong(frame, (RubyBignum) value);
        }

        // TODO CS 5-April-15 missing the (Object#to_int gives String) part

        throw new CantConvertException("can't convert Object to Integer");
    }

}
