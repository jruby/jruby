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
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.RubyString;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.util.StringSupport;

public class TraceNode extends RubyNode {

    private final RubyContext context;

    @CompilationFinal private Assumption traceAssumption;
    @CompilationFinal private DynamicObject traceFunc;
    @Child private DirectCallNode callNode;

    private final DynamicObject event;
    private final DynamicObject file;
    private final int line;

    public TraceNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
        this.context = context;
        traceAssumption = context.getTraceManager().getTraceAssumption();
        traceFunc = null;
        callNode = null;
        event = Layouts.STRING.createString(getContext().getCoreLibrary().getStringFactory(), RubyString.encodeBytelist("line", UTF8Encoding.INSTANCE), StringSupport.CR_7BIT, null);
        file = Layouts.STRING.createString(getContext().getCoreLibrary().getStringFactory(), RubyString.encodeBytelist(sourceSection.getSource().getName(), UTF8Encoding.INSTANCE), StringSupport.CR_UNKNOWN, null);
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
                callNode = insert(Truffle.getRuntime().createDirectCallNode(Layouts.PROC.getCallTargetForType(traceFunc)));
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
                        Layouts.BINDING.createBinding(getContext().getCoreLibrary().getBindingFactory(), RubyArguments.getSelf(frame.getArguments()), frame.materialize()),
                        context.getCoreLibrary().getNilObject()
                };

                try {
                    callNode.call(frame, RubyArguments.pack(Layouts.PROC.getMethod(traceFunc), Layouts.PROC.getDeclarationFrame(traceFunc), Layouts.PROC.getSelf(traceFunc), Layouts.PROC.getBlock(traceFunc), args));
                } finally {
                    context.getTraceManager().setInTraceFunc(false);
                }
            }
        }
    }

}
