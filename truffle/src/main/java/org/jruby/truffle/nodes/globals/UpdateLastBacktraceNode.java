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
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;

public class UpdateLastBacktraceNode extends RubyNode {

    @Child private RubyNode child;
    @Child private CallDispatchHeadNode setBacktraceNode;

    public UpdateLastBacktraceNode(RubyContext context, SourceSection sourceSection, RubyNode child) {
        super(context, sourceSection);
        this.child = child;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        // TODO (nirvdrum 12-Mar-15) $! should be thread-local.
        final RubyBasicObject globals = getContext().getCoreLibrary().getGlobalVariablesObject();
        final Object lastException = globals.getOperations().getInstanceVariable(globals, "$!");

        if (lastException == getContext().getCoreLibrary().getNilObject()) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().argumentError("$! is not set", this));
        }

        if (setBacktraceNode == null) {
            CompilerDirectives.transferToInterpreter();
            setBacktraceNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
        }

        final Object newBacktrace = child.execute(frame);
        setBacktraceNode.call(frame, lastException, "set_backtrace", null, newBacktrace);

        return newBacktrace;
    }
}
