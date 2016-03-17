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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.language.dispatch.DispatchAction;
import org.jruby.truffle.language.dispatch.DispatchHeadNode;
import org.jruby.truffle.language.dispatch.MissingBehavior;

class ResolvedInteropWriteToSymbolNode extends InteropNode {

    @Child private DispatchHeadNode head;
    private final DynamicObject name;
    private final DynamicObject  accessName;
    private final int labelIndex;
    private final int valueIndex;

    public ResolvedInteropWriteToSymbolNode(RubyContext context, SourceSection sourceSection, DynamicObject name, int labelIndex, int valueIndex) {
        super(context, sourceSection);
        this.name = name;
        this.accessName = context.getSymbolTable().getSymbol(Layouts.SYMBOL.getString(name) + "=");
        this.head = new DispatchHeadNode(context, true, MissingBehavior.CALL_METHOD_MISSING, DispatchAction.CALL_METHOD);
        this.labelIndex = labelIndex;
        this.valueIndex = valueIndex;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        if (name.equals(ForeignAccess.getArguments(frame).get(labelIndex))) {
            Object value = ForeignAccess.getArguments(frame).get(valueIndex);
            return head.dispatch(frame, ForeignAccess.getReceiver(frame), accessName, null, new Object[]{value});
        } else {
            CompilerDirectives.transferToInterpreter();
            throw new IllegalStateException("Name changed");
        }
    }
}
