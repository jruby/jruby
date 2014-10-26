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

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.MaterializedFrame;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.DebugOperations;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.methods.*;
import org.jruby.truffle.runtime.subsystems.ObjectSpaceManager;

/**
 * Represents the Ruby {@code Proc} class.
 */
public class RubyProc extends RubyObject implements MethodLike {

    /**
     * The class from which we create the object that is {@code Proc}. A subclass of
     * {@link RubyClass} so that we can override {@link RubyClass#newInstance} and allocate a
     * {@link RubyProc} rather than a normal {@link RubyBasicObject}.
     */
    public static class RubyProcClass extends RubyClass {

        public RubyProcClass(RubyClass objectClass) {
            super(null, null, objectClass, "Proc");
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
    @CompilationFinal private CallTarget callTarget;
    @CompilationFinal private CallTarget callTargetForMethods;
    @CompilationFinal private MaterializedFrame declarationFrame;
    @CompilationFinal private Object self;
    @CompilationFinal private RubyProc block;

    public RubyProc(RubyClass procClass, Type type) {
        super(procClass);
        this.type = type;
    }

    public RubyProc(RubyClass procClass, Type type, SharedMethodInfo sharedMethodInfo, CallTarget callTarget,
                    CallTarget callTargetForMethods, MaterializedFrame declarationFrame, Object self, RubyProc block) {
        this(procClass, type);
        initialize(sharedMethodInfo, callTarget, callTargetForMethods, declarationFrame, self, block);
    }

    public void initialize(SharedMethodInfo sharedMethodInfo, CallTarget callTarget, CallTarget callTargetForMethods,
                           MaterializedFrame declarationFrame, Object self, RubyProc block) {
        this.sharedMethodInfo = sharedMethodInfo;
        this.callTarget = callTarget;
        this.callTargetForMethods = callTargetForMethods;
        this.declarationFrame = declarationFrame;
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
