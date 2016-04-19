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
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.format.FormatNode;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;

@NodeChildren({
        @NodeChild(value = "value", type = FormatNode.class),
})
public abstract class ToIntegerNode extends FormatNode {

    @Child private CallDispatchHeadNode integerNode;

    public ToIntegerNode(RubyContext context) {
        super(context);
    }

    public abstract Object executeToInteger(VirtualFrame frame, Object object);

    @Specialization
    public int toInteger(int value) {
        return value;
    }

    @Specialization
    public long toInteger(long value) {
        return value;
    }

    @Specialization(guards = "isRubyBignum(value)")
    public DynamicObject toInteger(DynamicObject value) {
        return value;
    }

    @Specialization
    public long toInteger(double value) {
        return (long) value;
    }

    @Specialization(guards = {
            "!isInteger(value)",
            "!isLong(value)",
            "!isRubyBignum(value)"})
    public Object toInteger(VirtualFrame frame, Object value) {
        if (integerNode == null) {
            CompilerDirectives.transferToInterpreter();
            integerNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext(), true));
        }

        return integerNode.call(frame, getContext().getCoreLibrary().getKernelModule(), "Integer", null, value);
    }

}
