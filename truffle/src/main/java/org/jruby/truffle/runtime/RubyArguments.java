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
import org.jruby.truffle.nodes.methods.MarkerNode;
import org.jruby.truffle.runtime.core.RubyHash;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.methods.InternalMethod;
import org.jruby.truffle.runtime.array.ArrayUtils;

/**
 * Pack and unpack Ruby method arguments to and from an array of objects.
 */
public final class RubyArguments {

    public static final int METHOD_INDEX = 0;
    public static final int DECLARATION_FRAME_INDEX = 1;
    public static final int SELF_INDEX = 2;
    public static final int BLOCK_INDEX = 3;
    public static final int RUNTIME_ARGUMENT_COUNT = 4;

    public static Object[] pack(InternalMethod method, MaterializedFrame declarationFrame, Object self, RubyProc block, Object[] arguments) {
        final Object[] packed = new Object[arguments.length + RUNTIME_ARGUMENT_COUNT];

        packed[METHOD_INDEX] = method;
        packed[DECLARATION_FRAME_INDEX] = declarationFrame;
        packed[SELF_INDEX] = self;
        packed[BLOCK_INDEX] = block;
        ArrayUtils.arraycopy(arguments, 0, packed, RUNTIME_ARGUMENT_COUNT, arguments.length);

        return packed;
    }
    
    public static Object getOptimizedKeywordArgument(Object[] arguments,
            int index) {
        return arguments[arguments.length - 1 + index];
    }

    public static boolean isKwOptimized(Object[] arguments) {
        return arguments[arguments.length - 1] instanceof MarkerNode.Marker;
    }

    public static InternalMethod getMethod(Object[] arguments) {
        return (InternalMethod) arguments[METHOD_INDEX];
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

    public static Object[] extractUserArgumentsWithUnshift(Object first, Object[] arguments) {
        final Object[] range = ArrayUtils.extractRange(arguments, BLOCK_INDEX, arguments.length);
        range[0] = first;
        return range;
    }

    public static int getUserArgumentsCount(Object[] internalArguments) {
        return internalArguments.length - RUNTIME_ARGUMENT_COUNT;
    }

    public static int getNamedUserArgumentsCount(Object[] internalArguments) {
        if (isKwOptimized(internalArguments)) {
            return getUserArgumentsCount(internalArguments)
                    - getMethod(internalArguments).getSharedMethodInfo().getArity()
                            .getKeywordArguments().size() - 1;
        } else {
            return getUserArgumentsCount(internalArguments);
        }
    }

    public static Object getUserArgument(Object[] internalArguments, int index) {
        return internalArguments[RUNTIME_ARGUMENT_COUNT + index];
    }

    public static void setUserArgument(Object[] internalArguments, int index, Object value) {
        internalArguments[RUNTIME_ARGUMENT_COUNT + index] = value;
    }

    public static RubyHash getUserKeywordsHash(Object[] internalArguments, int minArgumentCount) {
        final int argumentCount = getUserArgumentsCount(internalArguments);

        if (argumentCount <= minArgumentCount) {
            return null;
        }

        final Object lastArgument = getUserArgument(internalArguments, argumentCount - 1);

        if (lastArgument instanceof RubyHash) {
            return (RubyHash) lastArgument;
        }

        return null;
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
