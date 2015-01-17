/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.control;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.cast.BooleanCastNode;
import org.jruby.truffle.runtime.RubyContext;

/**
 * Represents a Ruby {@code not} or {@code !} expression.
 */
public class NotNode extends RubyNode {

    @Child private BooleanCastNode child;

    public NotNode(RubyContext context, SourceSection sourceSection, BooleanCastNode child) {
        super(context, sourceSection);
        this.child = child;
    }

    @Override
    public boolean executeBoolean(VirtualFrame frame) {
        return !child.executeBoolean(frame);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return executeBoolean(frame);
    }

}
