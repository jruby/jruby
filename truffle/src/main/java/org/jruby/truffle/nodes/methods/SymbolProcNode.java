/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.methods;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.core.array.ArrayUtils;

public class SymbolProcNode extends RubyNode {

    private final String symbol;

    @Child private CallDispatchHeadNode dispatch;

    public SymbolProcNode(RubyContext context, SourceSection sourceSection, String symbol) {
        super(context, sourceSection);
        this.symbol = symbol;
        dispatch = DispatchHeadNodeFactory.createMethodCall(context);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object[] args = frame.getArguments();
        final Object receiver = RubyArguments.getArgument(args, 0);
        final Object[] arguments = RubyArguments.getArguments(args);
        final Object[] sendArgs = ArrayUtils.extractRange(arguments, 1, arguments.length);
        return dispatch.call(frame, receiver, symbol, RubyArguments.getBlock(args), sendArgs);
    }

}
