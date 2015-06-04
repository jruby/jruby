/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.translator;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;

/**
 * Dead nodes are removed wherever they are found during translation. They fill in for some missing
 * nodes when we're processing the AST.
 */
public class DeadNode extends RubyNode {

    private final Exception reason;

    public DeadNode(RubyContext context, SourceSection sourceSection, Exception reason) {
        super(context, sourceSection);
        this.reason = reason;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        throw new UnsupportedOperationException(reason);
    }
}
