/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.methods;

import com.oracle.truffle.api.SourceSection;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;

import java.util.Arrays;

public class SymbolProcNode extends RubyNode {

    private final String symbol;

    public SymbolProcNode(RubyContext context, SourceSection sourceSection, String symbol) {
        super(context, sourceSection);
        this.symbol = symbol;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        notDesignedForCompilation();

        final Object[] args = frame.getArguments();
        final Object receiver = RubyArguments.getUserArgument(args, 0);
        final Object[] arguments = RubyArguments.extractUserArguments(args);
        final Object[] sendArgs = Arrays.copyOfRange(arguments, 1, arguments.length);
        final RubyBasicObject receiverObject = getContext().getCoreLibrary().box(receiver);
        return receiverObject.send(this, symbol, RubyArguments.getBlock(args), sendArgs);
    }

}
