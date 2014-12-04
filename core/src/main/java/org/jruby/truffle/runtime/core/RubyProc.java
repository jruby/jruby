/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.MaterializedFrame;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.methods.*;
import org.jruby.truffle.runtime.subsystems.ObjectSpaceManager;
import org.jruby.util.cli.Options;

/**
 * Represents the Ruby {@code Proc} class.
 */
public class RubyProc extends RubyBasicObject implements MethodLike {

    public static final boolean PROC_BINDING = Options.TRUFFLE_PROC_BINDING.load();

    /**
     * The class from which we create the object that is {@code Proc}. A subclass of
     * {@link RubyClass} so that we can override {@link RubyClass#newInstance} and allocate a
     * {@link RubyProc} rather than a normal {@link RubyBasicObject}.
     */
    public static class RubyProcClass extends RubyClass {

        public RubyProcClass(RubyContext context, RubyClass objectClass) {
            super(context, objectClass, objectClass, "Proc");
        }

        @Override
        public RubyBasicObject newInstance(RubyNode currentNode) {
            return new RubyProc(this, Type.PROC);
        }

    }

    public static enum Type {
        PROC, LAMBDA
    }

    private final Type type;
    @CompilationFinal private SharedMethodInfo sharedMethodInfo;
    /** Call target for procs, which have special arguments destructuring */
    @CompilationFinal private CallTarget callTarget;
    /** Call target for lambdas and methods, which have strict arguments destructuring */
    @CompilationFinal private CallTarget callTargetForMethods;
    @CompilationFinal private MaterializedFrame declarationFrame;
    @CompilationFinal private RubyModule declaringModule;
    @CompilationFinal private MethodLike method;
    @CompilationFinal private Object self;
    @CompilationFinal private RubyProc block;

    public RubyProc(RubyClass procClass, Type type) {
        super(procClass);
        this.type = type;
    }

    public RubyProc(RubyClass procClass, Type type, SharedMethodInfo sharedMethodInfo, CallTarget callTarget,
                    CallTarget callTargetForMethods, MaterializedFrame declarationFrame, RubyModule declaringModule, MethodLike method, Object self, RubyProc block) {
        this(procClass, type);
        initialize(sharedMethodInfo, callTarget, callTargetForMethods, declarationFrame, declaringModule, method, self, block);
    }

    public void initialize(SharedMethodInfo sharedMethodInfo, CallTarget callTarget, CallTarget callTargetForMethods,
                           MaterializedFrame declarationFrame, RubyModule declaringModule, MethodLike method, Object self, RubyProc block) {
        this.sharedMethodInfo = sharedMethodInfo;
        this.callTarget = callTarget;
        this.callTargetForMethods = callTargetForMethods;
        this.declarationFrame = declarationFrame;
        this.declaringModule = declaringModule;
        this.method = method;
        this.self = self;
        this.block = block;
    }

    public CallTarget getCallTargetForType() {
        switch (type) {
            case PROC:
                return callTarget;
            case LAMBDA:
                return callTargetForMethods;
        }

        throw new UnsupportedOperationException(type.toString());
    }

    public Object rootCall(Object... args) {
        RubyNode.notDesignedForCompilation();

        // TODO(CS): handle exceptions in here?

        return getCallTargetForType().call(RubyArguments.pack(this, declarationFrame, self, block, args));
    }

    public Type getType() {
        return type;
    }

    public SharedMethodInfo getSharedMethodInfo() {
        return sharedMethodInfo;
    }

    public CallTarget getCallTarget() {
        return callTarget;
    }

    public CallTarget getCallTargetForMethods() {
        return callTargetForMethods;
    }

    public MaterializedFrame getDeclarationFrame() {
        return declarationFrame;
    }

    @Override
    public RubyModule getDeclaringModule() {
        return declaringModule;
    }

    public MethodLike getMethod() {
        return method;
    }

    public Object getSelfCapturedInScope() {
        return self;
    }

    public RubyProc getBlockCapturedInScope() {
        return block;
    }

    @Override
    public void visitObjectGraphChildren(ObjectSpaceManager.ObjectGraphVisitor visitor) {
        getContext().getObjectSpaceManager().visitFrame(declarationFrame, visitor);
    }

}
