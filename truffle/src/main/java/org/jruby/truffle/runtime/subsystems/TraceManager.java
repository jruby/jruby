/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.subsystems;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrument.*;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jruby.truffle.nodes.core.ProcNodes;
import org.jruby.truffle.nodes.core.StringNodes;
import org.jruby.truffle.nodes.dispatch.RubyCallNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.RubySyntaxTag;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyBinding;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.runtime.methods.InternalMethod;

import java.util.ArrayList;
import java.util.Collection;

public class TraceManager {

    private final RubyContext context;

    private Collection<Instrument> instruments;
    private boolean isInTraceFunc = false;

    public TraceManager(RubyContext context) {
        this.context = context;
    }

    public void setTraceFunc(final RubyBasicObject traceFunc) {
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

        final AdvancedInstrumentResultListener listener = new AdvancedInstrumentResultListener() {

            @Override
            public void notifyResult(Node node, VirtualFrame virtualFrame, Object o) {
            }

            @Override
            public void notifyFailure(Node node, VirtualFrame virtualFrame, RuntimeException e) {
            }

        };

        final AdvancedInstrumentRootFactory lineEventFactory = new AdvancedInstrumentRootFactory() {

            @Override
            public AdvancedInstrumentRoot createInstrumentRoot(Probe probe, Node node) {
                final RubyBasicObject event = StringNodes.createString(context.getCoreLibrary().getStringClass(), "line");

                final SourceSection sourceSection = node.getEncapsulatingSourceSection();

                final RubyBasicObject file = StringNodes.createString(context.getCoreLibrary().getStringClass(), sourceSection.getSource().getName());
                final int line = sourceSection.getStartLine();

                return new AdvancedInstrumentRoot() {

                    @Child private DirectCallNode callNode;

                    private final ConditionProfile inTraceFuncProfile = ConditionProfile.createBinaryProfile();

                    @Override
                    public Object executeRoot(Node node, VirtualFrame frame) {
                        if (!inTraceFuncProfile.profile(isInTraceFunc)) {
                            final Object self = context.getCoreLibrary().getNilObject();
                            final Object classname = self;
                            final Object id = context.getCoreLibrary().getNilObject();

                            final RubyBinding binding = new RubyBinding(
                                    context.getCoreLibrary().getBindingClass(),
                                    self,
                                    frame.materialize());

                            if (callNode == null) {
                                CompilerDirectives.transferToInterpreterAndInvalidate();

                                callNode = insert(Truffle.getRuntime().createDirectCallNode(ProcNodes.getCallTargetForBlocks(traceFunc)));

                                if (callNode.isCallTargetCloningAllowed()) {
                                    callNode.cloneCallTarget();
                                }

                                if (callNode.isInlinable()) {
                                    callNode.forceInlining();
                                }
                            }

                            isInTraceFunc = true;

                            callNode.call(frame, RubyArguments.pack(
                                    ProcNodes.getMethod(traceFunc),
                                    ProcNodes.getDeclarationFrame(traceFunc),
                                    ProcNodes.getSelfCapturedInScope(traceFunc),
                                    ProcNodes.getBlockCapturedInScope(traceFunc),
                                    new Object[]{event, file, line, id, binding, classname}));

                            isInTraceFunc = false;
                        }

                        return null;
                    }

                    @Override
                    public String instrumentationInfo() {
                        return "set_trace_func";
                    }

                };
            }

        };

        instruments = new ArrayList<>();
        for (Probe probe : Probe.findProbesTaggedAs(RubySyntaxTag.LINE)) {
            final Instrument instrument = Instrument.create(listener, lineEventFactory, null, "set_trace_func");
            instruments.add(instrument);
            probe.attach(instrument);
        }

        Probe.addProbeListener(new ProbeListener() {
            @Override
            public void startASTProbing(Source source) {
            }

            @Override
            public void newProbeInserted(Probe probe) {
            }

            @Override
            public void probeTaggedAs(Probe probe, SyntaxTag tag, Object tagValue) {
                if (tag == RubySyntaxTag.LINE) {
                    final Instrument instrument = Instrument.create(listener, lineEventFactory, null, "set_trace_func");
                    instruments.add(instrument);
                    probe.attach(instrument);
                }
            }

            @Override
            public void endASTProbing(Source source) {
            }
        });
    }

}
