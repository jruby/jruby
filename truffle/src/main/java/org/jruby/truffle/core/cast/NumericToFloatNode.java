/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved. This
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
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.language.dispatch.MissingBehavior;
import org.jruby.truffle.language.objects.IsANode;
import org.jruby.truffle.language.objects.IsANodeGen;

/**
 * Casts a value into a Ruby Float (double).
 */
@NodeChild(value = "value", type = RubyNode.class)
public abstract class NumericToFloatNode extends RubyNode {

    @Child private IsANode isANode = IsANodeGen.create(null, null);
    @Child private CallDispatchHeadNode toFloatCallNode;

    private final String method;

    public NumericToFloatNode(String method) {
        this.method = method;
    }

    public abstract double executeDouble(VirtualFrame frame, DynamicObject value);

    private Object callToFloat(VirtualFrame frame, DynamicObject value) {
        if (toFloatCallNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toFloatCallNode = insert(DispatchHeadNodeFactory.createMethodCall(MissingBehavior.RETURN_MISSING));
        }
        return toFloatCallNode.call(frame, value, method);
    }

    @Specialization(guards = "isNumeric(frame, value)")
    protected double castNumeric(VirtualFrame frame, DynamicObject value,
            @Cached("create()") BranchProfile errorProfile) {
        final Object result = callToFloat(frame, value);

        if (result instanceof Double) {
            return (double) result;
        } else {
            errorProfile.enter();
            throw new RaiseException(coreExceptions().typeErrorCantConvertTo(value, "Float", method, result, this));
        }
    }

    @Fallback
    protected double fallback(Object value) {
        throw new RaiseException(coreExceptions().typeErrorCantConvertInto(value, "Float", this));
    }

    protected boolean isNumeric(VirtualFrame frame, Object value) {
        return isANode.executeIsA(value, coreLibrary().getNumericClass());
    }

}
