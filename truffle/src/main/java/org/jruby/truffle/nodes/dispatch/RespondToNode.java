/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.dispatch;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;

public class RespondToNode extends RubyNode {

    private final String methodName;

    @Child private RubyNode child;
    @Child private DoesRespondDispatchHeadNode dispatch;

    public RespondToNode(RubyContext context, SourceSection sourceSection, RubyNode child, String methodName) {
        super(context, sourceSection);
        this.methodName = methodName;
        this.child = child;
        dispatch = new DoesRespondDispatchHeadNode(context, false, false, MissingBehavior.RETURN_MISSING, null);
    }

    public boolean executeBoolean(VirtualFrame frame) {
        // TODO(cseaton): check this is actually a static "find if there is such method" and not a dynamic call to respond_to?
        return dispatch.doesRespondTo(frame, methodName, child.execute(frame));
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return executeBoolean(frame);
    }

}
