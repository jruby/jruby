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
import org.jruby.util.ByteList;

/**
 * Simply write bytes.
 */
public class WriteBytesNode extends PackNode {

    private final ByteList bytes;

    public WriteBytesNode(ByteList bytes) {
        this.bytes = bytes;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        writeBytes(frame, bytes);
        return null;
    }

}
