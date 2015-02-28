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
import org.jruby.truffle.runtime.core.RubyString;

@NodeChild(value = "child", type = RubyNode.class)
public abstract class ToStrNode extends RubyNode {

    @Child private CallDispatchHeadNode toStrNode;

    public ToStrNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
        toStrNode = DispatchHeadNodeFactory.createMethodCall(context);
    }

    public ToStrNode(ToStrNode prev) {
        super(prev);
        toStrNode = prev.toStrNode;
    }

    @Specialization
    public RubyString coerceRubyString(RubyString string) {
        return string;
    }

    @Specialization(guards = "!isRubyString(object)")
    public RubyString coerceObject(VirtualFrame frame, Object object) {
        notDesignedForCompilation();

        final Object coerced;

        try {
            coerced = toStrNode.call(frame, object, "to_str", null);
        } catch (RaiseException e) {
            if (e.getRubyException().getLogicalClass() == getContext().getCoreLibrary().getNoMethodErrorClass()) {
                CompilerDirectives.transferToInterpreter();

                throw new RaiseException(
                        getContext().getCoreLibrary().typeErrorNoImplicitConversion(object, "String", this));
            } else {
                throw e;
            }
        }

        if (coerced instanceof RubyString) {
            return (RubyString) coerced;
        } else {
            CompilerDirectives.transferToInterpreter();

            throw new RaiseException(
                    getContext().getCoreLibrary().typeErrorBadCoercion(object, "String", "to_str", coerced, this));
        }
    }

    @Override
    public abstract RubyString executeRubyString(VirtualFrame frame);

    public abstract RubyString executeRubyString(VirtualFrame frame, Object object);

    @Override
    public final Object execute(VirtualFrame frame) {
        return executeRubyString(frame);
    }
}
