/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.methods;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.objects.ObjectGraphNode;

import java.util.HashSet;
import java.util.Set;

/**
 * A Ruby method: either a method in a module,
 * a literal module/class body
 * or some meta-information for eval'd code.
 *
 * Blocks capture the method in which they are defined.
 */
public class InternalMethod implements ObjectGraphNode {

    private final SharedMethodInfo sharedMethodInfo;
    private final String name;

    private final DynamicObject declaringModule;
    private final Visibility visibility;
    private final boolean undefined;
    private final DynamicObject proc; // only if method is created from a Proc

    private final CallTarget callTarget;
    private final DynamicObject capturedBlock;
    private final DynamicObject capturedDefaultDefinee;

    public static InternalMethod fromProc(SharedMethodInfo sharedMethodInfo, String name, DynamicObject declaringModule,
            Visibility visibility, DynamicObject proc, CallTarget callTarget) {
        return new InternalMethod(sharedMethodInfo, name, declaringModule, visibility, false, proc, callTarget, Layouts.PROC.getBlock(proc), null);
    }

    public InternalMethod(SharedMethodInfo sharedMethodInfo, String name, DynamicObject declaringModule,
            Visibility visibility, CallTarget callTarget) {
        this(sharedMethodInfo, name, declaringModule, visibility, false, null, callTarget, null, null);
    }
    public InternalMethod(SharedMethodInfo sharedMethodInfo, String name, DynamicObject declaringModule,
                          Visibility visibility, boolean undefined, DynamicObject proc, CallTarget callTarget) {
        this(sharedMethodInfo, name, declaringModule, visibility, undefined, proc, callTarget, null, null);
    }

    public InternalMethod(SharedMethodInfo sharedMethodInfo, String name, DynamicObject declaringModule,
                          Visibility visibility, boolean undefined, DynamicObject proc, CallTarget callTarget, DynamicObject capturedBlock, DynamicObject capturedDefaultDefinee) {
        assert RubyGuards.isRubyModule(declaringModule);
        this.sharedMethodInfo = sharedMethodInfo;
        this.declaringModule = declaringModule;
        this.name = name;
        this.visibility = visibility;
        this.undefined = undefined;
        this.proc = proc;
        this.callTarget = callTarget;
        this.capturedBlock = capturedBlock;
        this.capturedDefaultDefinee = capturedDefaultDefinee;
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

    public CallTarget getCallTarget(){
        return callTarget;
    }

    public InternalMethod withDeclaringModule(DynamicObject newDeclaringModule) {
        assert RubyGuards.isRubyModule(newDeclaringModule);

        if (newDeclaringModule == declaringModule) {
            return this;
        } else {
            return new InternalMethod(sharedMethodInfo, name, newDeclaringModule, visibility, undefined, proc, callTarget, capturedBlock, capturedDefaultDefinee);
        }
    }

    public InternalMethod withName(String newName) {
        if (newName.equals(name)) {
            return this;
        } else {
            return new InternalMethod(sharedMethodInfo, newName, declaringModule, visibility, undefined, proc, callTarget, capturedBlock, capturedDefaultDefinee);
        }
    }

    public InternalMethod withVisibility(Visibility newVisibility) {
        if (newVisibility == visibility) {
            return this;
        } else {
            return new InternalMethod(sharedMethodInfo, name, declaringModule, newVisibility, undefined, proc, callTarget, capturedBlock, capturedDefaultDefinee);
        }
    }

    public InternalMethod undefined() {
        return new InternalMethod(sharedMethodInfo, name, declaringModule, visibility, true, proc, callTarget, capturedBlock, capturedDefaultDefinee);
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

    @Override
    public Set<DynamicObject> getAdjacentObjects() {
        final Set<DynamicObject> adjacent = new HashSet<>();

        if (declaringModule  != null) {
            adjacent.add(declaringModule);
        }

        if (proc != null) {
            adjacent.add(proc);
        }

        return adjacent;
    }

    public DynamicObject getCapturedBlock() {
        return capturedBlock;
    }

    public DynamicObject getCapturedDefaultDefinee() {
        return capturedDefaultDefinee;
    }
}
