/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.exceptions;

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.ThreadLocalObjectNode;
import org.jruby.truffle.nodes.literal.NilNode;
import org.jruby.truffle.nodes.objects.WriteInstanceVariableNode;
import org.jruby.truffle.runtime.RubyContext;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

/** Clear the thread-local $! */
public class ClearExceptionVariableNode extends RubyNode {

    @Child private WriteInstanceVariableNode writeNode;

    public ClearExceptionVariableNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
        writeNode = new WriteInstanceVariableNode(context, sourceSection, "$!",
                new ThreadLocalObjectNode(context, sourceSection),
                new NilNode(context, sourceSection),
                true);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return writeNode.execute(frame);
    }

}
