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
import com.oracle.truffle.api.SourceSection;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.NilPlaceholder;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.*;

public class TraceNode extends WrapperNode {

    @CompilerDirectives.CompilationFinal private Assumption traceAssumption;
    @CompilerDirectives.CompilationFinal private RubyProc traceFunc;
    @Child protected DirectCallNode callNode;

    private final RubyString event;
    private final RubyString file;
    private final int line;

    public TraceNode(RubyContext context, SourceSection sourceSection, RubyNode child) {
        super(context, sourceSection, child);
        traceAssumption = context.getTraceManager().getTraceAssumption();
        traceFunc = null;
        callNode = null;
        event = context.makeString("line");
        file = context.makeString(sourceSection.getSource().getName());
        line = sourceSection.getStartLine();
    }

    @Override
    public void before(VirtualFrame frame) {
        try {
            traceAssumption.check();
        } catch (InvalidAssumptionException e) {

            traceAssumption = getContext().getTraceManager().getTraceAssumption();
            traceFunc = getContext().getTraceManager().getTraceFunc();

            if (traceFunc != null) {
                callNode = insert(Truffle.getRuntime().createDirectCallNode(traceFunc.getMethod().getCallTarget()));
            } else {
                callNode = null;
            }
        }

        if (traceFunc != null) {
            if (!getContext().getTraceManager().isInTraceFunc()) {
                getContext().getTraceManager().setInTraceFunc(true);

                final Object[] args = new Object[]{
                        event,
                        file,
                        line,
                        NilPlaceholder.INSTANCE,
                        new RubyBinding(getContext().getCoreLibrary().getBindingClass(), RubyArguments.getSelf(frame.getArguments()), frame.materialize()),
                        NilPlaceholder.INSTANCE
                };

                try {
                    callNode.call(frame, RubyArguments.pack(traceFunc.getMethod().getDeclarationFrame(), traceFunc.getSelfCapturedInScope(), traceFunc.getBlockCapturedInScope(), args));
                } finally {
                    getContext().getTraceManager().setInTraceFunc(false);
                }
            }
        }
    }


}
