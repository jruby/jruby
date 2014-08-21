/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.respondto;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.call.DispatchHeadNode;
import org.jruby.truffle.runtime.RubyContext;

public class RespondToNode extends RubyNode {

    @Child protected RubyNode child;
    @Child protected DispatchHeadNode dispatch;

    public RespondToNode(RubyContext context, SourceSection sourceSection, RubyNode child, String name) {
        super(context, sourceSection);
        this.child = child;
        dispatch = new DispatchHeadNode(context, name, false, DispatchHeadNode.MissingBehavior.RETURN_MISSING);
    }

    public boolean executeBoolean(VirtualFrame frame) {
        return dispatch.doesRespondTo(frame, child.execute(frame));
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return executeBoolean(frame);
    }

}
