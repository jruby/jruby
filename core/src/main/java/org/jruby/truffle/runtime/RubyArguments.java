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
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import org.jruby.truffle.runtime.core.*;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Arguments and other context passed to a Ruby method. Includes the central Ruby context object,
 * optionally the scope at the point of declaration (forming a closure), the value of self, a passed
 * block, and the formal arguments.
 */
public final class RubyArguments {

    public static final int RUNTIME_ARGUMENT_COUNT = 3;
    public static final int DECLARATION_FRAME_INDEX = 0;
    public static final int SELF_INDEX = 1;
    public static final int BLOCK_INDEX = 2;

    private final Object[] internalArguments;

    public RubyArguments(Object[] internalArguments) {
        this.internalArguments = internalArguments;
    }

    public static Object[] create(MaterializedFrame declarationFrame, Object self, RubyProc block, Object... arguments) {
        assert self != null;
        assert arguments != null;
        Object[] internalArguments = create(arguments.length);
        setDeclarationFrame(internalArguments, declarationFrame);
        setSelf(internalArguments, self);
        setBlock(internalArguments, block);
        for (int i = RUNTIME_ARGUMENT_COUNT; i < internalArguments.length; ++i) {
            internalArguments[i] = arguments[i - RUNTIME_ARGUMENT_COUNT];
        }
        return internalArguments;
    }

    public static Object[] create(int userArgumentCount) {
        return new Object[userArgumentCount + RUNTIME_ARGUMENT_COUNT];
    }

    public static void setSelf(Object[] arguments, Object self) {
        arguments[SELF_INDEX] = self;
    }

    public static void setBlock(Object[] arguments, RubyProc block) {
        arguments[BLOCK_INDEX] = block;
    }

    public static void setDeclarationFrame(Object[] arguments, MaterializedFrame frame) {
        arguments[DECLARATION_FRAME_INDEX] = frame;
    }

    public static Object getSelf(Object[] arguments) {
        return arguments[SELF_INDEX];
    }

    public static RubyProc getBlock(Object[] arguments) {
        return (RubyProc) arguments[BLOCK_INDEX];
    }

    public static MaterializedFrame getDeclarationFrame(Object[] arguments) {
        return (MaterializedFrame) arguments[DECLARATION_FRAME_INDEX];
    }

    public static Object[] extractUserArguments(Object[] arguments) {
        return Arrays.copyOfRange(arguments, RUNTIME_ARGUMENT_COUNT, arguments.length);
    }

    public MaterializedFrame getDeclarationFrame() {
        return getDeclarationFrame(internalArguments);
    }

    /**
     * Get the declaration frame a certain number of levels up from the current frame, where the
     * current frame is 0.
     */
    public static MaterializedFrame getDeclarationFrame(VirtualFrame frame, int level) {
        assert level > 0;

        MaterializedFrame parentFrame = new RubyArguments(frame.getArguments()).getDeclarationFrame();
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
            parentFrame = new RubyArguments(parentFrame.getArguments()).getDeclarationFrame();
        }

        return parentFrame;
    }

    public static FrameInstance getCallerFrame() {
        final Iterable<FrameInstance> stackIterable = Truffle.getRuntime().getStackTrace();
        assert stackIterable != null;

        final Iterator<FrameInstance> stack = stackIterable.iterator();

        assert stack.hasNext();
        return stack.next();
    }

    public static Frame getCallerFrame(FrameInstance.FrameAccess access, boolean slowPath) {
        return getCallerFrame().getFrame(access, slowPath);
    }

    public Object getSelf() {
        return getSelf(internalArguments);
    }

    public RubyProc getBlock() {
        return getBlock(internalArguments);
    }

    public Object[] getArgumentsClone() {
        return extractUserArguments(internalArguments);
    }

    public static void setUserArgument(Object[] internalArguments, int index, Object arg) {
        internalArguments[RUNTIME_ARGUMENT_COUNT + index] = arg;
    }

    public static Object getUserArgument(Object[] internalArguments, int index) {
        return internalArguments[RUNTIME_ARGUMENT_COUNT + index];
    }

    public int getUserArgumentsCount() {
        return internalArguments.length - RUNTIME_ARGUMENT_COUNT;
    }

    public Object getUserArgument(int index) {
        return getUserArgument(internalArguments, index);
    }

}
