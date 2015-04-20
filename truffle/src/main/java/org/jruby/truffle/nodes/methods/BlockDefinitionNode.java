/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.methods;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyModule;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.methods.SharedMethodInfo;
import org.jruby.truffle.translator.TranslatorEnvironment.BlockID;

/**
 * Create a RubyProc to pass as a block to the called method.
 * The literal block is represented as call targets and a SharedMethodInfo.
 * This is executed at the call site just before dispatch.
 */
public class BlockDefinitionNode extends RubyNode {

    private final SharedMethodInfo sharedMethodInfo;

    // TODO(CS, 10-Jan-15) having three call targets isn't ideal, but they all have different semantics, and we don't
    // want to move logic into the call site

    private final CallTarget callTargetForBlocks;
    private final CallTarget callTargetForProcs;
    private final CallTarget callTargetForMethods;

    private final boolean requiresDeclarationFrame;
    private final BlockID blockID;

    public BlockDefinitionNode(RubyContext context, SourceSection sourceSection, SharedMethodInfo sharedMethodInfo,
                               boolean requiresDeclarationFrame, CallTarget callTargetForBlocks,
                               CallTarget callTargetForProcs, CallTarget callTargetForMethods, BlockID blockID) {
        super(context, sourceSection);
        this.sharedMethodInfo = sharedMethodInfo;

        this.callTargetForBlocks = callTargetForBlocks;
        this.callTargetForProcs = callTargetForProcs;
        this.callTargetForMethods = callTargetForMethods;

        this.requiresDeclarationFrame = requiresDeclarationFrame;
        this.blockID = blockID;
    }

    public BlockID getBlockID() {
        return blockID;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final MaterializedFrame declarationFrame;

        if (requiresDeclarationFrame) {
            declarationFrame = frame.materialize();
        } else {
            declarationFrame = null;
        }

        return new RubyProc(getContext().getCoreLibrary().getProcClass(), RubyProc.Type.PROC, sharedMethodInfo,
                callTargetForBlocks, callTargetForProcs, callTargetForMethods,
                declarationFrame,
                RubyArguments.getMethod(frame.getArguments()),
                RubyArguments.getSelf(frame.getArguments()),
                RubyArguments.getBlock(frame.getArguments()));
    }

}
