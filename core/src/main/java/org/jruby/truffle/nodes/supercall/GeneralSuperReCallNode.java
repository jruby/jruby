/*
 * Copyright (c) 2013-2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.supercall;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.SourceSection;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import org.jruby.common.IRubyWarnings;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.NilPlaceholder;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyModule;
import org.jruby.truffle.runtime.methods.RubyMethod;

public class GeneralSuperReCallNode extends RubyNode {

    @Child protected IndirectCallNode callNode;

    @CompilerDirectives.CompilationFinal private Assumption unmodifiedAssumption;
    @CompilerDirectives.CompilationFinal private RubyMethod method;

    public GeneralSuperReCallNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
        callNode = Truffle.getRuntime().createIndirectCallNode();
    }

    @ExplodeLoop
    @Override
    public final Object execute(VirtualFrame frame) {
        // Check we have a method and the module is unmodified

        if (method == null || !unmodifiedAssumption.isValid()) {
            CompilerDirectives.transferToInterpreterAndInvalidate();

            // Lookup method

            final RubyModule declaringModule = getMethod().getDeclaringModule();

            method = ((RubyClass) declaringModule).getSuperclass().lookupMethod(getMethod().getName());

            if (method == null || method.isUndefined()) {
                method = null;
                throw new RaiseException(getContext().getCoreLibrary().nameErrorNoMethod(getMethod().getName(), RubyArguments.getSelf(frame.getArguments()).toString()));
            }

            unmodifiedAssumption = declaringModule.getUnmodifiedAssumption();
        }

        // Call the method

        return callNode.call(frame, method.getCallTarget(), frame.getArguments());
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
