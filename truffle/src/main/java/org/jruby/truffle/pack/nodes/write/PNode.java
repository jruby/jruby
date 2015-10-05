/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.pack.nodes.write;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.pack.nodes.PackNode;
import org.jruby.truffle.runtime.RubyContext;

/**
 * Write the address of a 'structure'. In MRI this means the pointer to a
 * binary string. I think MRI that now has a copying collector so not sure how
 * safe this is even there.
 * <p>
 * We simply implement it as {@code NULL}. At least any attempty to dereference
 * will fail early.
 * <pre>
 * [1, 2, 3].pack('x') # =>  "\x00\x00\x00\x00\x00\x00\x00\x00"
 */
public class PNode extends PackNode {

    public PNode(RubyContext context) {
        super(context);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        /*
         * P and p print the address of a string. Obviously that doesn't work
         * well in Java. We'll print 0x0000000000000000 with the hope that at
         * least it should page fault if anyone tries to read it.
         */
        advanceSourcePosition(frame);
        return (long) 0;
    }

}
