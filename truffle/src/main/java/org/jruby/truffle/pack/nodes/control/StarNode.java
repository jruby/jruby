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
import org.jruby.truffle.pack.nodes.PackNode;

/**
 * Keep applying a child node as long as there is still source to read.
 * <pre>
 * [1, 2, 3].pack('C*') # =>  "\x01\x02\x03"
 */
public class StarNode extends PackNode {

    @Child private PackNode child;

    public StarNode(PackNode child) {
        this.child = child;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        int loops = 0;

        while (getSourcePosition(frame) < getSourceLength(frame)) {
            child.execute(frame);

            if (CompilerDirectives.inInterpreter()) {
                loops++;
            }
        }

        if (CompilerDirectives.inInterpreter()) {
            getRootNode().reportLoopCount(loops);
        }

        return null;
    }

}
