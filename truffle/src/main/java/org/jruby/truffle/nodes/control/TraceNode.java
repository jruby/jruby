/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.control;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.BindingNodes;
import org.jruby.truffle.nodes.core.ProcNodes;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;

public class TraceNode extends RubyNode {

    private final RubyContext context;

    @CompilationFinal private Assumption traceAssumption;
    @CompilationFinal private RubyBasicObject traceFunc;
    @Child private DirectCallNode callNode;

    private final RubyBasicObject event;
    private final RubyBasicObject file;
    private final int line;

    public TraceNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
        this.context = context;
        traceAssumption = context.getTraceManager().getTraceAssumption();
        traceFunc = null;
        callNode = null;
        event = createString("line");
        file = createString(sourceSection.getSource().getName());
        line = sourceSection.getStartLine();
    }

    @Override
    public Object execute(VirtualFrame frame) {
        throw new UnsupportedOperationException();
    }


    @Override
    public void executeVoid(VirtualFrame frame) {
        trace(frame);
    }

    public void trace(VirtualFrame frame) {
        try {
            traceAssumption.check();
        } catch (InvalidAssumptionException e) {
            traceAssumption = context.getTraceManager().getTraceAssumption();
            traceFunc = context.getTraceManager().getTraceFunc();

            if (traceFunc != null) {
                callNode = insert(Truffle.getRuntime().createDirectCallNode(ProcNodes.getCallTargetForBlocks(traceFunc)));
            } else {
                callNode = null;
            }
        }

        if (traceFunc != null) {
            if (!context.getTraceManager().isInTraceFunc()) {
                context.getTraceManager().setInTraceFunc(true);

                final Object[] args = new Object[]{
                        event,
                        file,
                        line,
                        context.getCoreLibrary().getNilObject(),
                        BindingNodes.createRubyBinding(context.getCoreLibrary().getBindingClass(), RubyArguments.getSelf(frame.getArguments()), frame.materialize()),
                        context.getCoreLibrary().getNilObject()
                };

                try {
                    callNode.call(frame, RubyArguments.pack(ProcNodes.getMethod(traceFunc), ProcNodes.getDeclarationFrame(traceFunc), ProcNodes.getSelfCapturedInScope(traceFunc), ProcNodes.getBlockCapturedInScope(traceFunc), args));
                } finally {
                    context.getTraceManager().setInTraceFunc(false);
                }
            }
        }
    }

}
