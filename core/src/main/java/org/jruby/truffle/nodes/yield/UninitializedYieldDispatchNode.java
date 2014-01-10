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
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.nodes.call.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.methods.*;

/**
 * An uninitialized node in the yield dispatch chain.
 */
public class UninitializedYieldDispatchNode extends YieldDispatchNode {

    public UninitializedYieldDispatchNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    @Override
    public Object dispatch(VirtualFrame frame, RubyProc block, Object[] argumentsObjects) {
        CompilerDirectives.transferToInterpreter();

        final MethodImplementation implementation = block.getMethod().getImplementation();

        if (implementation instanceof InlinableMethodImplementation && InlineHeuristic.shouldInlineYield((InlinableMethodImplementation) implementation)) {
            final InlinedYieldDispatchNode dispatch = new InlinedYieldDispatchNode(getContext(), getSourceSection(), (InlinableMethodImplementation) implementation);
            replace(dispatch);
            return dispatch.dispatch(frame, block, argumentsObjects);
        } else {
            final GeneralYieldDispatchNode dispatch = new GeneralYieldDispatchNode(getContext(), getSourceSection());
            replace(dispatch);
            return dispatch.dispatch(frame, block, argumentsObjects);
        }
    }

}
