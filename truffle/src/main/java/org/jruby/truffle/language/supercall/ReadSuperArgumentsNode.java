/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.supercall;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import org.jruby.truffle.core.array.ArrayToObjectArrayNode;
import org.jruby.truffle.core.array.ArrayToObjectArrayNodeGen;
import org.jruby.truffle.language.RubyNode;

/**
 * Get the arguments of a super call with explicit arguments.
 */
public class ReadSuperArgumentsNode extends RubyNode {

    @Children private final RubyNode[] arguments;
    @Child private ArrayToObjectArrayNode unsplatNode;

    private final boolean isSplatted;

    public ReadSuperArgumentsNode(RubyNode[] arguments, boolean isSplatted) {
        assert !isSplatted || arguments.length == 1;
        this.arguments = arguments;
        this.isSplatted = isSplatted;
    }

    @ExplodeLoop
    @Override
    public final Object execute(VirtualFrame frame) {
        CompilerAsserts.compilationConstant(arguments.length);

        // Execute the arguments
        final Object[] argumentsObjects = new Object[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            argumentsObjects[i] = arguments[i].execute(frame);
        }

        if (isSplatted) {
            return unsplat(argumentsObjects);
        } else {
            return argumentsObjects;
        }
    }

    private Object[] unsplat(Object[] argumentsObjects) {
        if (unsplatNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            unsplatNode = insert(ArrayToObjectArrayNodeGen.create(null));
        }
        return unsplatNode.unsplat(argumentsObjects);
    }

}
