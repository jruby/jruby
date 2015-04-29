/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.cast;

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.KernelNodes;
import org.jruby.truffle.nodes.core.KernelNodesFactory;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.nodes.dispatch.MissingBehavior;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

/**
 * Casts a value into a Ruby Float (double).
 */
@NodeChild(value = "value", type = RubyNode.class)
public abstract class NumericToFloatNode extends RubyNode {

    @Child private KernelNodes.IsANode isANode;
    @Child CallDispatchHeadNode toFloatCallNode;

    private final String method;

    public NumericToFloatNode(RubyContext context, SourceSection sourceSection, String method) {
        super(context, sourceSection);
        isANode = KernelNodesFactory.IsANodeFactory.create(context, sourceSection, new RubyNode[] { null, null });
        this.method = method;
    }

    public abstract double executeFloat(VirtualFrame frame, RubyBasicObject value);

    private Object callToFloat(VirtualFrame frame, RubyBasicObject value) {
        if (toFloatCallNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toFloatCallNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext(), MissingBehavior.RETURN_MISSING));
        }
        return toFloatCallNode.call(frame, value, method, null);
    }

    @Specialization(guards = "isNumeric(frame, value)")
    protected double castNumeric(VirtualFrame frame, RubyBasicObject value) {
        final Object result = callToFloat(frame, value);

        if (result instanceof Double) {
            return (double) result;
        } else {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().typeErrorCantConvertTo(
                    value, getContext().getCoreLibrary().getFloatClass(), method, result, this));
        }
    }

    @Fallback
    protected double fallback(Object value) {
        CompilerDirectives.transferToInterpreter();
        throw new RaiseException(getContext().getCoreLibrary().typeErrorCantConvertInto(
                value, getContext().getCoreLibrary().getFloatClass(), this));
    }

    protected boolean isNumeric(VirtualFrame frame, Object value) {
        return isANode.executeIsA(frame, value, getContext().getCoreLibrary().getNumericClass());
    }

}
