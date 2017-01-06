/*
 * Copyright (c) 2014, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.cast;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.core.kernel.KernelNodes;
import org.jruby.truffle.core.kernel.KernelNodesFactory;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;

@NodeChild(type = RubyNode.class)
public abstract class ToSNode extends RubyNode {

    @Child private CallDispatchHeadNode callToSNode = DispatchHeadNodeFactory.createMethodCall(true);
    @Child private KernelNodes.ToSNode kernelToSNode;

    protected DynamicObject kernelToS(Object object) {
        if (kernelToSNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            kernelToSNode = insert(KernelNodesFactory.ToSNodeFactory.create(null));
        }
        return kernelToSNode.executeToS(object);
    }

    @Specialization(guards = "isRubyString(string)")
    public DynamicObject toS(DynamicObject string) {
        return string;
    }

    @Specialization(guards = "!isRubyString(object)", rewriteOn = UnexpectedResultException.class)
    public DynamicObject toS(VirtualFrame frame, Object object) throws UnexpectedResultException {
        final Object value = callToSNode.call(frame, object, "to_s");

        if (RubyGuards.isRubyString(value)) {
            return (DynamicObject) value;
        }

        throw new UnexpectedResultException(value);
    }

    @Specialization(guards = "!isRubyString(object)")
    public DynamicObject toSFallback(VirtualFrame frame, Object object) {
        final Object value = callToSNode.call(frame, object, "to_s");

        if (RubyGuards.isRubyString(value)) {
            return (DynamicObject) value;
        } else {
            return kernelToS(object);
        }
    }

}
