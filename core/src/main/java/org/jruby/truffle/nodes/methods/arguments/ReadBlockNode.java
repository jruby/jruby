/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.methods.arguments;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.*;

/**
 * Read the block as a {@code Proc}.
 */
@NodeInfo(shortName = "read-block-argument")
public class ReadBlockNode extends RubyNode {

    private final Object valueIfNotPresent;

    public ReadBlockNode(RubyContext context, SourceSection sourceSection, Object valueIfNotPresent) {
        super(context, sourceSection);
        this.valueIfNotPresent = valueIfNotPresent;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final RubyArguments arguments = new RubyArguments(frame.getArguments());
        final RubyProc block = arguments.getBlock();

        if (block == null) {
            return valueIfNotPresent;
        } else {
            return block;
        }
    }

}
