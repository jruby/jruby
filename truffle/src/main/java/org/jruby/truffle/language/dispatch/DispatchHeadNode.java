/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.dispatch;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.RubyContext;

public class DispatchHeadNode extends Node {

    protected final RubyContext context;
    protected final boolean ignoreVisibility;
    protected final MissingBehavior missingBehavior;
    protected final DispatchAction dispatchAction;

    @Child private DispatchNode first;

    public DispatchHeadNode(
            RubyContext context,
            boolean ignoreVisibility,
            MissingBehavior missingBehavior,
            DispatchAction dispatchAction) {
        this.context = context;
        this.ignoreVisibility = ignoreVisibility;
        this.missingBehavior = missingBehavior;
        this.dispatchAction = dispatchAction;
        first = new UnresolvedDispatchNode(context, ignoreVisibility, missingBehavior, dispatchAction);
    }

    public Object dispatch(
            VirtualFrame frame,
            Object receiverObject,
            Object methodName,
            DynamicObject blockObject,
            Object[] argumentsObjects) {
        return first.executeDispatch(
                frame,
                receiverObject,
                methodName,
                blockObject,
                argumentsObjects);
    }

    public void reset(String reason) {
        first.replace(new UnresolvedDispatchNode(
                context, ignoreVisibility, missingBehavior, dispatchAction), reason);
    }

    public DispatchNode getFirstDispatchNode() {
        return first;
    }

    public DispatchAction getDispatchAction() {
        return dispatchAction;
    }

}
