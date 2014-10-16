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
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyModule;
import org.jruby.truffle.runtime.methods.RubyMethod;

public abstract class AbstractGeneralSuperCallNode extends RubyNode {

    private final String name;

    @Child protected DirectCallNode callNode;

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

        final RubyModule declaringModule = RubyCallStack.getCurrentMethod().getDeclaringModule();

        if (!(declaringModule instanceof RubyClass)) {
            method = null;
            throw new RaiseException(getContext().getCoreLibrary().nameErrorNoMethod(name, "wasn't a class", this));
        }

        assert declaringModule instanceof RubyClass;
        method = ModuleOperations.lookupMethod(((RubyClass) declaringModule).getSuperClass(), name);

        if (method == null || method.isUndefined()) {
            method = null;
            throw new RaiseException(getContext().getCoreLibrary().nameErrorNoMethod(name, "no such method", this));
        }

        final DirectCallNode newCallNode = Truffle.getRuntime().createDirectCallNode(method.getCallTarget());

        if (callNode == null) {
            callNode = newCallNode;
            adoptChildren();
        } else {
            callNode.replace(newCallNode);
        }

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

            if (method == null || method.isUndefined() || !method.isVisibleTo(this, self)) {
                return getContext().getCoreLibrary().getNilObject();
            } else {
                return context.makeString("super");
            }
        } catch (Exception e) {
            return getContext().getCoreLibrary().getNilObject();
        }
    }

}
