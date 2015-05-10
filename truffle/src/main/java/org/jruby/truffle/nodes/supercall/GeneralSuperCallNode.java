/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.supercall;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyProc;

/**
 * Represents a super call - that is a call with self as the receiver, but the superclass of self
 * used for lookup. Currently implemented without any caching, and needs to be replaced with the
 * same caching mechanism as for normal calls without complicating the existing calls too much.
 */
public class GeneralSuperCallNode extends AbstractGeneralSuperCallNode {

    private final boolean isSplatted;
    @Child private RubyNode block;
    @Children private final RubyNode[] arguments;

    public GeneralSuperCallNode(RubyContext context, SourceSection sourceSection, RubyNode block, RubyNode[] arguments, boolean isSplatted) {
        super(context, sourceSection);
        assert arguments != null;
        assert !isSplatted || arguments.length == 1;
        this.block = block;
        this.arguments = arguments;
        this.isSplatted = isSplatted;
    }

    @ExplodeLoop
    @Override
    public final Object execute(VirtualFrame frame) {
        final Object self = RubyArguments.getSelf(frame.getArguments());

        // Execute the arguments

        final Object[] argumentsObjects = new Object[arguments.length];

        CompilerAsserts.compilationConstant(arguments.length);
        for (int i = 0; i < arguments.length; i++) {
            argumentsObjects[i] = arguments[i].execute(frame);
        }

        // Execute the block

        RubyProc blockObject;

        if (block != null) {
            final Object blockTempObject = block.execute(frame);

            if (blockTempObject == nil()) {
                blockObject = null;
            } else {
                blockObject = (RubyProc) blockTempObject;
            }
        } else {
            blockObject = null;
        }

        // Check we have a method and the module is unmodified

        if (!guard(frame, self)) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            lookup(frame);
        }

        // Call the method

        if (isSplatted) {
            // TODO(CS): need something better to splat the arguments array
            final RubyArray argumentsArray = (RubyArray) argumentsObjects[0];
            return callNode.call(frame, RubyArguments.pack(superMethod, superMethod.getDeclarationFrame(), self, blockObject,argumentsArray.slowToArray()));
        } else {
            return callNode.call(frame, RubyArguments.pack(superMethod, superMethod.getDeclarationFrame(), self, blockObject, argumentsObjects));
        }
    }


}
