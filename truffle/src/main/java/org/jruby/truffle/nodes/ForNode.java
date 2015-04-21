/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.runtime.RubyContext;

public class ForNode extends RubyNode {

    @Child private RubyNode callNode;

    public ForNode(RubyContext context, SourceSection sourceSection, RubyNode callNode) {
        super(context, sourceSection);
        this.callNode = callNode;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return callNode.execute(frame);
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        callNode.executeVoid(frame);
    }
}
