/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.language.globals;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;

public class UpdateLastBacktraceNode extends RubyNode {

    @Child private RubyNode child;
    @Child private ReadThreadLocalGlobalVariableNode getLastExceptionNode;
    @Child private CallDispatchHeadNode setBacktraceNode;

    private final BranchProfile lastExceptionNilProfile = BranchProfile.create();

    public UpdateLastBacktraceNode(RubyNode child) {
        this.child = child;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object lastException = getGetLastExceptionNode().execute(frame);

        if (lastException == nil()) {
            lastExceptionNilProfile.enter();
            throw new RaiseException(coreExceptions().argumentError("$! is not set", this));
        }

        final Object newBacktrace = child.execute(frame);

        getSetBacktraceNode().call(frame, lastException, "set_backtrace", newBacktrace);

        return newBacktrace;
    }

    private ReadThreadLocalGlobalVariableNode getGetLastExceptionNode() {
        if (getLastExceptionNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getLastExceptionNode = insert(new ReadThreadLocalGlobalVariableNode("$!", true));
        }

        return getLastExceptionNode;
    }

    private CallDispatchHeadNode getSetBacktraceNode() {
        if (setBacktraceNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            setBacktraceNode = insert(DispatchHeadNodeFactory.createMethodCall());
        }

        return setBacktraceNode;
    }

}
