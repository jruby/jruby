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

public class NNode extends PackNode {

    private final int repeats;
    @Child private PackNode child;

    public NNode(int repeats, PackNode child) {
        this.repeats = repeats;
        this.child = child;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        for (int i = 0; i < repeats; i++) {
            child.execute(frame);
        }

        if (CompilerDirectives.inInterpreter()) {
            getRootNode().reportLoopCount(repeats);
        }

        return null;
    }

}
