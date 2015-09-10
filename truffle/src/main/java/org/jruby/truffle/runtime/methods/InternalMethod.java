/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.methods;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.runtime.layouts.Layouts;

/**
 * Any kind of Ruby method - so normal methods in classes and modules, but also blocks, procs,
 * lambdas and native methods written in Java.
 */
public class InternalMethod {

    private final SharedMethodInfo sharedMethodInfo;
    private final String name;

    private final DynamicObject declaringModule;
    private final Visibility visibility;
    private final boolean undefined;

    private final CallTarget callTarget;
    private final MaterializedFrame declarationFrame;

    public InternalMethod(SharedMethodInfo sharedMethodInfo, String name,
                          DynamicObject declaringModule, Visibility visibility, boolean undefined,
                          CallTarget callTarget, MaterializedFrame declarationFrame) {
        assert declaringModule == null || RubyGuards.isRubyModule(declaringModule);
        this.sharedMethodInfo = sharedMethodInfo;
        this.declaringModule = declaringModule;
        this.name = name;
        this.visibility = visibility;
        this.undefined = undefined;
        this.callTarget = callTarget;
        this.declarationFrame = declarationFrame;
    }

    public SharedMethodInfo getSharedMethodInfo() {
        return sharedMethodInfo;
    }

    public DynamicObject getDeclaringModule() {
        return declaringModule;
    }

    public String getName() {
        return name;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public boolean isUndefined() {
        return undefined;
    }

    public MaterializedFrame getDeclarationFrame() {
        return declarationFrame;
    }

    public CallTarget getCallTarget(){
        return callTarget;
    }

    public InternalMethod withDeclaringModule(DynamicObject newDeclaringModule) {
        assert RubyGuards.isRubyModule(newDeclaringModule);

        if (newDeclaringModule == declaringModule) {
            return this;
        } else {
            return new InternalMethod(sharedMethodInfo, name, newDeclaringModule, visibility, undefined, callTarget, declarationFrame);
        }
    }

    public InternalMethod withName(String newName) {
        if (newName.equals(name)) {
            return this;
        } else {
            return new InternalMethod(sharedMethodInfo, newName, declaringModule, visibility, undefined, callTarget, declarationFrame);
        }
    }

    public InternalMethod withVisibility(Visibility newVisibility) {
        if (newVisibility == visibility) {
            return this;
        } else {
            return new InternalMethod(sharedMethodInfo, name, declaringModule, newVisibility, undefined, callTarget, declarationFrame);
        }
    }

    public InternalMethod undefined() {
        return new InternalMethod(sharedMethodInfo, name, declaringModule, visibility, true, callTarget, declarationFrame);
    }

    public boolean isVisibleTo(Node currentNode, DynamicObject callerClass) {
        assert RubyGuards.isRubyClass(callerClass);

        switch (visibility) {
            case PUBLIC:
                return true;

            case PROTECTED:
                for (DynamicObject ancestor : Layouts.MODULE.getFields(callerClass).ancestors()) {
                    if (ancestor == declaringModule || Layouts.BASIC_OBJECT.getMetaClass(ancestor) == declaringModule) {
                        return true;
                    }
                }

                return false;

            case PRIVATE:
                // A private method may only be called with an implicit receiver,
                // in which case the visibility must not be checked.
                return false;

            default:
                throw new UnsupportedOperationException(visibility.name());
        }
    }

    @Override
    public String toString() {
        return sharedMethodInfo.toString();
    }

}
