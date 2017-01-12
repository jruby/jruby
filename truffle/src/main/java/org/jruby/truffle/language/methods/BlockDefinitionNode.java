/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.methods;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.core.proc.ProcOperations;
import org.jruby.truffle.core.proc.ProcType;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.control.BreakID;
import org.jruby.truffle.language.control.FrameOnStackMarker;
import org.jruby.truffle.language.locals.ReadFrameSlotNode;
import org.jruby.truffle.language.locals.ReadFrameSlotNodeGen;

/**
 * Create a Ruby Proc to pass as a block to the called method. The literal block is represented as
 * call targets and a SharedMethodInfo. This is executed at the call site just before dispatch.
 */
public class BlockDefinitionNode extends RubyNode {

    private final ProcType type;
    private final SharedMethodInfo sharedMethodInfo;

    // TODO(CS, 10-Jan-15) having two call targets isn't ideal, but they all have different semantics, and we don't
    // want to move logic into the call site

    private final CallTarget callTargetForProcs;
    private final CallTarget callTargetForLambdas;

    private final BreakID breakID;

    @Child private ReadFrameSlotNode readFrameOnStackMarkerNode;

    public BlockDefinitionNode(ProcType type, SharedMethodInfo sharedMethodInfo,
                               CallTarget callTargetForProcs, CallTarget callTargetForLambdas, BreakID breakID, FrameSlot frameOnStackMarkerSlot) {
        this.type = type;
        this.sharedMethodInfo = sharedMethodInfo;

        this.callTargetForProcs = callTargetForProcs;
        this.callTargetForLambdas = callTargetForLambdas;
        this.breakID = breakID;

        if (frameOnStackMarkerSlot == null) {
            readFrameOnStackMarkerNode = null;
        } else {
            readFrameOnStackMarkerNode = ReadFrameSlotNodeGen.create(frameOnStackMarkerSlot);
        }
    }

    public BreakID getBreakID() {
        return breakID;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final FrameOnStackMarker frameOnStackMarker;

        if (readFrameOnStackMarkerNode == null) {
            frameOnStackMarker = null;
        } else {
            final Object frameOnStackMarkerValue = readFrameOnStackMarkerNode.executeRead(frame);

            if (frameOnStackMarkerValue instanceof FrameOnStackMarker) {
                frameOnStackMarker = (FrameOnStackMarker) frameOnStackMarkerValue;
            } else {
                frameOnStackMarker = null;
            }
        }

        return ProcOperations.createRubyProc(coreLibrary().getProcFactory(), type, sharedMethodInfo,
                callTargetForProcs, callTargetForLambdas, frame.materialize(),
                RubyArguments.getMethod(frame),
                RubyArguments.getSelf(frame),
                RubyArguments.getBlock(frame),
                frameOnStackMarker);
    }

}
