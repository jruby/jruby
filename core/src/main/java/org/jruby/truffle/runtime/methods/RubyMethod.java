/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
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
import org.jruby.runtime.Visibility;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyModule;

/**
 * Any kind of Ruby method - so normal methods in classes and modules, but also blocks, procs,
 * lambdas and native methods written in Java.
 */
public class RubyMethod implements MethodLike {

    private final SharedMethodInfo sharedMethodInfo;
    private final String name;

    private final RubyModule declaringModule;
    private final Visibility visibility;
    private final boolean undefined;

    private final CallTarget callTarget;
    private final MaterializedFrame declarationFrame;

    public RubyMethod(SharedMethodInfo sharedMethodInfo, String name,
                      RubyModule declaringModule, Visibility visibility, boolean undefined,
                      CallTarget callTarget, MaterializedFrame declarationFrame) {
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

    @Override
    public RubyModule getDeclaringModule() { return declaringModule; }

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

    public RubyMethod withDeclaringModule(RubyModule newDeclaringModule) {
        if (newDeclaringModule == declaringModule) {
            return this;
        } else {
            return new RubyMethod(sharedMethodInfo, name, newDeclaringModule, visibility, undefined, callTarget, declarationFrame);
        }
    }

    public RubyMethod withNewName(String newName) {
        return new RubyMethod(sharedMethodInfo, newName, declaringModule, visibility, undefined, callTarget, declarationFrame);
    }

    public RubyMethod withVisibility(Visibility newVisibility) {
        if (newVisibility == visibility) {
            return this;
        } else {
            return new RubyMethod(sharedMethodInfo, name, declaringModule, newVisibility, undefined, callTarget, declarationFrame);
        }
    }

    public RubyMethod undefined() {
        return new RubyMethod(sharedMethodInfo, name, declaringModule, visibility, true, callTarget, declarationFrame);
    }

    public boolean isVisibleTo(Node currentNode, RubyClass callerClass) {
        switch (visibility) {
            case PUBLIC:
                return true;

            case PROTECTED:
                for (RubyModule ancestor : callerClass.ancestors()) {
                    if (ancestor == declaringModule || ancestor.getMetaClass() == declaringModule) {
                        return true;
                    }
                }

                return false;

            case PRIVATE:
                // A private method may only be called with an implicit receiver,
                // in which case the visibility must not be checked.
                return false;

            default:
                return false;
        }
    }

    @Override
    public String toString() {
        return sharedMethodInfo.toString();
    }

}
