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
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.core.ProcNodes;
import org.jruby.truffle.runtime.methods.InternalMethod;
import org.jruby.truffle.runtime.methods.SharedMethodInfo;

/**
 * Represents the Ruby {@code Proc} class.
 */
@Deprecated
public class RubyProc extends RubyBasicObject {

    public final ProcNodes.Type type;
    @CompilationFinal public SharedMethodInfo sharedMethodInfo;
    /** Call target for blocks, which have special arguments destructuring */
    @CompilationFinal public CallTarget callTargetForBlocks;
    /** Call target for actual Proc arguments, which handle break differently */
    @CompilationFinal public CallTarget callTargetForProcs;
    /** Call target for lambdas and methods, which have strict arguments destructuring */
    @CompilationFinal public CallTarget callTargetForLambdas;
    @CompilationFinal public MaterializedFrame declarationFrame;
    /** The method which defined the block, that is the lexically enclosing method.
     * Notably used by super to figure out in which method we were. */
    @CompilationFinal public InternalMethod method;
    @CompilationFinal public Object self;
    @CompilationFinal public RubyBasicObject block;

    public RubyProc(RubyBasicObject procClass, ProcNodes.Type type) {
        super(procClass);
        this.type = type;
    }

    public RubyProc(RubyBasicObject procClass, ProcNodes.Type type, SharedMethodInfo sharedMethodInfo, CallTarget callTargetForBlocks,
                    CallTarget callTargetForProcs, CallTarget callTargetForLambdas, MaterializedFrame declarationFrame,
                    InternalMethod method, Object self, RubyBasicObject block) {
        this(procClass, type);
        assert block == null || RubyGuards.isRubyProc(block);
        ProcNodes.initialize(this, sharedMethodInfo, callTargetForBlocks, callTargetForProcs, callTargetForLambdas, declarationFrame,
                method, self, block);
    }

}
