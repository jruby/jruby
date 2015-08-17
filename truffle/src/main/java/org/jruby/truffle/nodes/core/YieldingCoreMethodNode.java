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
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.cast.BooleanCastNode;
import org.jruby.truffle.nodes.cast.BooleanCastNodeGen;
import org.jruby.truffle.nodes.yield.YieldDispatchHeadNode;
import org.jruby.truffle.runtime.RubyContext;

public abstract class YieldingCoreMethodNode extends CoreMethodArrayArgumentsNode {

    @Child private YieldDispatchHeadNode dispatchNode;
    @Child private BooleanCastNode booleanCastNode;

    public YieldingCoreMethodNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
        dispatchNode = new YieldDispatchHeadNode(context);
    }

    private boolean booleanCast(VirtualFrame frame, Object value) {
        if (booleanCastNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            booleanCastNode = insert(BooleanCastNodeGen.create(getContext(), getSourceSection(), null));
        }
        return booleanCastNode.executeBoolean(frame, value);
    }

    public Object yield(VirtualFrame frame, DynamicObject block, Object... arguments) {
        assert block == null || RubyGuards.isRubyProc(block);
        return dispatchNode.dispatch(frame, block, arguments);
    }

    public boolean yieldIsTruthy(VirtualFrame frame, DynamicObject block, Object... arguments) {
        assert block == null || RubyGuards.isRubyProc(block);
        return booleanCast(frame, yield(frame, block, arguments));
    }

    public Object yieldWithModifiedSelf(VirtualFrame frame, DynamicObject block, Object self, Object... arguments) {
        assert block == null || RubyGuards.isRubyProc(block);
        return dispatchNode.dispatchWithModifiedSelf(frame, block, self, arguments);
    }

}
