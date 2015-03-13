/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.pack.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import org.jruby.truffle.pack.runtime.ByteWriter;
import org.jruby.truffle.runtime.util.ArrayUtils;
import org.jruby.util.ByteList;

public class PackRootNode extends RootNode {

    private final String description;

    @Child private PackNode child;

    @CompilerDirectives.CompilationFinal private int expectedLength = ArrayUtils.capacity(0, 0);

    public PackRootNode(String description, PackNode child) {
        this.description = description;
        this.child = child;
    }

    public ByteList executeByteList(VirtualFrame frame) {
        final Object source = frame.getArguments()[0];
        final int source_len = (int) frame.getArguments()[1];

        final ByteWriter writer = new ByteWriter(expectedLength);

        child.pack(source, 0, source_len, writer);

        if (writer.getLength() > expectedLength) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            expectedLength = ArrayUtils.capacity(expectedLength, writer.getLength());
        }

        return writer.toByteList();
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return executeByteList(frame);
    }

    @Override
    public boolean isCloningAllowed() {
        return true;
    }

    @Override
    public String toString() {
        return description;
    }
}
