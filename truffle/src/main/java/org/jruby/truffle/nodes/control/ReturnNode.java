/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.control;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.ReturnID;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.ReturnException;

/**
 * Represents an explicit return. The return ID indicates where we should be returning to - this can
 * be non-trivial if you have blocks.
 */
public class ReturnNode extends RubyNode {

    private final ReturnID returnID;
    @Child private RubyNode value;

    public ReturnNode(RubyContext context, SourceSection sourceSection, ReturnID returnID, RubyNode value) {
        super(context, sourceSection);
        this.returnID = returnID;
        this.value = value;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        throw new ReturnException(returnID, value.execute(frame));
    }

}
