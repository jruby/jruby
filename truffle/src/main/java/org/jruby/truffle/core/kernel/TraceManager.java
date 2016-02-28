/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.kernel;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrument.Instrument;
import com.oracle.truffle.api.instrument.Probe;
import com.oracle.truffle.api.instrument.ProbeInstrument;
import com.oracle.truffle.api.instrument.ProbeListener;
import com.oracle.truffle.api.instrument.StandardInstrumentListener;
import com.oracle.truffle.api.instrument.SyntaxTag;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.rope.CodeRange;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.instrument.RubySyntaxTag;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.loader.SourceLoader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class TraceManager {

    private final RubyContext context;

    private Collection<ProbeInstrument> instruments;
    private boolean isInTraceFunc = false;
    private final Map<SyntaxTag, TraceFuncEventFactory> eventFactories = new LinkedHashMap<>();

    public TraceManager(RubyContext context) {
        this.context = context;
    }

    public void setTraceFunc(final DynamicObject traceFunc) {
        assert RubyGuards.isRubyProc(traceFunc);

        if (instruments != null) {
            for (Instrument instrument : instruments) {
                instrument.dispose();
            }
        }

        if (traceFunc == null) {
            instruments = null;
            return;
        }

        final TraceFuncEventFactory lineEventFactory = new TraceFuncEventFactory() {
            @Override
            public StandardInstrumentListener createInstrumentListener(RubyContext context, DynamicObject traceFunc) {
                final DynamicObject event = StringOperations.createString(context, StringOperations.encodeRope("line", UTF8Encoding.INSTANCE, CodeRange.CR_7BIT));

                return new BaseEventInstrumentListener(context, traceFunc, event);
            }
        };

        final TraceFuncEventFactory callEventFactory = new TraceFuncEventFactory() {
            @Override
            public StandardInstrumentListener createInstrumentListener(RubyContext context, DynamicObject traceFunc) {
                final DynamicObject event = StringOperations.createString(context, StringOperations.encodeRope("call", UTF8Encoding.INSTANCE, CodeRange.CR_7BIT));

                return new CallEventInstrumentListener(context, traceFunc, event);
            }
        };

        final TraceFuncEventFactory classEventFactory = new TraceFuncEventFactory() {
            @Override
            public StandardInstrumentListener createInstrumentListener(RubyContext context, DynamicObject traceFunc) {
                final DynamicObject event = StringOperations.createString(context, StringOperations.encodeRope("class", UTF8Encoding.INSTANCE, CodeRange.CR_7BIT));

                return new BaseEventInstrumentListener(context, traceFunc, event);
            }
        };

        eventFactories.put(RubySyntaxTag.LINE, lineEventFactory);
        eventFactories.put(RubySyntaxTag.CALL, callEventFactory);
        eventFactories.put(RubySyntaxTag.CLASS, classEventFactory);

        instruments = new ArrayList<>();

        for (Map.Entry<SyntaxTag, TraceFuncEventFactory> entry : eventFactories.entrySet()) {
            for (Probe probe : context.getEnv().instrumenter().findProbesTaggedAs(entry.getKey())) {
                instruments.add(context.getEnv().instrumenter().attach(probe, entry.getValue().createInstrumentListener(context, traceFunc), "set_trace_func"));
            }
        }

        context.getEnv().instrumenter().addProbeListener(new ProbeListener() {

            @Override
            public void startASTProbing(RootNode rootNode) {
            }

            @Override
            public void newProbeInserted(Probe probe) {
            }

            @Override
            public void probeTaggedAs(Probe probe, SyntaxTag tag, Object tagValue) {
                if (instruments != null && eventFactories.containsKey(tag)) {
                    instruments.add(context.getEnv().instrumenter().attach(probe, eventFactories.get(tag).createInstrumentListener(context, traceFunc), "set_trace_func"));
                }
            }

            @Override
            public void endASTProbing(RootNode rootNode) {
            }

        });
    }

    private abstract class TraceFuncEventFactory {

        public abstract StandardInstrumentListener createInstrumentListener(RubyContext context, DynamicObject traceFunc);

    }

    private final class BaseEventInstrumentListener implements StandardInstrumentListener {

        private final ConditionProfile inTraceFuncProfile = ConditionProfile.createBinaryProfile();

        private final RubyContext context;
        private final DynamicObject traceFunc;
        private final Object event;

        public BaseEventInstrumentListener(RubyContext context, DynamicObject traceFunc, Object event) {
            this.context = context;
            this.traceFunc = traceFunc;
            this.event = event;
        }

        @Override
        public void onEnter(Probe probe, Node node, VirtualFrame frame) {
            if (!inTraceFuncProfile.profile(isInTraceFunc)) {
                callSetTraceFunc(node, frame.materialize());
            }
        }

        @TruffleBoundary
        private void callSetTraceFunc(Node node, MaterializedFrame frame) {
            final SourceSection sourceSection = node.getEncapsulatingSourceSection();

            final DynamicObject file = StringOperations.createString(context, StringOperations.encodeRope(sourceSection.getSource().getName(), UTF8Encoding.INSTANCE));
            final int line = sourceSection.getStartLine();

            final Object classname = context.getCoreLibrary().getNilObject();
            final Object id = context.getCoreLibrary().getNilObject();

            final DynamicObject binding = Layouts.BINDING.createBinding(context.getCoreLibrary().getBindingFactory(), frame);

            isInTraceFunc = true;
            try {
                context.getCodeLoader().inline(node, frame, "traceFunc.call(event, file, line, id, binding, classname)", "traceFunc", traceFunc, "event", event, "file", file, "line", line, "id", id, "binding", binding, "classname", classname);
            } finally {
               isInTraceFunc = false;
            }
        }

        @Override
        public void onReturnVoid(Probe probe, Node node, VirtualFrame frame) {
        }

        @Override
        public void onReturnValue(Probe probe, Node node, VirtualFrame frame, Object result) {
        }

        @Override
        public void onReturnExceptional(Probe probe, Node node, VirtualFrame virtualFrame, Throwable throwable) {
        }

    }

    private final class CallEventInstrumentListener implements StandardInstrumentListener {

        private final ConditionProfile inTraceFuncProfile = ConditionProfile.createBinaryProfile();

        private final static String callTraceFuncCode = "traceFunc.call(event, file, line, id, binding, classname)";

        private final RubyContext context;
        private final DynamicObject traceFunc;
        private final Object event;

        public CallEventInstrumentListener(RubyContext context, DynamicObject traceFunc, Object event) {
            this.context = context;
            this.traceFunc = traceFunc;
            this.event = event;
        }

        @Override
        public void onEnter(Probe probe, Node node, VirtualFrame frame) {
            if (!inTraceFuncProfile.profile(isInTraceFunc)) {
                callSetTraceFunc(node, frame.materialize());
            }
        }

        @TruffleBoundary
        private void callSetTraceFunc(Node node, MaterializedFrame frame) {
            // set_trace_func reports the file and line of the call site.
            final String filename;
            final int line;
            final SourceSection sourceSection = Truffle.getRuntime().getCallerFrame().getCallNode().getEncapsulatingSourceSection();

            if (sourceSection.getSource() != null) {
                // Skip over any lines that are a result of the trace function call being made.
                if (sourceSection.getSource().getCode().equals(callTraceFuncCode)) {
                    return;
                }

                filename = sourceSection.getSource().getName();
                line = sourceSection.getStartLine();
            } else {
                filename = "<internal>";
                line = -1;
            }

            final DynamicObject file = StringOperations.createString(context, StringOperations.encodeRope(filename, UTF8Encoding.INSTANCE));

            if (!context.getOptions().INCLUDE_CORE_FILE_CALLERS_IN_SET_TRACE_FUNC && filename.startsWith(SourceLoader.TRUFFLE_SCHEME)) {
                return;
            }

            final Object self = RubyArguments.getSelf(frame);
            final Object classname = context.getCoreLibrary().getLogicalClass(self);
            final Object id = context.getSymbolTable().getSymbol(RubyArguments.getMethod(frame).getName());

            final DynamicObject binding = Layouts.BINDING.createBinding(context.getCoreLibrary().getBindingFactory(), Truffle.getRuntime().getCallerFrame().getFrame(FrameInstance.FrameAccess.MATERIALIZE, true).materialize());

            isInTraceFunc = true;
            try {
                context.getCodeLoader().inline(node, frame, callTraceFuncCode, "traceFunc", traceFunc, "event", event, "file", file, "line", line, "id", id, "binding", binding, "classname", classname);
            } finally {
                isInTraceFunc = false;
            }
        }

        @Override
        public void onReturnVoid(Probe probe, Node node, VirtualFrame frame) {
        }

        @Override
        public void onReturnValue(Probe probe, Node node, VirtualFrame frame, Object result) {
        }

        @Override
        public void onReturnExceptional(Probe probe, Node node, VirtualFrame virtualFrame, Throwable throwable) {
        }

    }
}
