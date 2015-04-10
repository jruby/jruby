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
 * Repeats a child node N times.
 * <p>
 * Note that there is no {@link ExplodeLoop} annotation on our {@link #execute}
 * method. This is because in general the problem with format expressions is
 * finding loops and recreating them - not removing them when we have them.
 * <p>
 * N is often very large - for example the number of pixels in an image.
 * <pre>
 * [1, 2, 3].pack('C3') # =>  "\x01\x02\x03"
 */
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
