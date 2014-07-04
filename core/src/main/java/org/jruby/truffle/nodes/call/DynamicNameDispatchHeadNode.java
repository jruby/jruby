/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.call;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.core.RubySymbol;

public class DynamicNameDispatchHeadNode extends Node {

    private final RubyContext context;
    @Child protected DynamicNameDispatchNode dispatch;

    public DynamicNameDispatchHeadNode(RubyContext context) {
        this.context = context;
        dispatch = new UninitializedDynamicNameDispatchNode(context);
    }

    public Object dispatch(VirtualFrame frame, Object receiverObject, RubySymbol name, RubyProc blockObject, Object[] argumentsObjects) {
        return dispatch.dispatch(frame, receiverObject, name, blockObject, argumentsObjects);
    }

    public Object dispatch(VirtualFrame frame, Object receiverObject, RubyString name, RubyProc blockObject, Object[] argumentsObjects) {
        return dispatch.dispatch(frame, receiverObject, name, blockObject, argumentsObjects);
    }

    public boolean doesRespondTo(VirtualFrame frame, Object receiverObject, RubySymbol name) {
        return dispatch.doesRespondTo(frame, receiverObject, name);
    }

    public boolean doesRespondTo(VirtualFrame frame, Object receiverObject, RubyString name) {
        return dispatch.doesRespondTo(frame, receiverObject, name);
    }

}
