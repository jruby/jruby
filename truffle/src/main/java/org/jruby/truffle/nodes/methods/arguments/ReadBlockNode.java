/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.methods.arguments;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyProc;

/**
 * Read the block as a {@code Proc}.
 */
public class ReadBlockNode extends RubyNode {

    private final Object valueIfNotPresent;

    public ReadBlockNode(RubyContext context, SourceSection sourceSection, Object valueIfNotPresent) {
        super(context, sourceSection);
        this.valueIfNotPresent = valueIfNotPresent;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final RubyProc block = RubyArguments.getBlock(frame.getArguments());

        if (block == null) {
            return valueIfNotPresent;
        } else {
            return block;
        }
    }

}
