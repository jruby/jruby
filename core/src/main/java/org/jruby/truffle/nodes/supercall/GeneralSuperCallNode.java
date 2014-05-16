/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.supercall;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.core.array.RubyArray;

/**
 * Represents a super call - that is a call with self as the receiver, but the superclass of self
 * used for lookup. Currently implemented without any caching, and needs to be replaced with the
 * same caching mechanism as for normal calls without complicating the existing calls too much.
 */
@NodeInfo(shortName = "general-super-call")
public class GeneralSuperCallNode extends AbstractGeneralSuperCallNode {

    private final boolean isSplatted;
    @Child protected RubyNode block;
    @Children protected final RubyNode[] arguments;
    @Child protected IndirectCallNode callNode;

    public GeneralSuperCallNode(RubyContext context, SourceSection sourceSection, String name, RubyNode block, RubyNode[] arguments, boolean isSplatted) {
        super(context, sourceSection, name);
        assert arguments != null;
        assert !isSplatted || arguments.length == 1;
        this.block = block;
        this.arguments = arguments;
        this.isSplatted = isSplatted;

        // TODO(CS): We definitely don't want an indirect call...
        callNode = Truffle.getRuntime().createIndirectCallNode();
    }

    @ExplodeLoop
    @Override
    public final Object execute(VirtualFrame frame) {
        notDesignedForCompilation();

        final RubyBasicObject self = (RubyBasicObject) RubyArguments.getSelf(frame.getArguments());

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

            if (blockTempObject instanceof NilPlaceholder) {
                blockObject = null;
            } else {
                blockObject = (RubyProc) blockTempObject;
            }
        } else {
            blockObject = null;
        }

        // Check we have a method and the module is unmodified

        if (!guard()) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            lookup();
        }

        // Call the method

        if (isSplatted) {
            // TODO(CS): need something better to splat the arguments array
            CompilerAsserts.neverPartOfCompilation();
            final RubyArray argumentsArray = (RubyArray) argumentsObjects[0];
            return callNode.call(frame, method.getCallTarget(), RubyArguments.pack(method.getDeclarationFrame(), self, blockObject,argumentsArray.slowToArray()));
        } else {
            return callNode.call(frame, method.getCallTarget(), RubyArguments.pack(method.getDeclarationFrame(), self, blockObject, argumentsObjects));
        }
    }


}
