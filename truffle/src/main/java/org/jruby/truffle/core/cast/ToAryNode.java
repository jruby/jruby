/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.cast;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;


@NodeChild(value = "child", type = RubyNode.class)
public abstract class ToAryNode extends RubyNode {

    @Child private CallDispatchHeadNode toAryNode;

    @Specialization(guards = "isRubyArray(array)")
    public DynamicObject coerceRubyArray(DynamicObject array) {
        return array;
    }

    @Specialization(guards = "!isRubyArray(object)")
    public DynamicObject coerceObject(VirtualFrame frame, Object object,
            @Cached("create()") BranchProfile errorProfile) {
        if (toAryNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toAryNode = insert(DispatchHeadNodeFactory.createMethodCall());
        }

        final Object coerced;
        try {
            coerced = toAryNode.call(frame, object, "to_ary");
        } catch (RaiseException e) {
            errorProfile.enter();
            if (Layouts.BASIC_OBJECT.getLogicalClass(e.getException()) == coreLibrary().getNoMethodErrorClass()) {
                throw new RaiseException(coreExceptions().typeErrorNoImplicitConversion(object, "Array", this));
            } else {
                throw e;
            }
        }

        if (RubyGuards.isRubyArray(coerced)) {
            return (DynamicObject) coerced;
        } else {
            errorProfile.enter();
            throw new RaiseException(coreExceptions().typeErrorBadCoercion(object, "Array", "to_ary", coerced, this));
        }
    }
}
