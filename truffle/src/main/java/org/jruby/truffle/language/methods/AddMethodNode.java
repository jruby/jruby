/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.methods;

import org.jruby.runtime.Visibility;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.objects.SingletonClassNode;
import org.jruby.truffle.language.objects.SingletonClassNodeGen;
import org.jruby.truffle.runtime.ModuleOperations;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.layouts.Layouts;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;

@NodeChildren({
        @NodeChild("moduleNode"),
        @NodeChild("methodNode"),
        @NodeChild("visibilityNode")
})
public abstract class AddMethodNode extends RubyNode {

    private final boolean ignoreNameVisibility; // def expr.meth()
    private final boolean isLiteralDef;

    @Child private SingletonClassNode singletonClassNode;

    public AddMethodNode(RubyContext context, SourceSection sourceSection, boolean ignoreNameVisibility, boolean isLiteralDef) {
        super(context, sourceSection);
        this.ignoreNameVisibility = ignoreNameVisibility;
        this.isLiteralDef = isLiteralDef;
        this.singletonClassNode = SingletonClassNodeGen.create(context, sourceSection, null);
    }

    public abstract DynamicObject executeAddMethod(DynamicObject module, InternalMethod method, Visibility visibility);

    @TruffleBoundary
    @Specialization(guards = "isRubyModule(module)")
    public DynamicObject addMethod(DynamicObject module, InternalMethod method, Visibility visibility) {
        if (!ignoreNameVisibility && ModuleOperations.isMethodPrivateFromName(method.getName())) {
            visibility = Visibility.PRIVATE;
        }

        method = method.withVisibility(visibility);

        if (isLiteralDef) {
            method = method.withDeclaringModule(module);
        }

        if (visibility == Visibility.MODULE_FUNCTION) {
            addMethodToModule(module, method.withVisibility(Visibility.PRIVATE));
            final DynamicObject singletonClass = singletonClassNode.executeSingletonClass(module);
            addMethodToModule(singletonClass, method.withDeclaringModule(singletonClass).withVisibility(Visibility.PUBLIC));
        } else {
            addMethodToModule(module, method);
        }

        return getSymbol(method.getName());
    }

    public void addMethodToModule(final DynamicObject module, InternalMethod method) {
        Layouts.MODULE.getFields(module).addMethod(getContext(), this, method);
    }

}
