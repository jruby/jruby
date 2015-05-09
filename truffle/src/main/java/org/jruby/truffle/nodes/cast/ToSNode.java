/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.cast;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.KernelNodes;
import org.jruby.truffle.nodes.core.KernelNodesFactory;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyString;

@NodeChild(type = RubyNode.class)
public abstract class ToSNode extends RubyNode {

    @Child private CallDispatchHeadNode callToSNode;
    @Child private KernelNodes.ToSNode kernelToSNode;

    public ToSNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
        callToSNode = DispatchHeadNodeFactory.createMethodCall(context, true);
    }

    protected RubyString kernelToS(VirtualFrame frame, Object object) {
        if (kernelToSNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            kernelToSNode = insert(KernelNodesFactory.ToSNodeFactory.create(getContext(), getSourceSection(), new RubyNode[] {null}));
        }
        return kernelToSNode.executeToS(frame, object);
    }

    @Override
    public abstract RubyString executeRubyString(VirtualFrame frame);

    @Specialization
    public RubyString toS(RubyString string) {
        return string;
    }

    @Specialization(guards = "!isRubyString(object)", rewriteOn = UnexpectedResultException.class)
    public RubyString toS(VirtualFrame frame, Object object) throws UnexpectedResultException {
        final Object value = callToSNode.call(frame, object, "to_s", null);

        if (value instanceof RubyString) {
            return (RubyString) value;
        }

        throw new UnexpectedResultException(value);
    }

    @Specialization(guards = "!isRubyString(object)")
    public RubyString toSFallback(VirtualFrame frame, Object object) {
        final Object value = callToSNode.call(frame, object, "to_s", null);

        if (value instanceof RubyString) {
            return (RubyString) value;
        } else {
            return kernelToS(frame, object);
        }
    }

}
