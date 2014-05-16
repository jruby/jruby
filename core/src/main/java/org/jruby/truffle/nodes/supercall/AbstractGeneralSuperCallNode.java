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
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.common.IRubyWarnings;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.NilPlaceholder;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyCallStack;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyModule;
import org.jruby.truffle.runtime.methods.RubyMethod;

public abstract class AbstractGeneralSuperCallNode extends RubyNode {

    private final String name;

    @CompilerDirectives.CompilationFinal protected Assumption unmodifiedAssumption;
    @CompilerDirectives.CompilationFinal protected RubyMethod method;

    public AbstractGeneralSuperCallNode(RubyContext context, SourceSection sourceSection, String name) {
        super(context, sourceSection);
        this.name = name;
    }

    protected boolean guard() {
        // TODO(CS): not sure this is enough... lots of 'unspecified' behaviour in the ISO spec here
        return method != null && unmodifiedAssumption.isValid();
    }

    protected void lookup() {
        CompilerAsserts.neverPartOfCompilation();

        final RubyModule declaringModule = RubyCallStack.getCurrentDeclaringModule();

        method = ((RubyClass) declaringModule).getSuperclass().lookupMethod(name);

        if (method == null || method.isUndefined()) {
            method = null;
            throw new RaiseException(getContext().getCoreLibrary().nameErrorNoMethod(name, "todo"));
        }

        getContext().getRuntime().getWarnings().warn(IRubyWarnings.ID.TRUFFLE, getSourceSection().getSource().getName(), getSourceSection().getStartLine(), "lookup for super call is " + method.getSharedMethodInfo().getSourceSection());

        unmodifiedAssumption = declaringModule.getUnmodifiedAssumption();
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        notDesignedForCompilation();

        final RubyContext context = getContext();

        try {
            final RubyBasicObject self = context.getCoreLibrary().box(RubyArguments.getSelf(frame.getArguments()));

            if (!guard()) {
                lookup();
            }

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
