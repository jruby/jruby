/*
 * Copyright (c) 2014, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.dispatch;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.language.RubyNode;

public class RespondToNode extends RubyNode {

    private final String methodName;

    @Child private RubyNode child;
    @Child private DoesRespondDispatchHeadNode dispatch = new DoesRespondDispatchHeadNode(true);

    public RespondToNode(RubyNode child, String methodName) {
        this.methodName = methodName;
        this.child = child;
    }

    @Override
    public boolean executeBoolean(VirtualFrame frame) {
        // TODO(cseaton): check this is actually a static "find if there is such method" and not a dynamic call to respond_to?
        return dispatch.doesRespondTo(frame, methodName, child.execute(frame));
    }

    public boolean executeBoolean(VirtualFrame frame, Object object) {
        return dispatch.doesRespondTo(frame, methodName, object);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return executeBoolean(frame);
    }

}
