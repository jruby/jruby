/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import org.jruby.truffle.runtime.core.*;

import java.util.Iterator;

/**
 * Pack and unpack Ruby method arguments to and from an array of objects.
 */
public final class RubyArguments {

    public static final int DECLARATION_FRAME_INDEX = 0;
    public static final int SELF_INDEX = 1;
    public static final int BLOCK_INDEX = 2;
    public static final int RUNTIME_ARGUMENT_COUNT = 3;

    public static Object[] pack(MaterializedFrame declarationFrame, Object self, RubyProc block, Object... arguments) {
        assert RubyContext.shouldObjectBeVisible(self);
        assert RubyContext.shouldObjectsBeVisible(arguments);

        final Object[] packed = new Object[arguments.length + RUNTIME_ARGUMENT_COUNT];
        packed[DECLARATION_FRAME_INDEX] = declarationFrame;
        packed[SELF_INDEX] = self;
        packed[BLOCK_INDEX] = block;

        for (int n = 0; n < arguments.length; n++) {
            packed[RUNTIME_ARGUMENT_COUNT + n] = arguments[n];
        }

        return packed;
    }

    public static Object getSelf(Object[] arguments) {
        return arguments[SELF_INDEX];
    }

    public static RubyProc getBlock(Object[] arguments) {
        return (RubyProc) arguments[BLOCK_INDEX];
    }

    public static Object[] extractUserArguments(Object[] arguments) {
        final Object[] userArguments = new Object[arguments.length - RUNTIME_ARGUMENT_COUNT];

        for (int n = 0; n < userArguments.length; n++) {
            userArguments[n] = arguments[RUNTIME_ARGUMENT_COUNT + n];
        }

        return userArguments;
    }

    public static int getUserArgumentsCount(Object[] internalArguments) {
        return internalArguments.length - RUNTIME_ARGUMENT_COUNT;
    }

    public static Object getUserArgument(Object[] internalArguments, int index) {
        return internalArguments[RUNTIME_ARGUMENT_COUNT + index];
    }

    public static MaterializedFrame getDeclarationFrame(Object[] arguments) {
        return (MaterializedFrame) arguments[DECLARATION_FRAME_INDEX];
    }

    /**
     * Get the declaration frame a certain number of levels up from the current frame, where the
     * current frame is 0.
     */
    public static MaterializedFrame getDeclarationFrame(VirtualFrame frame, int level) {
        assert level > 0;

        MaterializedFrame parentFrame = RubyArguments.getDeclarationFrame(frame.getArguments());
        return getDeclarationFrame(parentFrame, level - 1);
    }

    /**
     * Get the declaration frame a certain number of levels up from the current frame, where the
     * current frame is 0.
     */
    @ExplodeLoop
    private static MaterializedFrame getDeclarationFrame(MaterializedFrame frame, int level) {
        assert frame != null;
        assert level >= 0;

        MaterializedFrame parentFrame = frame;

        for (int n = 0; n < level; n++) {
            parentFrame = RubyArguments.getDeclarationFrame(parentFrame.getArguments());
        }

        return parentFrame;
    }

}
