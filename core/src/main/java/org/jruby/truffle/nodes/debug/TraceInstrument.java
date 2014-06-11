/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.debug;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrument.Instrument;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.runtime.NilPlaceholder;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBinding;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.core.RubyString;

public class TraceInstrument extends Instrument {

    private final RubyContext context;

    @CompilerDirectives.CompilationFinal private Assumption traceAssumption;
    @CompilerDirectives.CompilationFinal private RubyProc traceFunc;
    @Child protected DirectCallNode callNode;

    private final RubyString event;
    private final RubyString file;
    private final int line;

    public TraceInstrument(RubyContext context, SourceSection sourceSection) {
        this.context = context;
        traceAssumption = context.getTraceManager().getTraceAssumption();
        traceFunc = null;
        callNode = null;
        event = context.makeString("line");
        file = context.makeString(sourceSection.getSource().getName());
        line = sourceSection.getStartLine();
    }

    @Override
    public void enter(Node node, VirtualFrame frame) {
        try {
            traceAssumption.check();

            if (traceFunc != null) {
                if (!context.getTraceManager().isInTraceFunc()) {
                    context.getTraceManager().setInTraceFunc(true);

                    try {
                        final Object[] args = new Object[] {
                                event,
                                file,
                                line,
                                NilPlaceholder.INSTANCE,
                                new RubyBinding(context.getCoreLibrary().getBindingClass(), RubyArguments.getSelf(frame.getArguments()), frame.materialize()),
                                NilPlaceholder.INSTANCE
                        };

                        callNode.call(frame, RubyArguments.pack(traceFunc.getMethod().getDeclarationFrame(), traceFunc.getSelfCapturedInScope(), traceFunc.getBlockCapturedInScope(), args));
                    } finally {
                        context.getTraceManager().setInTraceFunc(false);
                    }
                }
            }
        } catch (InvalidAssumptionException e) {
            traceAssumption = context.getTraceManager().getTraceAssumption();
            traceFunc = context.getTraceManager().getTraceFunc();

            if (traceFunc != null) {
                callNode = insert(Truffle.getRuntime().createDirectCallNode(traceFunc.getMethod().getCallTarget()));
            } else {
                callNode = null;
            }
        }
    }

}
