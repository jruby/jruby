/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime;

import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.methods.MethodLike;
import org.jruby.truffle.runtime.util.ArrayUtils;

import java.util.Arrays;

/**
 * Pack and unpack Ruby method arguments to and from an array of objects.
 */
public final class RubyArguments {

    public static final int METHOD_INDEX = 0;
    public static final int DECLARATION_FRAME_INDEX = 1;
    public static final int SELF_INDEX = 2;
    public static final int BLOCK_INDEX = 3;
    public static final int RUNTIME_ARGUMENT_COUNT = 4;

    public static Object[] pack(MethodLike method, MaterializedFrame declarationFrame, Object self, RubyProc block, Object[] arguments) {
        final Object[] packed = new Object[arguments.length + RUNTIME_ARGUMENT_COUNT];

        packed[METHOD_INDEX] = method;
        packed[DECLARATION_FRAME_INDEX] = declarationFrame;
        packed[SELF_INDEX] = self;
        packed[BLOCK_INDEX] = block;
        arraycopy(arguments, 0, packed, RUNTIME_ARGUMENT_COUNT, arguments.length);

        return packed;
    }

    public static MethodLike getMethod(Object[] arguments) {
        return (MethodLike) arguments[METHOD_INDEX];
    }

    public static Object getSelf(Object[] arguments) {
        return arguments[SELF_INDEX];
    }

    public static RubyProc getBlock(Object[] arguments) {
        return (RubyProc) arguments[BLOCK_INDEX];
    }

    public static Object[] extractUserArguments(Object[] arguments) {
        return ArrayUtils.extractRange(arguments, RUNTIME_ARGUMENT_COUNT, arguments.length);
    }

    public static Object[] concatUserArguments(Object o, Object[] arguments) {
        final Object[] concatenatedArguments;

        if (o instanceof Object[]) {
            Object[] concatArray = (Object[]) o;
            concatenatedArguments = new Object[concatArray.length + arguments.length - RUNTIME_ARGUMENT_COUNT];

            arraycopy(concatArray, 0, concatenatedArguments, 0, concatArray.length);
            arraycopy(arguments, RUNTIME_ARGUMENT_COUNT, concatenatedArguments, concatArray.length, getUserArgumentsCount(arguments));
        } else {
            concatenatedArguments = new Object[1 + arguments.length - RUNTIME_ARGUMENT_COUNT];

            concatenatedArguments[0] = o;
            arraycopy(arguments, RUNTIME_ARGUMENT_COUNT, concatenatedArguments, 1, getUserArgumentsCount(arguments));
        }

        return concatenatedArguments;
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

    @ExplodeLoop
    public static void arraycopy(Object[] src, int srcPos, Object[] dest, int destPos, int length) {
        for (int i = 0; i < length; i++) {
            dest[destPos + i] = src[srcPos + i];
        }
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
