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
import com.oracle.truffle.api.object.DynamicObject;

import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.methods.DeclarationContext;
import org.jruby.truffle.nodes.methods.MarkerNode;
import org.jruby.truffle.runtime.array.ArrayUtils;
import org.jruby.truffle.runtime.methods.InternalMethod;

/**
 * Pack and unpack Ruby method arguments to and from an array of objects.
 */
public final class RubyArguments {

    public static final int METHOD_INDEX = 0;
    public static final int DECLARATION_FRAME_INDEX = 1;
    public static final int CALLER_FRAME_INDEX = 2;
    public static final int SELF_INDEX = 3;
    public static final int BLOCK_INDEX = 4;
    public static final int DECLARATION_CONTEXT_INDEX = 5;
    public static final int RUNTIME_ARGUMENT_COUNT = 6;

    public static Object[] pack(InternalMethod method, MaterializedFrame declarationFrame, MaterializedFrame callerFrame, Object self, DynamicObject block, DeclarationContext declarationContext, Object[] arguments) {
        assert self != null;
        assert block == null || RubyGuards.isRubyProc(block);
        assert declarationContext != DeclarationContext.METHOD || method != null;
        assert declarationContext != null;
        assert arguments != null;

        final Object[] packed = new Object[arguments.length + RUNTIME_ARGUMENT_COUNT];

        packed[METHOD_INDEX] = method;
        packed[DECLARATION_FRAME_INDEX] = declarationFrame;
        packed[CALLER_FRAME_INDEX] = callerFrame;
        packed[SELF_INDEX] = self;
        packed[BLOCK_INDEX] = block;
        packed[DECLARATION_CONTEXT_INDEX] = declarationContext;
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

    public static void setSelf(Object[] arguments, Object self) {
        arguments[SELF_INDEX] = self;
    }

    public static DynamicObject getBlock(Object[] arguments) {
        return (DynamicObject) arguments[BLOCK_INDEX];
    }

    public static DeclarationContext getDeclarationContext(Object[] arguments) {
        return (DeclarationContext) arguments[DECLARATION_CONTEXT_INDEX];
    }

    public static void setDeclarationContext(Object[] arguments, DeclarationContext declarationContext) {
        arguments[DECLARATION_CONTEXT_INDEX] = declarationContext;
    }

    public static Object[] extractUserArguments(Object[] arguments) {
        return ArrayUtils.extractRange(arguments, RUNTIME_ARGUMENT_COUNT, arguments.length);
    }

    public static Object[] extractUserArgumentsFrom(Object[] arguments, int start) {
        return ArrayUtils.extractRange(arguments, RUNTIME_ARGUMENT_COUNT + start, arguments.length);
    }

    public static Object[] extractUserArgumentsWithUnshift(Object first, Object[] arguments) {
        final Object[] range = ArrayUtils.extractRange(arguments, RUNTIME_ARGUMENT_COUNT - 1, arguments.length);
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
                            .getKeywordsCount() - 1;
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

    public static DynamicObject getUserKeywordsHash(Object[] internalArguments, int minArgumentCount) {
        final int argumentCount = getUserArgumentsCount(internalArguments);

        if (argumentCount <= minArgumentCount) {
            return null;
        }

        final Object lastArgument = getUserArgument(internalArguments, argumentCount - 1);

        if (RubyGuards.isRubyHash(lastArgument)) {
            return (DynamicObject) lastArgument;
        }

        return null;
    }

    public static MaterializedFrame tryGetDeclarationFrame(Object[] arguments) {
        if (DECLARATION_FRAME_INDEX >= arguments.length) {
            return null;
        }

        final Object frame = arguments[DECLARATION_FRAME_INDEX];

        if (frame instanceof MaterializedFrame) {
            return (MaterializedFrame) frame;
        }

        return null;
    }

    public static MaterializedFrame getCallerFrame(Object[] arguments) {
        return (MaterializedFrame) arguments[CALLER_FRAME_INDEX];
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
    public static MaterializedFrame getDeclarationFrame(MaterializedFrame frame, int level) {
        assert frame != null;
        assert level >= 0;

        MaterializedFrame parentFrame = frame;

        for (int n = 0; n < level; n++) {
            parentFrame = RubyArguments.getDeclarationFrame(parentFrame.getArguments());
        }

        return parentFrame;
    }
}
