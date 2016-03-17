/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.interop;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.dispatch.DispatchAction;
import org.jruby.truffle.language.dispatch.DispatchHeadNode;
import org.jruby.truffle.language.dispatch.MissingBehavior;

class ResolvedInteropExecuteAfterReadNode extends InteropNode {

    @Child private DispatchHeadNode head;
    @Child private InteropArgumentsNode arguments;
    private final String name;
    private final int labelIndex;
    private final int receiverIndex;

    public ResolvedInteropExecuteAfterReadNode(RubyContext context, SourceSection sourceSection, String name, int arity) {
        super(context, sourceSection);
        this.name = name;
        this.head = new DispatchHeadNode(context, true, MissingBehavior.CALL_METHOD_MISSING, DispatchAction.CALL_METHOD);
        this.arguments = new InteropArgumentsNode(context, sourceSection, arity); // [0] is receiver, [1] is the label
        this.labelIndex = 1;
        this.receiverIndex = 0;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        if (name.equals(frame.getArguments()[labelIndex])) {
            Object[] args = new Object[arguments.getCount(frame)];
            arguments.executeFillObjectArray(frame, args);
            return head.dispatch(frame, frame.getArguments()[receiverIndex], frame.getArguments()[labelIndex], null, args);
        } else {
            CompilerDirectives.transferToInterpreter();
            throw new IllegalStateException("Name changed");
        }
    }
}
