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

import org.jruby.truffle.pack.runtime.ByteWriter;
import org.jruby.truffle.pack.nodes.PackNode;

public class NNode extends PackNode {

    private final int repeats;
    @Child private PackNode child;

    public NNode(int repeats, PackNode child) {
        this.repeats = repeats;
        this.child = child;
    }

    @Override
    public int pack(int[] source, int source_pos, int source_len, ByteWriter writer) {
        for (int i = 0; i < repeats; i++) {
            source_pos = child.pack(source, source_pos, source_len, writer);
        }
        getRootNode().reportLoopCount(repeats);
        return source_pos;
    }

    @Override
    public int pack(long[] source, int source_pos, int source_len, ByteWriter writer) {
        for (int i = 0; i < repeats; i++) {
            source_pos = child.pack(source, source_pos, source_len, writer);
        }
        getRootNode().reportLoopCount(repeats);
        return source_pos;
    }

    @Override
    public int pack(double[] source, int source_pos, int source_len, ByteWriter writer) {
        for (int i = 0; i < repeats; i++) {
            source_pos = child.pack(source, source_pos, source_len, writer);
        }
        getRootNode().reportLoopCount(repeats);
        return source_pos;
    }

    @Override
    public int pack(Object[] source, int source_pos, int source_len, ByteWriter writer) {
        for (int i = 0; i < repeats; i++) {
            source_pos = child.pack(source, source_pos, source_len, writer);
        }
        getRootNode().reportLoopCount(repeats);
        return source_pos;
    }

}
