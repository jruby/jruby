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

import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.pack.nodes.PackNode;
import org.jruby.truffle.runtime.RubyContext;

/**
 * Moves the output position to a particular location - similar to seek
 * absolute in a file stream.
 * <pre>
 * [0xabcd].pack('N') # => "\x00\x00\xAB\xCD"
 * [0xabcd].pack('@2N') # => "\x00\x00\x00\x00\xAB\xCD"
 */
public class AtNode extends PackNode {

    private final int position;

    public AtNode(RubyContext context, int position) {
        super(context);
        this.position = position;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        setOutputPosition(frame, position);
        return null;
    }

}
