/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.yield;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.DirectCallNode;
import org.jruby.common.IRubyWarnings;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.*;

/**
 * Dispatch to a known method which has been inlined.
 */
public class CachedYieldDispatchNode extends YieldDispatchNode {

    private final RubyProc block;

    @Child protected DirectCallNode callNode;

    public CachedYieldDispatchNode(RubyContext context, SourceSection sourceSection, RubyProc block) {
        super(context, sourceSection);
        this.block = block;

        callNode = Truffle.getRuntime().createDirectCallNode(block.getMethod().getCallTarget());
    }

    @Override
    public Object dispatch(VirtualFrame frame, RubyProc block, Object[] argumentsObjects) {
        if (block.getMethod().getCallTarget() != callNode.getCallTarget()) {
            CompilerDirectives.transferToInterpreter();

            // TODO(CS): at the moment we just go back to uninit, which may cause loops

            getContext().getRuntime().getWarnings().warn(IRubyWarnings.ID.TRUFFLE, getSourceSection().getSource().getName(), getSourceSection().getStartLine(), "uninitialized yield - may run in a loop");

            final UninitializedYieldDispatchNode dispatch = new UninitializedYieldDispatchNode(getContext(), getSourceSection());
            replace(dispatch);
            return dispatch.dispatch(frame, block, argumentsObjects);
        }

        return callNode.call(frame, RubyArguments.create(block.getMethod().getDeclarationFrame(), block.getSelfCapturedInScope(), block.getBlockCapturedInScope(), argumentsObjects));
    }
}
