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
import org.jruby.truffle.nodes.core.FloatNodes;
import org.jruby.truffle.nodes.core.FloatNodesFactory;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBignum;

@NodeChild(value = "child", type = RubyNode.class)
public abstract class ToIntNode extends RubyNode {

    @Child private CallDispatchHeadNode toIntNode;
    @Child private FloatNodes.ToINode floatToIntNode;

    public ToIntNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public ToIntNode(ToIntNode prev) {
        super(prev);
    }

    @Specialization
    public int coerceInt(int value) {
        return value;
    }

    @Specialization
    public long coerceLong(long value) {
        return value;
    }

    @Specialization
    public RubyBignum coerceRubyBignum(RubyBignum value) {
        return value;
    }

    @Specialization
    public Object coerceDouble(double value) {
        if (floatToIntNode == null) {
            CompilerDirectives.transferToInterpreter();
            floatToIntNode = insert(FloatNodesFactory.ToINodeFactory.create(getContext(), getSourceSection(), new RubyNode[]{}));
        }

        return floatToIntNode.toI(value);
    }

    @Specialization(guards = "!isRubyBignum")
    public Object coerceObject(VirtualFrame frame, Object object) {
        notDesignedForCompilation("2afba555165e4881bfcf6f04dd37bd01");

        if (toIntNode == null) {
            CompilerDirectives.transferToInterpreter();
            toIntNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
        }

        final Object coerced;

        try {
            coerced = toIntNode.call(frame, object, "to_int", null);
        } catch (RaiseException e) {
            if (e.getRubyException().getLogicalClass() == getContext().getCoreLibrary().getNoMethodErrorClass()) {
                CompilerDirectives.transferToInterpreter();

                throw new RaiseException(
                        getContext().getCoreLibrary().typeErrorNoImplicitConversion(object, "Integer", this));
            } else {
                throw e;
            }
        }

        if (getContext().getCoreLibrary().getLogicalClass(coerced) == getContext().getCoreLibrary().getFixnumClass()) {
            return coerced;
        } else {
            CompilerDirectives.transferToInterpreter();

            throw new RaiseException(
                    getContext().getCoreLibrary().typeErrorBadCoercion(object, "Integer", "to_int", coerced, this));
        }
    }

    @Override
    public abstract int executeIntegerFixnum(VirtualFrame frame);

    public abstract int executeIntegerFixnum(VirtualFrame frame, Object object);

    @Override
    public abstract long executeLongFixnum(VirtualFrame frame);

    @Override
    public abstract RubyBignum executeBignum(VirtualFrame frame);
}
