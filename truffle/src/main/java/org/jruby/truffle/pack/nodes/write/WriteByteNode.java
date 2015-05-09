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

/**
 * Simply write a single bytes.
 */
public class WriteByteNode extends PackNode {

    private final byte value;

    public WriteByteNode(byte value) {
        this.value = value;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        writeBytes(frame, value);
        return null;
    }

}
