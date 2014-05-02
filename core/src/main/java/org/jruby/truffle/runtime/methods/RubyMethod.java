/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.methods;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.NodeUtil;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.InlinableMethodImplementation;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.nodes.methods.arguments.BehaveAsBlockNode;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.*;

/**
 * Any kind of Ruby method - so normal methods in classes and modules, but also blocks, procs,
 * lambdas and native methods written in Java.
 */
public class RubyMethod {

    private final SharedRubyMethod sharedMethodInfo;
    private final RubyModule declaringModule;
    private final String name;
    private final Visibility visibility;
    private final boolean undefined;

    private final MethodImplementation implementation;

    public RubyMethod(SharedRubyMethod sharedMethodInfo, RubyModule declaringModule, String name, Visibility visibility, boolean undefined,
                    MethodImplementation implementation) {
        this.sharedMethodInfo = sharedMethodInfo;
        this.declaringModule = declaringModule;
        this.name = name;
        this.visibility = visibility;
        this.undefined = undefined;
        this.implementation = implementation;
    }

    public Object call(PackedFrame caller, Object self, RubyProc block, Object... args) {
        assert RubyContext.shouldObjectBeVisible(self);
        assert RubyContext.shouldObjectsBeVisible(args);

        final Object result = implementation.call(caller, self, block, args);

        assert RubyContext.shouldObjectBeVisible(result);

        return result;
    }

    public SharedRubyMethod getSharedMethodInfo() {
        return sharedMethodInfo;
    }

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

    public MethodImplementation getImplementation() {
        return implementation;
    }

    public RubyMethod withDeclaringModule(RubyModule newDeclaringModule) {
        if (newDeclaringModule == declaringModule) {
            return this;
        }

        return new RubyMethod(sharedMethodInfo, newDeclaringModule, name, visibility, undefined, implementation);
    }

    public RubyMethod withNewName(String newName) {
        if (newName.equals(name)) {
            return this;
        }

        return new RubyMethod(sharedMethodInfo, declaringModule, newName, visibility, undefined, implementation);
    }

    public RubyMethod withNewVisibility(Visibility newVisibility) {
        if (newVisibility == visibility) {
            return this;
        }

        return new RubyMethod(sharedMethodInfo, declaringModule, name, newVisibility, undefined, implementation);
    }

    public RubyMethod withoutBlockDestructureSemantics() {
        final InlinableMethodImplementation inlinableMethodImplementation = (InlinableMethodImplementation) implementation;

        final RubyRootNode modifiedRootNode = inlinableMethodImplementation.getCloneOfPristineRootNode();

        for (BehaveAsBlockNode behaveAsBlockNode : NodeUtil.findAllNodeInstances(modifiedRootNode, BehaveAsBlockNode.class)) {
            behaveAsBlockNode.setBehaveAsBlock(false);
        }

        final InlinableMethodImplementation newImplementation = new InlinableMethodImplementation(
                Truffle.getRuntime().createCallTarget(modifiedRootNode),
                inlinableMethodImplementation.getDeclarationFrame(),
                inlinableMethodImplementation.getFrameDescriptor(),
                modifiedRootNode,
                inlinableMethodImplementation.alwaysInline(),
                inlinableMethodImplementation.getShouldAppendCallNode());

        return new RubyMethod(sharedMethodInfo, declaringModule, name, visibility, undefined, newImplementation);
    }

    public RubyMethod undefined() {
        if (undefined) {
            return this;
        }

        return new RubyMethod(sharedMethodInfo, declaringModule, name, visibility, true, implementation);
    }

    public boolean isVisibleTo(RubyBasicObject caller, RubyBasicObject receiver) {
        if (caller == receiver.getRubyClass()){
            return true;
        }

        if (caller == receiver){
            return true;
        }
        return isVisibleTo(caller);
    }

    public boolean isVisibleTo(RubyBasicObject caller) {
        if (caller instanceof RubyModule) {
            if (isVisibleTo((RubyModule) caller)) {
                return true;
            }
        }

        if (isVisibleTo(caller.getRubyClass())) {
            return true;
        }

        if (isVisibleTo(caller.getSingletonClass())) {
            return true;
        }

        return false;
    }

    private boolean isVisibleTo(RubyModule module) {
        switch (visibility) {
            case PUBLIC:
                return true;

            case PROTECTED:
                if (module == declaringModule) {
                    return true;
                }

                if (module.getSingletonClass() == declaringModule) {
                    return true;
                }

                if (module.getParentModule() != null && isVisibleTo(module.getParentModule())) {
                    return true;
                }

                return false;

            case PRIVATE:
                if (module == declaringModule) {
                    return true;
                }

                if (module.getSingletonClass() == declaringModule) {
                    return true;
                }

                if (module.getParentModule() != null && isVisibleTo(module.getParentModule())) {
                    return true;
                }

                return false;

            default:
                return false;
        }
    }

}
