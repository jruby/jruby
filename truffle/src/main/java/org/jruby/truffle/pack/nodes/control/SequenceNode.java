/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.pack.nodes.control;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import org.jruby.truffle.pack.nodes.PackNode;

/**
 * Run a sequence of child nodes.
 * <pre>
 * [1, 2, 3].pack('CCC') # =>  "\x01\x02\x03"
 */
public class SequenceNode extends PackNode {

    @Children private final PackNode[] children;

    public SequenceNode(PackNode... children) {
        this.children = children;
    }

    @ExplodeLoop
    @Override
    public Object execute(VirtualFrame frame) {
        for (PackNode child : children) {
            child.execute(frame);
        }

        if (CompilerDirectives.inInterpreter()) {
            getRootNode().reportLoopCount(children.length);
        }

        return null;
    }

}
