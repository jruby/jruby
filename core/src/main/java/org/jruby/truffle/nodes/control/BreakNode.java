/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.control;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.nodes.literal.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.*;

@NodeInfo(shortName = "break")
public class BreakNode extends RubyNode {

    @Child private RubyNode child;

    public BreakNode(RubyContext context, SourceSection sourceSection, RubyNode child) {
        super(context, sourceSection);
        this.child = adoptChild(child);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        if (child instanceof NilNode) {
            throw BreakException.NIL;
        } else {
            throw new BreakException(child.execute(frame));
        }
    }

}
