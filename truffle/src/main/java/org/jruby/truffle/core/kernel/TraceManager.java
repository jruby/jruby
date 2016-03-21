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
import com.oracle.truffle.api.instrumentation.EventBinding;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;
import com.oracle.truffle.api.instrumentation.ExecutionEventNodeFactory;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.rope.CodeRange;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.loader.SourceLoader;

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

    private Collection<EventBinding<?>> instruments;
    private boolean isInTraceFunc = false;

    public TraceManager(RubyContext context, Instrumenter instrumenter) {
        this.context = context;
        this.instrumenter = instrumenter;
    }

    public void setTraceFunc(final DynamicObject traceFunc) {
        assert RubyGuards.isRubyProc(traceFunc);

        if (instruments != null) {
            for (EventBinding<?> instrument : instruments) {
                instrument.dispose();
            }
        }

        if (traceFunc == null) {
            instruments = null;
            return;
        }

        instruments = new ArrayList<>();

        instruments.add(instrumenter.attachFactory(SourceSectionFilter.newBuilder().annotatedBy(LineTag.class).build(), new ExecutionEventNodeFactory() {
            @Override
            public ExecutionEventNode create(EventContext eventContext) {
                final DynamicObject event = StringOperations.createString(context, StringOperations.encodeRope("line", UTF8Encoding.INSTANCE, CodeRange.CR_7BIT));
                return new BaseEventEventNode(context, traceFunc, event);
            }
        }));

        instruments.add(instrumenter.attachFactory(SourceSectionFilter.newBuilder().annotatedBy(CallTag.class).build(), new ExecutionEventNodeFactory() {
            @Override
            public ExecutionEventNode create(EventContext eventContext) {
                final DynamicObject event = StringOperations.createString(context, StringOperations.encodeRope("call", UTF8Encoding.INSTANCE, CodeRange.CR_7BIT));
                return new CallEventEventNode(context, traceFunc, event);
            }
        }));

        instruments.add(instrumenter.attachFactory(SourceSectionFilter.newBuilder().annotatedBy(ClassTag.class).build(), new ExecutionEventNodeFactory() {
            @Override
            public ExecutionEventNode create(EventContext eventContext) {
                final DynamicObject event = StringOperations.createString(context, StringOperations.encodeRope("class", UTF8Encoding.INSTANCE, CodeRange.CR_7BIT));
                return new BaseEventEventNode(context, traceFunc, event);
            }
        }));

    }

    private final class BaseEventEventNode extends ExecutionEventNode {

        private final ConditionProfile inTraceFuncProfile = ConditionProfile.createBinaryProfile();

        private final RubyContext context;
        private final DynamicObject traceFunc;
        private final Object event;

        public BaseEventEventNode(RubyContext context, DynamicObject traceFunc, Object event) {
            this.context = context;
            this.traceFunc = traceFunc;
            this.event = event;
        }

        @Override
        protected void onEnter(VirtualFrame frame) {
            if (!inTraceFuncProfile.profile(isInTraceFunc)) {
                callSetTraceFunc(frame.materialize());
            }
        }

        @TruffleBoundary
        private void callSetTraceFunc(MaterializedFrame frame) {
            final SourceSection sourceSection = getEncapsulatingSourceSection();

            final DynamicObject file = StringOperations.createString(context, StringOperations.encodeRope(sourceSection.getSource().getName(), UTF8Encoding.INSTANCE));
            final int line = sourceSection.getStartLine();

            final Object classname = context.getCoreLibrary().getNilObject();
            final Object id = context.getCoreLibrary().getNilObject();

            final DynamicObject binding = Layouts.BINDING.createBinding(context.getCoreLibrary().getBindingFactory(), frame);

            isInTraceFunc = true;
            try {
                context.getCodeLoader().inline(this, frame, "traceFunc.call(event, file, line, id, binding, classname)", "traceFunc", traceFunc, "event", event, "file", file, "line", line, "id", id, "binding", binding, "classname", classname);
            } finally {
               isInTraceFunc = false;
            }
        }

    }

    private final class CallEventEventNode extends ExecutionEventNode {

        private final ConditionProfile inTraceFuncProfile = ConditionProfile.createBinaryProfile();

        private final static String callTraceFuncCode = "traceFunc.call(event, file, line, id, binding, classname)";

        private final RubyContext context;
        private final DynamicObject traceFunc;
        private final Object event;

        public CallEventEventNode(RubyContext context, DynamicObject traceFunc, Object event) {
            this.context = context;
            this.traceFunc = traceFunc;
            this.event = event;
        }

        @Override
        protected void onEnter(VirtualFrame frame) {
            if (!inTraceFuncProfile.profile(isInTraceFunc)) {
                callSetTraceFunc(frame.materialize());
            }
        }

        @TruffleBoundary
        private void callSetTraceFunc(MaterializedFrame frame) {
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
                context.getCodeLoader().inline(this, frame, callTraceFuncCode, "traceFunc", traceFunc, "event", event, "file", file, "line", line, "id", id, "binding", binding, "classname", classname);
            } finally {
                isInTraceFunc = false;
            }
        }

    }

}
