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
import org.jruby.common.IRubyWarnings;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.*;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.core.array.*;
import org.jruby.truffle.runtime.methods.*;

/**
 * Represents a super call - that is a call with self as the receiver, but the superclass of self
 * used for lookup. Currently implemented without any caching, and needs to be replaced with the
 * same caching mechanism as for normal calls without complicating the existing calls too much.
 */
@NodeInfo(shortName = "general-super-call")
public class GeneralSuperCallNode extends RubyNode {

    private final boolean isSplatted;
    @Child protected RubyNode block;
    @Children protected final RubyNode[] arguments;
    @Child protected IndirectCallNode callNode;

    @CompilerDirectives.CompilationFinal private Assumption unmodifiedAssumption;
    @CompilerDirectives.CompilationFinal private RubyMethod method;

    public GeneralSuperCallNode(RubyContext context, SourceSection sourceSection, RubyNode block, RubyNode[] arguments, boolean isSplatted) {
        super(context, sourceSection);
        assert arguments != null;
        assert !isSplatted || arguments.length == 1;
        this.block = block;
        this.arguments = arguments;
        this.isSplatted = isSplatted;

        callNode = Truffle.getRuntime().createIndirectCallNode();
    }

    @ExplodeLoop
    @Override
    public final Object execute(VirtualFrame frame) {
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

        if (method == null || !unmodifiedAssumption.isValid()) {
            CompilerDirectives.transferToInterpreterAndInvalidate();

            // Lookup method

            final RubyModule declaringModule = getMethod().getDeclaringModule();

            method = ((RubyClass) declaringModule).getSuperclass().lookupMethod(getMethod().getName());

            if (method == null || method.isUndefined()) {
                method = null;
                throw new RaiseException(getContext().getCoreLibrary().nameErrorNoMethod(getMethod().getName(), self.toString()));
            }

            unmodifiedAssumption = declaringModule.getUnmodifiedAssumption();
        }

        // Call the method

        if (isSplatted) {
            // TODO(CS): need something better to splat the arguments array
            CompilerAsserts.neverPartOfCompilation();
            final RubyArray argumentsArray = (RubyArray) argumentsObjects[0];
            return callNode.call(frame, method.getCallTarget(), RubyArguments.pack(method.getDeclarationFrame(), self, blockObject, argumentsArray.asList().toArray()));
        } else {
            return callNode.call(frame, method.getCallTarget(), RubyArguments.pack(method.getDeclarationFrame(), self, blockObject, argumentsObjects));
        }
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        final RubyContext context = getContext();

        try {
            final RubyBasicObject self = context.getCoreLibrary().box(RubyArguments.getSelf(frame.getArguments()));
            final RubyBasicObject receiverRubyObject = context.getCoreLibrary().box(self);

            final RubyMethod method = receiverRubyObject.getRubyClass().getSuperclass().lookupMethod(getMethod().getName());

            if (method == null || method.isUndefined() || !method.isVisibleTo(self)) {
                return NilPlaceholder.INSTANCE;
            } else {
                return context.makeString("super");
            }
        } catch (Exception e) {
            return NilPlaceholder.INSTANCE;
        }
    }

}
