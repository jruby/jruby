/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
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
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;


@NodeChild(value = "child", type = RubyNode.class)
public abstract class ToAryNode extends RubyNode {

    @Child private CallDispatchHeadNode toAryNode;

    public ToAryNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    @Specialization(guards = "isRubyArray(array)")
    public RubyBasicObject coerceRubyArray(RubyBasicObject array) {
        return array;
    }

    @Specialization(guards = "!isRubyArray(object)")
    public RubyBasicObject coerceObject(VirtualFrame frame, Object object) {
        if (toAryNode == null) {
            CompilerDirectives.transferToInterpreter();
            toAryNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
        }

        final Object coerced;

        try {
            coerced = toAryNode.call(frame, object, "to_ary", null);
        } catch (RaiseException e) {
            if (e.getRubyException().getLogicalClass() == getContext().getCoreLibrary().getNoMethodErrorClass()) {
                CompilerDirectives.transferToInterpreter();

                throw new RaiseException(
                    getContext().getCoreLibrary().typeErrorNoImplicitConversion(object, "Array", this));
            } else {
                throw e;
            }
        }
        if (RubyGuards.isRubyArray(coerced)) {
            return (RubyBasicObject) coerced;
        } else {
            CompilerDirectives.transferToInterpreter();

            throw new RaiseException(
                getContext().getCoreLibrary().typeErrorBadCoercion(object, "Array", "to_ary", coerced, this));
        }
    }
}
