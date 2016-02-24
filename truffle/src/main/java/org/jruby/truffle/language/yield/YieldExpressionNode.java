/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.yield;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.array.ArrayOperations;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.control.RaiseException;

/**
 * Yield to the current block.
 */
public class YieldExpressionNode extends RubyNode {

    @Children private final RubyNode[] arguments;
    @Child private YieldDispatchHeadNode dispatch;
    private final boolean unsplat;

    public YieldExpressionNode(RubyContext context, SourceSection sourceSection, RubyNode[] arguments, boolean unsplat) {
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

        DynamicObject block = RubyArguments.getBlock(frame.getArguments());

        if (block == null) {
            CompilerDirectives.transferToInterpreter();

            block = RubyArguments.getMethod(frame.getArguments()).getCapturedBlock();

            if (block == null) {
                throw new RaiseException(coreLibrary().noBlockToYieldTo(this));
            }
        }

        if (unsplat) {
            argumentsObjects = unsplat(argumentsObjects);
        }

        return dispatch.dispatch(frame, block, argumentsObjects);
    }

    @TruffleBoundary
    private Object[] unsplat(Object[] argumentsObjects) {
        // TOOD(CS): what is the error behaviour here?
        assert argumentsObjects.length == 1;
        assert RubyGuards.isRubyArray(argumentsObjects[0]);
        return ArrayOperations.toObjectArray(((DynamicObject) argumentsObjects[0]));
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        if (RubyArguments.getBlock(frame.getArguments()) == null) {
            return nil();
        } else {
            return create7BitString("yield", UTF8Encoding.INSTANCE);
        }
    }

}
