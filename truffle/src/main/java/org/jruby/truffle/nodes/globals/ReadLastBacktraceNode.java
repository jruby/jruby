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
import org.jruby.truffle.runtime.core.RubyBasicObject;

public class ReadLastBacktraceNode extends RubyNode {

    @Child private CallDispatchHeadNode getBacktraceNode;

    public ReadLastBacktraceNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        // TODO (nirvdrum 12-Mar-15) $! should be thread-local.
        final RubyBasicObject globals = getContext().getCoreLibrary().getGlobalVariablesObject();
        final Object lastException = globals.getOperations().getInstanceVariable(globals, "$!");

        if (lastException == getContext().getCoreLibrary().getNilObject()) {
            return getContext().getCoreLibrary().getNilObject();
        }

        if (getBacktraceNode == null) {
            CompilerDirectives.transferToInterpreter();
            getBacktraceNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
        }

        return getBacktraceNode.call(frame, lastException, "backtrace", null);
    }
}
