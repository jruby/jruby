/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.methods.DeclarationContext;
import org.jruby.truffle.nodes.methods.MarkerNode;
import org.jruby.truffle.runtime.array.ArrayUtils;
import org.jruby.truffle.runtime.control.FrameOnStackMarker;
import org.jruby.truffle.runtime.methods.InternalMethod;

/**
 * Pack and unpack Ruby method arguments to and from an array of objects.
 */
public final class RubyArguments {

    private enum ArgumentIndicies {
        METHOD,
        DECLARATION_FRAME,
        CALLER_FRAME,
        SELF,
        BLOCK,
        DECLARATION_CONTEXT,
        FRAME_ON_STACK_MARKER
    }

    private final static int RUNTIME_ARGUMENT_COUNT = ArgumentIndicies.values().length;

    public static Object[] pack(InternalMethod method, MaterializedFrame declarationFrame, MaterializedFrame callerFrame, Object self, DynamicObject block, DeclarationContext declarationContext, FrameOnStackMarker frameOnStackMarker, Object[] arguments) {
        assert method != null;
        assert self != null;
        assert block == null || RubyGuards.isRubyProc(block);
        assert declarationContext != null;
        assert arguments != null;

        final Object[] packed = new Object[arguments.length + RUNTIME_ARGUMENT_COUNT];

        packed[ArgumentIndicies.METHOD.ordinal()] = method;
        packed[ArgumentIndicies.DECLARATION_FRAME.ordinal()] = declarationFrame;
        packed[ArgumentIndicies.CALLER_FRAME.ordinal()] = callerFrame;
        packed[ArgumentIndicies.SELF.ordinal()] = self;
        packed[ArgumentIndicies.BLOCK.ordinal()] = block;
        packed[ArgumentIndicies.DECLARATION_CONTEXT.ordinal()] = declarationContext;
        packed[ArgumentIndicies.FRAME_ON_STACK_MARKER.ordinal()] = frameOnStackMarker;
        ArrayUtils.arraycopy(arguments, 0, packed, RUNTIME_ARGUMENT_COUNT, arguments.length);

        return packed;
    }

    public static InternalMethod getMethod(Object[] arguments) {
        return (InternalMethod) arguments[ArgumentIndicies.METHOD.ordinal()];
    }

    public static Object getSelf(Object[] arguments) {
        return arguments[ArgumentIndicies.SELF.ordinal()];
    }

    public static void setSelf(Object[] arguments, Object self) {
        arguments[ArgumentIndicies.SELF.ordinal()] = self;
    }

    public static DynamicObject getBlock(Object[] arguments) {
        return (DynamicObject) arguments[ArgumentIndicies.BLOCK.ordinal()];
    }

    public static DeclarationContext getDeclarationContext(Object[] arguments) {
        return (DeclarationContext) arguments[ArgumentIndicies.DECLARATION_CONTEXT.ordinal()];
    }

    public static void setDeclarationContext(Object[] arguments, DeclarationContext declarationContext) {
        arguments[ArgumentIndicies.DECLARATION_CONTEXT.ordinal()] = declarationContext;
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
        return getUserArgumentsCount(internalArguments);
    }

    public static Object getUserArgument(Object[] internalArguments, int index) {
        return internalArguments[RUNTIME_ARGUMENT_COUNT + index];
    }

    public static void setUserArgument(Object[] internalArguments, int index, Object value) {
        internalArguments[RUNTIME_ARGUMENT_COUNT + index] = value;
    }

    public static DynamicObject getUserKeywordsHash(Object[] internalArguments, int minArgumentCount, RubyContext context) {
        final int argumentCount = getUserArgumentsCount(internalArguments);

        if (argumentCount <= minArgumentCount) {
            return null;
        }

        final Object lastArgument = getUserArgument(internalArguments, argumentCount - 1);

        if (RubyGuards.isRubyHash(lastArgument)) {
            return (DynamicObject) lastArgument;
        }

        CompilerDirectives.transferToInterpreter();

        if ((boolean) context.inlineRubyHelper(null, "last_arg.respond_to?(:to_hash)", "last_arg", lastArgument)) {
            final Object converted = context.inlineRubyHelper(null, "last_arg.to_hash", "last_arg", lastArgument);

            if (RubyGuards.isRubyHash(converted)) {
                setUserArgument(internalArguments, argumentCount - 1, converted);
                return (DynamicObject) converted;
            }
        }

        return null;
    }

    public static MaterializedFrame tryGetDeclarationFrame(Object[] arguments) {
        if (ArgumentIndicies.DECLARATION_FRAME.ordinal() >= arguments.length) {
            return null;
        }

        final Object frame = arguments[ArgumentIndicies.DECLARATION_FRAME.ordinal()];

        if (frame instanceof MaterializedFrame) {
            return (MaterializedFrame) frame;
        }

        return null;
    }

    public static Object tryGetSelf(Object[] arguments) {
        if (ArgumentIndicies.SELF.ordinal() >= arguments.length) {
            return null;
        }
        return arguments[ArgumentIndicies.SELF.ordinal()];
    }

    public static DynamicObject tryGetBlock(Object[] arguments) {
        if (ArgumentIndicies.BLOCK.ordinal() >= arguments.length) {
            return null;
        }

        final Object block = arguments[ArgumentIndicies.BLOCK.ordinal()];
        if (block instanceof DynamicObject) {
            return (DynamicObject) block;
        } else {
            return null;
        }
    }

    public static MaterializedFrame getCallerFrame(Object[] arguments) {
        return (MaterializedFrame) arguments[ArgumentIndicies.CALLER_FRAME.ordinal()];
    }

    public static MaterializedFrame getDeclarationFrame(Object[] arguments) {
        return (MaterializedFrame) arguments[ArgumentIndicies.DECLARATION_FRAME.ordinal()];
    }

    public static void setDeclarationFrame(Object[] arguments, MaterializedFrame declarationFrame) {
        arguments[ArgumentIndicies.DECLARATION_FRAME.ordinal()] = declarationFrame;
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

    public static FrameOnStackMarker getFrameOnStackMarker(Object[] arguments) {
        return (FrameOnStackMarker) arguments[ArgumentIndicies.FRAME_ON_STACK_MARKER.ordinal()];
    }
}
