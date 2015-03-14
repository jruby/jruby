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

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.source.SourceSection;

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.objects.MetaClassNode;
import org.jruby.truffle.nodes.objects.MetaClassNodeFactory;
import org.jruby.truffle.runtime.ModuleOperations;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyCallStack;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyModule;
import org.jruby.truffle.runtime.methods.InternalMethod;

public abstract class AbstractGeneralSuperCallNode extends RubyNode {

    @Child protected MetaClassNode metaClassNode;
    @Child protected DirectCallNode callNode;

    @CompilerDirectives.CompilationFinal private InternalMethod currentMethod;
    @CompilerDirectives.CompilationFinal private RubyClass selfMetaClass;
    @CompilerDirectives.CompilationFinal private Assumption unmodifiedAssumption;
    @CompilerDirectives.CompilationFinal protected InternalMethod superMethod;

    public AbstractGeneralSuperCallNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
        metaClassNode = MetaClassNodeFactory.create(context, sourceSection, null);
    }

    protected boolean guard(VirtualFrame frame, Object self) {
        InternalMethod method = RubyArguments.getMethod(frame.getArguments());

        return superMethod != null &&
                method == currentMethod &&

                // This is overly restrictive, but seems the be the only reasonable check in term of performance.
                // The ideal condition would be to check if both ancestor lists starting at
                // the current method's module are identical, which is non-trivial
                // if the current method's module is an (included) module and not a class.
                metaClassNode.executeMetaClass(frame, self) == selfMetaClass &&
                unmodifiedAssumption.isValid();
    }

    protected void lookup(VirtualFrame frame) {
        lookup(frame, false);
    }

    private void lookup(VirtualFrame frame, boolean checkIfDefined) {
        CompilerAsserts.neverPartOfCompilation();

        currentMethod = RubyArguments.getMethod(frame.getArguments());

        String name = currentMethod.getSharedMethodInfo().getName(); // use the original name
        RubyModule declaringModule = currentMethod.getDeclaringModule();

        selfMetaClass = getContext().getCoreLibrary().getMetaClass(RubyArguments.getSelf(frame.getArguments()));

        superMethod = ModuleOperations.lookupSuperMethod(declaringModule, name, selfMetaClass);

        if (superMethod == null || superMethod.isUndefined()) {
            superMethod = null;
            if (checkIfDefined) {
                return;
            }
            // TODO: should add " for #{receiver.inspect}" in error message
            throw new RaiseException(getContext().getCoreLibrary().noMethodError(String.format("super: no superclass method `%s'", name), this));
        }

        unmodifiedAssumption = declaringModule.getUnmodifiedAssumption();

        final DirectCallNode newCallNode = Truffle.getRuntime().createDirectCallNode(superMethod.getCallTarget());

        if (callNode == null) {
            callNode = insert(newCallNode);
        } else {
            callNode.replace(newCallNode);
        }
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        notDesignedForCompilation();

        final Object self = RubyArguments.getSelf(frame.getArguments());

        if (!guard(frame, self)) {
            lookup(frame, true);
        }

        if (superMethod == null) {
            return nil();
        } else {
            return getContext().makeString("super");
        }
    }

}
