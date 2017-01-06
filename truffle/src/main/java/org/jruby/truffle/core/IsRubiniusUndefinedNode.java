/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.RubyNode;

public class IsRubiniusUndefinedNode extends RubyNode {

    @Child private RubyNode child;

    public IsRubiniusUndefinedNode(RubyNode child) {
        this.child = child;
    }

    @Override
    public boolean executeBoolean(VirtualFrame frame) {
        return child.execute(frame) == NotProvided.INSTANCE;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return executeBoolean(frame);
    }

}
