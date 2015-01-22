/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.yield;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyProc;

public abstract class YieldDispatchNode extends Node {

    private final RubyContext context;

    public YieldDispatchNode(RubyContext context) {
        assert context != null;
        this.context = context;
    }

    public abstract Object dispatch(VirtualFrame frame, RubyProc block, Object[] argumentsObjects);

    public abstract Object dispatchWithModifiedBlock(VirtualFrame frame, RubyProc block, RubyProc modifiedBlock, Object[] argumentsObjects);

    public abstract Object dispatchWithModifiedSelf(VirtualFrame frame, RubyProc block, Object self, Object... argumentsObjects);

    public RubyContext getContext() {
        return context;
    }

}
