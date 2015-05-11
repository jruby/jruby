/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.exceptions;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyException;

/**
 * Base node for all nodes which may be able to rescue an exception. They have a test method
 * {@link #canHandle} and a body to execute if that test passes.
 */
public abstract class RescueNode extends RubyNode {

    @Child private RubyNode body;

    public RescueNode(RubyContext context, SourceSection sourceSection, RubyNode body) {
        super(context, sourceSection);
        this.body = body;
    }

    public abstract boolean canHandle(VirtualFrame frame, RubyException exception);

    @Override
    public Object execute(VirtualFrame frame) {
        return body.execute(frame);
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        body.executeVoid(frame);
    }

}
