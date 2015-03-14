/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.nodes.globals;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.ThreadLocalObjectNode;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.nodes.objects.ReadInstanceVariableNode;
import org.jruby.truffle.runtime.RubyContext;

public class ReadLastBacktraceNode extends RubyNode {

    @Child private ReadInstanceVariableNode getLastExceptionNode;
    @Child private CallDispatchHeadNode getBacktraceNode;

    public ReadLastBacktraceNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
        getLastExceptionNode = new ReadInstanceVariableNode(getContext(), getSourceSection(), "$!",
                new ThreadLocalObjectNode(getContext(), getSourceSection()),
                true);
        getBacktraceNode = DispatchHeadNodeFactory.createMethodCall(getContext());
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        return getContext().makeString("global-variable");
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object lastException = getLastExceptionNode.execute(frame);

        if (lastException == nil()) {
            return nil();
        }

        return getBacktraceNode.call(frame, lastException, "backtrace", null);
    }
}
