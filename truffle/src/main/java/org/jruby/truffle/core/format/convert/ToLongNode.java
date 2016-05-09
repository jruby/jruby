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
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.format.FormatNode;
import org.jruby.truffle.core.format.exceptions.CantConvertException;
import org.jruby.truffle.core.format.exceptions.NoImplicitConversionException;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.language.dispatch.MissingBehavior;

@NodeChildren({
        @NodeChild(value = "value", type = FormatNode.class),
})
public abstract class ToLongNode extends FormatNode {

    private final boolean errorIfNeedsConversion;

    @Child private CallDispatchHeadNode toIntNode;
    @Child private ToLongNode redoNode;

    public ToLongNode(RubyContext context, boolean errorIfNeedsConversion) {
        super(context);
        this.errorIfNeedsConversion = errorIfNeedsConversion;
    }

    public abstract long executeToLong(VirtualFrame frame, Object object);

    @Specialization
    public long toLong(boolean object) {
        throw new NoImplicitConversionException(object, "Integer");
    }

    @Specialization
    public long toLong(int object) {
        return object;
    }

    @Specialization
    public long toLong(long object) {
        return object;
    }

    @Specialization(guards = "isRubyBignum(object)")
    public long toLong(DynamicObject object) {
        // A truncated value is exactly what we want
        return Layouts.BIGNUM.getValue(object).longValue();
    }

    @Specialization(guards = "isNil(nil)")
    public long toLongNil(Object nil) {
        throw new NoImplicitConversionException(nil, "Integer");
    }

    @Specialization(guards = {
            "!isBoolean(object)",
            "!isInteger(object)",
            "!isLong(object)",
            "!isBigInteger(object)",
            "!isRubyBignum(object)",
            "!isNil(object)"})
    public long toLong(VirtualFrame frame, Object object) {
        if (errorIfNeedsConversion) {
            throw new CantConvertException("can't convert Object to Integer");
        }

        if (toIntNode == null) {
            CompilerDirectives.transferToInterpreter();
            toIntNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext(), true, MissingBehavior.RETURN_MISSING));
        }

        final Object value = toIntNode.call(frame, object, "to_int", null);

        if (redoNode == null) {
            CompilerDirectives.transferToInterpreter();
            redoNode = insert(ToLongNodeGen.create(getContext(), true, null));
        }

        return redoNode.executeToLong(frame, value);
    }

}
