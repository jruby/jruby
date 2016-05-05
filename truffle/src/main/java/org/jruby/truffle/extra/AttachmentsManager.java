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

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventBinding;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;
import com.oracle.truffle.api.instrumentation.ExecutionEventNodeFactory;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.core.binding.BindingNodes;
import org.jruby.truffle.core.proc.ProcOperations;
import org.jruby.truffle.language.RubyGuards;

public class AttachmentsManager {

    public class LineTag {
    }

    private final RubyContext context;
    private final Instrumenter instrumenter;

    public AttachmentsManager(RubyContext context, Instrumenter instrumenter) {
        this.context = context;
        this.instrumenter = instrumenter;
    }

    public synchronized EventBinding<?> attach(String file, int line, final DynamicObject block) {
        assert RubyGuards.isRubyProc(block);

        final Source source = context.getSourceCache().getBestSourceFuzzily(file);

        final SourceSectionFilter filter = SourceSectionFilter.newBuilder()
                .sourceIs(source)
                .lineIs(line)
                .tagIs(LineTag.class)
                .build();

        return instrumenter.attachFactory(filter, new ExecutionEventNodeFactory() {

            public ExecutionEventNode create(EventContext eventContext) {
                return new AttachmentEventNode(context, block);
            }

        });
    }

    private static class AttachmentEventNode extends ExecutionEventNode {

        private final RubyContext context;
        private final DynamicObject block;

        @Child private DirectCallNode callNode;

        public AttachmentEventNode(RubyContext context, DynamicObject block) {
            this.context = context;
            this.block = block;
            this.callNode = Truffle.getRuntime().createDirectCallNode(Layouts.PROC.getCallTargetForType(block));
        }

        @Override
        public void onEnter(VirtualFrame frame) {
            callNode.call(frame,
                    ProcOperations.packArguments(block, BindingNodes.createBinding(context, frame.materialize())));
        }

    }


}
