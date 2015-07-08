/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.nodes.coerce;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;

@NodeChild(value = "child", type = RubyNode.class)
public abstract class ToFNode extends RubyNode {

    @Child private CallDispatchHeadNode toFNode;

    public ToFNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public double doDouble(VirtualFrame frame, Object value) {
        final Object doubleObject = executeDouble(frame, value);

        if (doubleObject instanceof Double) {
            return (double) doubleObject;
        }

        CompilerDirectives.transferToInterpreter();
        throw new UnsupportedOperationException("executeDouble must return a double, instead it returned a " + doubleObject.getClass().getName());
    }

    public abstract Object executeDouble(VirtualFrame frame, Object value);

    @Specialization
    public double coerceInt(int value) {
        return value;
    }

    @Specialization
    public double coerceLong(long value) {
        return value;
    }

    @Specialization
    public double coerceDouble(double value) {
        return value;
    }

    @Specialization
    public double coerceBoolean(VirtualFrame frame, boolean value) {
        return coerceObject(frame, value);
    }

    @Specialization
    public double coerceRubyBasicObject(VirtualFrame frame, RubyBasicObject object) {
        return coerceObject(frame, object);
    }

    private double coerceObject(VirtualFrame frame, Object object) {
        if (toFNode == null) {
            CompilerDirectives.transferToInterpreter();
            toFNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext(), true));
        }

        final Object coerced;

        try {
            coerced = toFNode.call(frame, object, "to_f", null);
        } catch (RaiseException e) {
            if (e.getRubyException().getLogicalClass() == getContext().getCoreLibrary().getNoMethodErrorClass()) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().typeErrorNoImplicitConversion(object, "Float", this));
            } else {
                throw e;
            }
        }

        if (getContext().getCoreLibrary().getLogicalClass(coerced) == getContext().getCoreLibrary().getFloatClass()) {
            return (double) coerced;
        } else {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().typeErrorBadCoercion(object, "Float", "to_f", coerced, this));
        }
    }

}
