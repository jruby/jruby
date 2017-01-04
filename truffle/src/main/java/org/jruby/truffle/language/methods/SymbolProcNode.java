/*
 * Copyright (c) 2014, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.methods;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.core.array.ArrayUtils;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;

public class SymbolProcNode extends RubyNode {

    private final String symbol;

    @Child private CallDispatchHeadNode callNode;

    public SymbolProcNode(String symbol) {
        this.symbol = symbol;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object receiver = RubyArguments.getArgument(frame, 0);

        final DynamicObject block = RubyArguments.getBlock(frame);

        final Object[] arguments = ArrayUtils.extractRange(
                RubyArguments.getArguments(frame),
                1,
                RubyArguments.getArgumentsCount(frame));

        return getCallNode().callWithBlock(frame, receiver, symbol, block, arguments);
    }

    private CallDispatchHeadNode getCallNode() {
        if (callNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            callNode = insert(DispatchHeadNodeFactory.createMethodCall());
        }

        return callNode;
    }

}
