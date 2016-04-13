/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
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
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;

public class ReadLastBacktraceNode extends RubyNode {

    @Child private ReadThreadLocalGlobalVariableNode getLastExceptionNode;
    @Child private CallDispatchHeadNode getBacktraceNode;

    private final ConditionProfile lastExceptionNilProfile = ConditionProfile.createBinaryProfile();

    public ReadLastBacktraceNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object lastException = getGetLastExceptionNode().execute(frame);

        if (lastExceptionNilProfile.profile(lastException == nil())) {
            return nil();
        }

        return getGetBacktraceNode().call(frame, lastException, "backtrace", null);
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        return coreStrings().GLOBAL_VARIABLE.createInstance();
    }

    private ReadThreadLocalGlobalVariableNode getGetLastExceptionNode() {
        if (getLastExceptionNode == null) {
            CompilerDirectives.transferToInterpreter();
            getLastExceptionNode = insert(new ReadThreadLocalGlobalVariableNode(getContext(), getSourceSection(), "$!", true));
        }

        return getLastExceptionNode;
    }

    private CallDispatchHeadNode getGetBacktraceNode() {
        if (getBacktraceNode == null) {
            CompilerDirectives.transferToInterpreter();
            getBacktraceNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
        }

        return getBacktraceNode;
    }

}
