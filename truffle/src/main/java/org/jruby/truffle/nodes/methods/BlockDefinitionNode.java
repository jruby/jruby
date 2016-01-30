/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.methods;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.core.ProcNodes;
import org.jruby.truffle.core.ProcNodes.Type;
import org.jruby.truffle.language.locals.ReadFrameSlotNode;
import org.jruby.truffle.language.locals.ReadFrameSlotNodeGen;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.language.control.FrameOnStackMarker;
import org.jruby.truffle.runtime.methods.SharedMethodInfo;
import org.jruby.truffle.language.control.BreakID;

/**
 * Create a RubyProc to pass as a block to the called method.
 * The literal block is represented as call targets and a SharedMethodInfo.
 * This is executed at the call site just before dispatch.
 */
public class BlockDefinitionNode extends RubyNode {

    private final Type type;
    private final SharedMethodInfo sharedMethodInfo;

    // TODO(CS, 10-Jan-15) having two call targets isn't ideal, but they all have different semantics, and we don't
    // want to move logic into the call site

    private final CallTarget callTargetForProcs;
    private final CallTarget callTargetForLambdas;

    private final BreakID breakID;

    @Child private ReadFrameSlotNode readFrameOnStackMarkerNode;

    public BlockDefinitionNode(RubyContext context, SourceSection sourceSection, Type type, SharedMethodInfo sharedMethodInfo,
                               CallTarget callTargetForProcs, CallTarget callTargetForLambdas, BreakID breakID, FrameSlot frameOnStackMarkerSlot) {
        super(context, sourceSection);
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

        return ProcNodes.createRubyProc(getContext().getCoreLibrary().getProcFactory(), type, sharedMethodInfo,
                callTargetForProcs, callTargetForLambdas, frame.materialize(),
                RubyArguments.getMethod(frame.getArguments()),
                RubyArguments.getSelf(frame.getArguments()),
                RubyArguments.getBlock(frame.getArguments()),
                frameOnStackMarker);
    }

}
