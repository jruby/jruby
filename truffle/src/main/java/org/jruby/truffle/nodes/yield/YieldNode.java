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
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.RubyString;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.ArrayOperations;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.util.StringSupport;

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

        final DynamicObject block = RubyArguments.getBlock(frame.getArguments());

        if (block == null) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().noBlockToYieldTo(this));
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
            return Layouts.STRING.createString(getContext().getCoreLibrary().getStringFactory(), RubyString.encodeBytelist("yield", UTF8Encoding.INSTANCE), StringSupport.CR_UNKNOWN, null);
        }
    }

}
