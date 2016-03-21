/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.interop;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.dispatch.DispatchAction;
import org.jruby.truffle.language.dispatch.DispatchHeadNode;
import org.jruby.truffle.language.dispatch.MissingBehavior;

public class ResolvedInteropIndexedWriteNode extends RubyNode {

    private final String name;
    @Child private DispatchHeadNode head;
    @Child private IndexLabelToRubyNode toRubyIndex;
    private final int indexIndex;
    private final int valueIndex;

    public ResolvedInteropIndexedWriteNode(RubyContext context, SourceSection sourceSection, int indexIndex, int valueIndex) {
        super(context, sourceSection);
        this.name = "[]=";
        this.indexIndex = indexIndex;
        this.valueIndex = valueIndex;
        this.head = new DispatchHeadNode(context, true, MissingBehavior.CALL_METHOD_MISSING, DispatchAction.CALL_METHOD);
        this.toRubyIndex = IndexLabelToRubyNodeGen.create(context, sourceSection, null);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object index = toRubyIndex.executeWithTarget(frame, ForeignAccess.getArguments(frame).get(indexIndex));
        Object value = ForeignAccess.getArguments(frame).get(valueIndex);
        return head.dispatch(frame, ForeignAccess.getReceiver(frame), name, null, new Object[] {index, value});
    }
}
