/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.dispatch;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.nodes.cast.BooleanCastNode;
import org.jruby.truffle.nodes.cast.BooleanCastNodeFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyProc;

public class PredicateDispatchHeadNode extends Node {

    @Child private DispatchHeadNode dispatchNode;
    @Child private BooleanCastNode booleanCastNode;

    public PredicateDispatchHeadNode(RubyContext context) {
        dispatchNode = new DispatchHeadNode(context);
        booleanCastNode = BooleanCastNodeFactory.create(context, getSourceSection(), null);
    }

    public boolean call(
            VirtualFrame frame,
            Object receiverObject,
            Object methodName,
            RubyProc blockObject,
            Object... argumentsObjects) {
        return booleanCastNode.executeBoolean(frame,
                dispatchNode.call(frame, receiverObject, methodName, blockObject, argumentsObjects));
    }
}
