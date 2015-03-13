/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;

public class ThreadLocalObjectNode extends RubyNode {

    public ThreadLocalObjectNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    @Override
    public RubyBasicObject executeRubyBasicObject(VirtualFrame frame) {
        return getContext().getThreadManager().getCurrentThread().getThreadLocals();
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return executeRubyBasicObject(frame);
    }
}
