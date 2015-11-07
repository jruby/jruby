/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
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
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrument.*;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.LineLocation;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.tools.LineToProbesMap;

import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.core.BindingNodes;
import org.jruby.truffle.nodes.methods.DeclarationContext;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.RubyLanguage;
import org.jruby.truffle.runtime.layouts.Layouts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttachmentsManager {

    public static final Source ATTACHMENT_SOURCE = Source.fromText("(attachment)", "(attachment)").withMimeType(RubyLanguage.MIME_TYPE);

    private final RubyContext context;
    private final LineToProbesMap lineToProbesMap;
    private final Map<LineLocation, List<Instrument>> attachments = new HashMap<>();

    public AttachmentsManager(RubyContext context) {
        this.context = context;

        lineToProbesMap = new LineToProbesMap();
        context.getEnv().instrumenter().install(lineToProbesMap);
    }

    public synchronized void attach(String file, int line, final DynamicObject block) {
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

        List<Instrument> instruments = attachments.get(lineLocation);

        if (instruments == null) {
            instruments = new ArrayList<>();
            attachments.put(lineLocation, instruments);
        }

        for (Probe probe : lineToProbesMap.findProbes(lineLocation)) {
            if (probe.isTaggedAs(StandardSyntaxTag.STATEMENT)) {
                final Map<String, Object> parameters = new HashMap<>();
                parameters.put("section", probe.getProbedSourceSection());
                parameters.put("block", block);
                instruments.add(context.getEnv().instrumenter().attach(probe, ATTACHMENT_SOURCE, listener, info, parameters));
                return;
            }
        }

        throw new RuntimeException("couldn't find a statement!");
    }

    public synchronized void detach(String file, int line) {
        final Source source = context.getSourceCache().getBestSourceFuzzily(file);

        final LineLocation lineLocation = source.createLineLocation(line);

        final List<Instrument> instruments = attachments.remove(lineLocation);

        if (instruments != null) {
            for (Instrument instrument : instruments) {
                instrument.dispose();
            }
        }
    }

    public static class AttachmentRootNode extends RootNode {

        private final RubyContext context;
        private final DynamicObject block;

        @Child private DirectCallNode callNode;

        public AttachmentRootNode(Class<? extends TruffleLanguage> language, RubyContext context, SourceSection sourceSection, FrameDescriptor frameDescriptor, DynamicObject block) {
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

            callNode.call(frame, RubyArguments.pack(Layouts.PROC.getMethod(block), Layouts.PROC.getDeclarationFrame(block), null, Layouts.PROC.getSelf(block), Layouts.PROC.getBlock(block), DeclarationContext.BLOCK, new Object[]{binding}));

            return null;
        }

    }

}
