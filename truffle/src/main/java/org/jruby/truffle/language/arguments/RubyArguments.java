/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.arguments;

import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.core.array.ArrayUtils;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.control.FrameOnStackMarker;
import org.jruby.truffle.language.methods.DeclarationContext;
import org.jruby.truffle.language.methods.InternalMethod;

public final class RubyArguments {

    private enum ArgumentIndicies {
        DECLARATION_FRAME,
        CALLER_FRAME,
        METHOD,
        DECLARATION_CONTEXT,
        FRAME_ON_STACK_MARKER,
        SELF,
        BLOCK
    }

    private final static int RUNTIME_ARGUMENT_COUNT = ArgumentIndicies.values().length;

    public static Object[] pack(
            MaterializedFrame declarationFrame,
            MaterializedFrame callerFrame,
            InternalMethod method,
            DeclarationContext declarationContext,
            FrameOnStackMarker frameOnStackMarker,
            Object self,
            DynamicObject block,
            Object[] arguments) {
        assert method != null;
        assert declarationContext != null;
        assert self != null;
        assert block == null || RubyGuards.isRubyProc(block);
        assert arguments != null;

        final Object[] packed = new Object[RUNTIME_ARGUMENT_COUNT + arguments.length];

        packed[ArgumentIndicies.DECLARATION_FRAME.ordinal()] = declarationFrame;
        packed[ArgumentIndicies.CALLER_FRAME.ordinal()] = callerFrame;
        packed[ArgumentIndicies.METHOD.ordinal()] = method;
        packed[ArgumentIndicies.DECLARATION_CONTEXT.ordinal()] = declarationContext;
        packed[ArgumentIndicies.FRAME_ON_STACK_MARKER.ordinal()] = frameOnStackMarker;
        packed[ArgumentIndicies.SELF.ordinal()] = self;
        packed[ArgumentIndicies.BLOCK.ordinal()] = block;

        ArrayUtils.arraycopy(arguments, 0, packed, RUNTIME_ARGUMENT_COUNT, arguments.length);

        return packed;
    }

    // Getters on Object[]

    public static MaterializedFrame getDeclarationFrame(Object[] arguments) {
        return (MaterializedFrame) arguments[ArgumentIndicies.DECLARATION_FRAME.ordinal()];
    }

    public static MaterializedFrame getCallerFrame(Object[] arguments) {
        return (MaterializedFrame) arguments[ArgumentIndicies.CALLER_FRAME.ordinal()];
    }

    public static InternalMethod getMethod(Object[] arguments) {
        return (InternalMethod) arguments[ArgumentIndicies.METHOD.ordinal()];
    }

    public static DeclarationContext getDeclarationContext(Object[] arguments) {
        return (DeclarationContext) arguments[ArgumentIndicies.DECLARATION_CONTEXT.ordinal()];
    }

    public static FrameOnStackMarker getFrameOnStackMarker(Object[] arguments) {
        return (FrameOnStackMarker) arguments[ArgumentIndicies.FRAME_ON_STACK_MARKER.ordinal()];
    }

    public static Object getSelf(Object[] arguments) {
        return arguments[ArgumentIndicies.SELF.ordinal()];
    }

    public static DynamicObject getBlock(Object[] arguments) {
        return (DynamicObject) arguments[ArgumentIndicies.BLOCK.ordinal()];
    }

    public static int getArgumentsCount(Object[] arguments) {
        return arguments.length - RUNTIME_ARGUMENT_COUNT;
    }

    public static Object getArgument(Object[] arguments, int index) {
        return arguments[RUNTIME_ARGUMENT_COUNT + index];
    }

    public static Object[] getArguments(Object[] arguments) {
        return ArrayUtils.extractRange(arguments, RUNTIME_ARGUMENT_COUNT, arguments.length);
    }

    public static Object[] getArguments(Object[] arguments, int start) {
        return ArrayUtils.extractRange(arguments, RUNTIME_ARGUMENT_COUNT + start, arguments.length);
    }

    // Getters on Frame

    public static MaterializedFrame getDeclarationFrame(Frame frame) {
        return getDeclarationFrame(frame.getArguments());
    }

    public static MaterializedFrame getCallerFrame(Frame frame) {
        return getCallerFrame(frame.getArguments());
    }

    public static InternalMethod getMethod(Frame frame) {
        return getMethod(frame.getArguments());
    }

    public static DeclarationContext getDeclarationContext(Frame frame) {
        return getDeclarationContext(frame.getArguments());
    }

    public static FrameOnStackMarker getFrameOnStackMarker(Frame frame) {
        return getFrameOnStackMarker(frame.getArguments());
    }

    public static Object getSelf(Frame frame) {
        return getSelf(frame.getArguments());
    }

    public static DynamicObject getBlock(Frame frame) {
        return getBlock(frame.getArguments());
    }

    public static int getArgumentsCount(Frame frame) {
        return getArgumentsCount(frame.getArguments());
    }

    public static Object getArgument(Frame frame, int index) {
        return getArgument(frame.getArguments(), index);
    }

    public static Object[] getArguments(Frame frame) {
        return getArguments(frame.getArguments());
    }

    public static Object[] getArguments(Frame frame, int start) {
        return getArguments(frame.getArguments(), start);
    }

    // Getters for the declaration frame that let you reach up several levels

    public static MaterializedFrame getDeclarationFrame(VirtualFrame frame, int level) {
        assert level > 0;
        return getDeclarationFrame(RubyArguments.getDeclarationFrame(frame.getArguments()), level - 1);
    }

    @ExplodeLoop
    public static MaterializedFrame getDeclarationFrame(MaterializedFrame frame, int level) {
        assert frame != null;
        assert level >= 0;

        MaterializedFrame currentFrame = frame;

        for (int n = 0; n < level; n++) {
            currentFrame = RubyArguments.getDeclarationFrame(currentFrame.getArguments());
        }

        return currentFrame;
    }

    // Getters that fail safely for when you aren't even sure if this is a Ruby frame

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

        return getSelf(arguments);
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

    public static InternalMethod tryGetMethod(Object[] arguments) {
        if (ArgumentIndicies.METHOD.ordinal() >= arguments.length) {
            return null;
        }

        final Object method = arguments[ArgumentIndicies.METHOD.ordinal()];

        if (method instanceof InternalMethod) {
            return (InternalMethod) method;
        }

        return null;
    }

    // Setters

    public static void setDeclarationFrame(Object[] arguments, MaterializedFrame declarationFrame) {
        arguments[ArgumentIndicies.DECLARATION_FRAME.ordinal()] = declarationFrame;
    }

    public static void setDeclarationContext(Object[] arguments, DeclarationContext declarationContext) {
        arguments[ArgumentIndicies.DECLARATION_CONTEXT.ordinal()] = declarationContext;
    }

    public static void setSelf(Object[] arguments, Object self) {
        arguments[ArgumentIndicies.SELF.ordinal()] = self;
    }

    public static void setArgument(Object[] internalArguments, int index, Object value) {
        internalArguments[RUNTIME_ARGUMENT_COUNT + index] = value;
    }

}
