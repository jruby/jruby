/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.cast.BooleanCastNode;
import org.jruby.truffle.nodes.cast.BooleanCastNodeFactory;
import org.jruby.truffle.nodes.yield.YieldDispatchHeadNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyProc;

public abstract class YieldingCoreMethodNode extends CoreMethodNode {

    @Child private YieldDispatchHeadNode dispatchNode;
    @Child private BooleanCastNode booleanCastNode;

    public YieldingCoreMethodNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
        dispatchNode = new YieldDispatchHeadNode(context);
    }

    public YieldingCoreMethodNode(YieldingCoreMethodNode prev) {
        super(prev);
        dispatchNode = prev.dispatchNode;
    }

    private boolean booleanCast(VirtualFrame frame, Object value) {
        if (booleanCastNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            booleanCastNode = insert(BooleanCastNodeFactory.create(getContext(), getSourceSection(), null));
        }
        return booleanCastNode.executeBoolean(frame, value);
    }

    public Object yield(VirtualFrame frame, RubyProc block, Object... arguments) {
        return dispatchNode.dispatch(frame, block, arguments);
    }

    public boolean yieldIsTruthy(VirtualFrame frame, RubyProc block, Object... arguments) {
        return booleanCast(frame, yield(frame, block, arguments));
    }

    public Object yieldWithModifiedSelf(VirtualFrame frame, RubyProc block, Object self, Object... arguments) {
        return dispatchNode.dispatchWithModifiedSelf(frame, block, self, arguments);
    }

}
