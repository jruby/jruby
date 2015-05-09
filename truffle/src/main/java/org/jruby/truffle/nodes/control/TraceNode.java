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
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBinding;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.core.RubyString;

public class TraceNode extends RubyNode {

    private final RubyContext context;

    @CompilerDirectives.CompilationFinal private Assumption traceAssumption;
    @CompilerDirectives.CompilationFinal private RubyProc traceFunc;
    @Child private DirectCallNode callNode;

    private final RubyString event;
    private final RubyString file;
    private final int line;

    public TraceNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
        this.context = context;
        traceAssumption = context.getTraceManager().getTraceAssumption();
        traceFunc = null;
        callNode = null;
        event = context.makeString("line");
        file = context.makeString(sourceSection.getSource().getName());
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
                callNode = insert(Truffle.getRuntime().createDirectCallNode(traceFunc.getCallTargetForBlocks()));
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
                        new RubyBinding(context.getCoreLibrary().getBindingClass(), RubyArguments.getSelf(frame.getArguments()), frame.materialize()),
                        context.getCoreLibrary().getNilObject()
                };

                try {
                    callNode.call(frame, RubyArguments.pack(traceFunc.getMethod(), traceFunc.getDeclarationFrame(), traceFunc.getSelfCapturedInScope(), traceFunc.getBlockCapturedInScope(), args));
                } finally {
                    context.getTraceManager().setInTraceFunc(false);
                }
            }
        }
    }

}
