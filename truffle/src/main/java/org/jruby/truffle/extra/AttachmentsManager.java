/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.extra;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrument.EvalInstrumentListener;
import com.oracle.truffle.api.instrument.Instrument;
import com.oracle.truffle.api.instrument.Probe;
import com.oracle.truffle.api.instrument.StandardSyntaxTag;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.LineLocation;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.tools.LineToProbesMap;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.RubyLanguage;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.binding.BindingNodes;
import org.jruby.truffle.core.proc.ProcNodes;
import org.jruby.truffle.language.RubyGuards;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttachmentsManager {

    public static final Source ATTACHMENT_SOURCE = Source.fromText("(attachment)", "(attachment)").withMimeType(RubyLanguage.MIME_TYPE);

    private final RubyContext context;
    private final LineToProbesMap lineToProbesMap;

    public AttachmentsManager(RubyContext context) {
        this.context = context;

        lineToProbesMap = new LineToProbesMap();
        context.getEnv().instrumenter().install(lineToProbesMap);
    }

    public synchronized Instrument attach(String file, int line, final DynamicObject block) {
        assert RubyGuards.isRubyProc(block);

        final String info = String.format("Truffle::Primitive.attach@%s:%d", file, line);

        final EvalInstrumentListener listener = new EvalInstrumentListener() {

            @Override
            public void onExecution(Node node, VirtualFrame virtualFrame, Object o) {
            }

            @Override
            public void onFailure(Node node, VirtualFrame virtualFrame, Exception e) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else {
                    throw new RuntimeException(e);
                }
            }

        };

        final Source source = context.getSourceCache().getBestSourceFuzzily(file);
        
        final LineLocation lineLocation = source.createLineLocation(line);

        for (Probe probe : lineToProbesMap.findProbes(lineLocation)) {
            if (probe.isTaggedAs(StandardSyntaxTag.STATEMENT)) {
                final Map<String, Object> parameters = new HashMap<>();
                parameters.put("section", probe.getProbedSourceSection());
                parameters.put("block", block);
                return context.getEnv().instrumenter().attach(probe, ATTACHMENT_SOURCE, listener, info, parameters);
            }
        }

        throw new RuntimeException("couldn't find a statement!");
    }

    public static class AttachmentRootNode extends RootNode {

        private final RubyContext context;
        private final DynamicObject block;

        @Child private DirectCallNode callNode;

        public AttachmentRootNode(Class<? extends TruffleLanguage<?>> language, RubyContext context, SourceSection sourceSection, FrameDescriptor frameDescriptor, DynamicObject block) {
            super(language, sourceSection, frameDescriptor);
            this.context = context;
            this.block = block;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            final MaterializedFrame callerFrame = (MaterializedFrame)frame.getArguments()[0];

            final DynamicObject binding = BindingNodes.createBinding(context, callerFrame);

            if (callNode == null) {
                CompilerDirectives.transferToInterpreter();

                callNode = insert(Truffle.getRuntime().createDirectCallNode(Layouts.PROC.getCallTargetForType(block)));

                if (callNode.isCallTargetCloningAllowed()) {
                    callNode.cloneCallTarget();
                }

                if (callNode.isInlinable()) {
                    callNode.forceInlining();
                }
            }

            callNode.call(frame, ProcNodes.packArguments(block, binding));

            return null;
        }

    }

}
