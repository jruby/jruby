/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
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
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.ModuleOperations;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyCallStack;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyModule;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.methods.MethodLike;
import org.jruby.truffle.runtime.methods.InternalMethod;

public abstract class AbstractGeneralSuperCallNode extends RubyNode {

    @Child protected DirectCallNode callNode;

    @CompilerDirectives.CompilationFinal protected Assumption unmodifiedAssumption;
    @CompilerDirectives.CompilationFinal protected InternalMethod method;

    public AbstractGeneralSuperCallNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    protected boolean guard() {
        // TODO(CS): not sure this is enough... lots of 'unspecified' behaviour in the ISO spec here
        return method != null && unmodifiedAssumption != null && unmodifiedAssumption.isValid();
    }

    protected void lookup(VirtualFrame frame) {
        CompilerAsserts.neverPartOfCompilation();

        final FrameInstance currentFrame = Truffle.getRuntime().getCurrentFrame();
        MethodLike methodLike = RubyCallStack.getMethod(currentFrame);

        while (!(methodLike instanceof InternalMethod)) {
            methodLike = ((RubyProc) methodLike).getMethod();
        }

        final String name = ((InternalMethod) methodLike).getName();

        // TODO: this is wrong, we need the lexically enclosing method (or define_method)'s module
        final RubyModule declaringModule = RubyCallStack.getCurrentDeclaringModule();
        final RubyClass selfMetaClass = getContext().getCoreLibrary().getMetaClass(RubyArguments.getSelf(frame.getArguments()));

        method = ModuleOperations.lookupSuperMethod(declaringModule, name, selfMetaClass);

        if (method == null || method.isUndefined()) {
            method = null;
            // TODO: should add " for #{receiver.inspect}" in error message
            throw new RaiseException(getContext().getCoreLibrary().noMethodError(String.format("super: no superclass method `%s'", name), this));
        }

        final DirectCallNode newCallNode = Truffle.getRuntime().createDirectCallNode(method.getCallTarget());

        if (callNode == null) {
            callNode = insert(newCallNode);
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
            final Object self = RubyArguments.getSelf(frame.getArguments());

            if (!guard()) {
                lookup(frame);
            }

            if (method == null || method.isUndefined() || !method.isVisibleTo(this, context.getCoreLibrary().getMetaClass(self))) {
                return getContext().getCoreLibrary().getNilObject();
            } else {
                return context.makeString("super");
            }
        } catch (Throwable t) {
            return getContext().getCoreLibrary().getNilObject();
        }
    }

}
