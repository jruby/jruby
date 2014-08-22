/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.SourceSection;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.yield.YieldDispatchHeadNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.core.RubyTrueClass;

public abstract class YieldingArrayCoreMethodNode extends ArrayCoreMethodNode {

    @Child protected YieldDispatchHeadNode dispatchNode;

    public YieldingArrayCoreMethodNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
        dispatchNode = new YieldDispatchHeadNode(context);
    }

    public YieldingArrayCoreMethodNode(YieldingArrayCoreMethodNode prev) {
        super(prev);
        dispatchNode = prev.dispatchNode;
    }

    public Object yield(VirtualFrame frame, RubyProc block, Object... arguments) {
        assert RubyContext.shouldObjectsBeVisible(arguments);

        return dispatchNode.dispatch(frame, block, arguments);
    }

    public boolean yieldBoolean(VirtualFrame frame, RubyProc block, Object... arguments) {
        // TODO(CS): this should be a node!
        RubyNode.notDesignedForCompilation();
        return RubyTrueClass.toBoolean(yield(frame, block, arguments));
    }

}
