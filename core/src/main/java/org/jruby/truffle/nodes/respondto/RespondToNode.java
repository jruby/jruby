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

import com.oracle.truffle.api.SourceSection;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;

public class RespondToNode extends RubyNode {

    @Child protected RubyNode child;
    private final String name;

    public RespondToNode(RubyContext context, SourceSection sourceSection, RubyNode child, String name) {
        super(context, sourceSection);
        this.child = adoptChild(child);
        this.name = name;
    }

    public boolean executeBoolean(VirtualFrame frame) {
        final Object receiver = child.execute(frame);

        final RubyBasicObject boxed = getContext().getCoreLibrary().box(receiver);

        return boxed.getLookupNode().lookupMethod(name) != null;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return executeBoolean(frame);
    }

}
