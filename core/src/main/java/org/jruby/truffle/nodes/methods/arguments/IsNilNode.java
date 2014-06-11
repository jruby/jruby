/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.methods.arguments;

import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.NilPlaceholder;
import org.jruby.truffle.runtime.RubyContext;

@NodeInfo(shortName = "is-nil")
public class IsNilNode extends RubyNode {

    @Child protected RubyNode child;

    public IsNilNode(RubyContext context, SourceSection sourceSection, RubyNode child) {
        super(context, sourceSection);
        this.child = child;
    }

    @Override
    public boolean executeBoolean(VirtualFrame frame) {
        notDesignedForCompilation();

        return child.execute(frame) instanceof NilPlaceholder;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return executeBoolean(frame);
    }

}
