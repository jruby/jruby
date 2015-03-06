/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.yield;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyProc;

/**
 * Yield to the current block.
 */
public class YieldNode extends RubyNode {

    @Children private final RubyNode[] arguments;
    @Child private YieldDispatchHeadNode dispatch;
    private final boolean unsplat;

    public YieldNode(RubyContext context, SourceSection sourceSection, RubyNode[] arguments, boolean unsplat) {
        super(context, sourceSection);
        this.arguments = arguments;
        dispatch = new YieldDispatchHeadNode(getContext());
        this.unsplat = unsplat;
    }

    @ExplodeLoop
    @Override
    public final Object execute(VirtualFrame frame) {
        Object[] argumentsObjects = new Object[arguments.length];

        for (int i = 0; i < arguments.length; i++) {
            argumentsObjects[i] = arguments[i].execute(frame);
        }

        final RubyProc block = RubyArguments.getBlock(frame.getArguments());

        if (block == null) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().noBlockToYieldTo(this));
        }

        if (unsplat) {
            notDesignedForCompilation("d41ae5686eae4f23bd0d9ebbdb0fc002");

            // TOOD(CS): what is the error behaviour here?
            assert argumentsObjects.length == 1;
            assert argumentsObjects[0] instanceof RubyArray;
            argumentsObjects = ((RubyArray) argumentsObjects[0]).slowToArray();
        }

        return dispatch.dispatch(frame, block, argumentsObjects);
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        notDesignedForCompilation("73d218aff82b4cc88c23520f3ba8c38f");

        if (RubyArguments.getBlock(frame.getArguments()) == null) {
            return getContext().getCoreLibrary().getNilObject();
        } else {
            return getContext().makeString("yield");
        }
    }

}
