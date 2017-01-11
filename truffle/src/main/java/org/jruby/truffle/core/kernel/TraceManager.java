/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.kernel;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventBinding;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.CyclicAssumption;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.RubyLanguage;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.LazyRubyNode;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.objects.LogicalClassNode;
import org.jruby.truffle.language.objects.LogicalClassNodeGen;
import org.jruby.truffle.language.yield.YieldNode;

import java.util.ArrayList;
import java.util.Collection;

public class TraceManager {
    public static @interface LineTag {
    }
    public static @interface CallTag {
    }
    public static @interface ClassTag {
    }
    
    private final RubyContext context;
    private final Instrumenter instrumenter;
    private final CyclicAssumption unusedAssumption;

    private Collection<EventBinding<?>> instruments;
    private boolean isInTraceFunc = false;

    public TraceManager(RubyContext context, Instrumenter instrumenter) {
        this.context = context;
        this.instrumenter = instrumenter;
        this.unusedAssumption = new CyclicAssumption("set_trace_func is not used");
    }

    public Assumption getUnusedAssumption() {
        return unusedAssumption.getAssumption();
    }

    @TruffleBoundary
    public void setTraceFunc(final DynamicObject traceFunc) {
        assert traceFunc == null || RubyGuards.isRubyProc(traceFunc);

        if (instruments != null) {
            for (EventBinding<?> instrument : instruments) {
                instrument.dispose();
            }
        }

        if (traceFunc == null) {
            // Update to a new valid assumption
            unusedAssumption.invalidate();
            instruments = null;
            return;
        }

        // TODO CS 11-Jan-17 race conditions here?

        LazyRubyNode.resolveAll(context);

        // Invalidate current assumption
        unusedAssumption.getAssumption().invalidate();

        instruments = new ArrayList<>();

        instruments.add(instrumenter.attachFactory(SourceSectionFilter.newBuilder()
                        .mimeTypeIs(RubyLanguage.MIME_TYPE)
                        .tagIs(LineTag.class)
                        .build(),
                eventContext -> new BaseEventEventNode(context, eventContext, traceFunc, context.getCoreStrings().LINE.createInstance())));

        instruments.add(instrumenter.attachFactory(SourceSectionFilter.newBuilder()
                        .mimeTypeIs(RubyLanguage.MIME_TYPE)
                        .tagIs(ClassTag.class)
                        .build(),
                eventContext -> new BaseEventEventNode(context, eventContext, traceFunc, context.getCoreStrings().CLASS.createInstance())));

        if (context.getOptions().TRACE_CALLS) {
            instruments.add(instrumenter.attachFactory(SourceSectionFilter.newBuilder()
                            .mimeTypeIs(RubyLanguage.MIME_TYPE)
                            .tagIs(CallTag.class)
                            .build(),
                    eventContext -> new CallEventEventNode(context, eventContext, traceFunc, context.getCoreStrings().CALL.createInstance())));
        }
    }

    private class BaseEventEventNode extends ExecutionEventNode {

        protected final ConditionProfile inTraceFuncProfile = ConditionProfile.createBinaryProfile();

        protected final RubyContext context;
        protected final EventContext eventContext;
        protected final DynamicObject traceFunc;
        protected final Object event;

        @Child private YieldNode yieldNode;

        @CompilationFinal private DynamicObject file;
        @CompilationFinal private int line;

        public BaseEventEventNode(RubyContext context, EventContext eventContext, DynamicObject traceFunc, Object event) {
            this.context = context;
            this.eventContext = eventContext;
            this.traceFunc = traceFunc;
            this.event = event;
        }

        @Override
        protected void onEnter(VirtualFrame frame) {
            if (inTraceFuncProfile.profile(isInTraceFunc)) {
                return;
            }

            isInTraceFunc = true;

            try {
                getYieldNode().dispatch(frame, traceFunc,
                        event,
                        getFile(),
                        getLine(),
                        context.getCoreLibrary().getNilObject(),
                        Layouts.BINDING.createBinding(context.getCoreLibrary().getBindingFactory(), frame.materialize()),
                        context.getCoreLibrary().getNilObject());
            } finally {
                isInTraceFunc = false;
            }
        }

        private DynamicObject getFile() {
            if (file == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                file = StringOperations.createString(context, context.getRopeTable().getRopeUTF8(eventContext.getInstrumentedSourceSection().getSource().getName()));
            }

            return file;
        }

        private int getLine() {
            if (line == 0) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                line = eventContext.getInstrumentedSourceSection().getStartLine();
            }

            return line;
        }

        protected YieldNode getYieldNode() {
            if (yieldNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                yieldNode = insert(new YieldNode());
            }

            return yieldNode;
        }

    }

    private class CallEventEventNode extends BaseEventEventNode {

        @Child private LogicalClassNode logicalClassNode;

        public CallEventEventNode(RubyContext context, EventContext eventContext, DynamicObject traceFunc, Object event) {
            super(context, eventContext, traceFunc, event);
        }

        @Override
        protected void onEnter(VirtualFrame frame) {
            if (inTraceFuncProfile.profile(isInTraceFunc)) {
                return;
            }

            final SourceSection sourceSection = getCallerSourceSection();

            final String file;
            final int line;

            if (sourceSection != null && sourceSection.getSource() != null) {
                file = sourceSection.getSource().getName();
                line = sourceSection.getStartLine();
            } else {
                file = "<internal>";
                line = -1;
            }

            isInTraceFunc = true;

            try {
                getYieldNode().dispatch(frame, traceFunc,
                        event,
                        getFile(file),
                        line,
                        context.getSymbolTable().getSymbol(RubyArguments.getMethod(frame).getName()),
                        Layouts.BINDING.createBinding(context.getCoreLibrary().getBindingFactory(), frame.materialize()),
                        getLogicalClass(RubyArguments.getSelf(frame)));
            } finally {
                isInTraceFunc = false;
            }
        }

        @TruffleBoundary
        private SourceSection getCallerSourceSection() {
            final Node callNode = Truffle.getRuntime().getCallerFrame().getCallNode();

            if (callNode == null) {
                return null;
            } else {
                return callNode.getEncapsulatingSourceSection();
            }
        }

        @TruffleBoundary
        private DynamicObject getFile(String file) {
            return StringOperations.createString(context, context.getRopeTable().getRopeUTF8(file));
        }

        private DynamicObject getLogicalClass(Object object) {
            if (logicalClassNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                logicalClassNode = insert(LogicalClassNodeGen.create(null));
            }

            return logicalClassNode.executeLogicalClass(object);
        }

    }

}
