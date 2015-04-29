/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
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
import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.methods.InternalMethod;
import org.jruby.truffle.runtime.methods.SharedMethodInfo;
import org.jruby.truffle.runtime.subsystems.ObjectSpaceManager;

/**
 * Represents the Ruby {@code Proc} class.
 */
public class RubyProc extends RubyBasicObject {

    public static enum Type {
        BLOCK, PROC, LAMBDA
    }

    private final Type type;
    @CompilationFinal private SharedMethodInfo sharedMethodInfo;
    /** Call target for blocks, which have special arguments destructuring */
    @CompilationFinal private CallTarget callTargetForBlocks;
    /** Call target for actual Proc arguments, which handle break differently */
    @CompilationFinal private CallTarget callTargetForProcs;
    /** Call target for lambdas and methods, which have strict arguments destructuring */
    @CompilationFinal private CallTarget callTargetForMethods;
    @CompilationFinal private MaterializedFrame declarationFrame;
    /** The method which defined the block, that is the lexically enclosing method.
     * Notably used by super to figure out in which method we were. */
    @CompilationFinal private InternalMethod method;
    @CompilationFinal private Object self;
    @CompilationFinal private RubyProc block;

    public RubyProc(RubyClass procClass, Type type) {
        super(procClass);
        this.type = type;
    }

    public RubyProc(RubyClass procClass, Type type, SharedMethodInfo sharedMethodInfo, CallTarget callTargetForBlocks,
                    CallTarget callTargetForProcs, CallTarget callTargetForMethods, MaterializedFrame declarationFrame,
                    InternalMethod method, Object self, RubyProc block) {
        this(procClass, type);
        initialize(sharedMethodInfo, callTargetForBlocks, callTargetForProcs, callTargetForMethods, declarationFrame,
                method, self, block);
    }

    public void initialize(SharedMethodInfo sharedMethodInfo, CallTarget callTargetForBlocks, CallTarget callTargetForProcs,
                           CallTarget callTargetForMethods, MaterializedFrame declarationFrame, InternalMethod method,
                           Object self, RubyProc block) {
        this.sharedMethodInfo = sharedMethodInfo;
        this.callTargetForBlocks = callTargetForBlocks;
        this.callTargetForProcs = callTargetForProcs;
        this.callTargetForMethods = callTargetForMethods;
        this.declarationFrame = declarationFrame;
        this.method = method;
        this.self = self;
        this.block = block;
    }

    public CallTarget getCallTargetForType() {
        switch (type) {
            case BLOCK:
                return callTargetForBlocks;
            case PROC:
                return callTargetForProcs;
            case LAMBDA:
                return callTargetForMethods;
        }

        throw new UnsupportedOperationException(type.toString());
    }

    public Object rootCall(Object... args) {
        RubyNode.notDesignedForCompilation();

        // TODO(CS): handle exceptions in here?

        return getCallTargetForType().call(RubyArguments.pack(getMethod(), declarationFrame, self, block, args));
    }

    public Type getType() {
        return type;
    }

    public SharedMethodInfo getSharedMethodInfo() {
        return sharedMethodInfo;
    }

    public CallTarget getCallTargetForBlocks() {
        return callTargetForBlocks;
    }

    public CallTarget getCallTargetForProcs() {
        return callTargetForProcs;
    }

    public CallTarget getCallTargetForMethods() {
        return callTargetForMethods;
    }

    public MaterializedFrame getDeclarationFrame() {
        return declarationFrame;
    }

    public InternalMethod getMethod() {
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

    public static class ProcAllocator implements Allocator {

        @Override
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, Node currentNode) {
            return new RubyProc(rubyClass, Type.PROC);
        }

    }

}
