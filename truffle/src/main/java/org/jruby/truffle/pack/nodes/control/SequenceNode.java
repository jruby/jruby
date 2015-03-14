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
import com.oracle.truffle.api.nodes.ExplodeLoop;
import org.jruby.truffle.pack.runtime.ByteWriter;
import org.jruby.truffle.pack.nodes.PackNode;

public class SequenceNode extends PackNode {

    @Children private final PackNode[] children;

    public SequenceNode(PackNode[] children) {
        this.children = children;
    }

    @ExplodeLoop
    @Override
    public int pack(int[] source, int source_pos, int source_len, ByteWriter writer) {
        for (PackNode child : children) {
            source_pos = child.pack(source, source_pos, source_len, writer);
        }

        if (CompilerDirectives.inInterpreter()) {
            getRootNode().reportLoopCount(children.length);
        }

        return source_pos;
    }

    @ExplodeLoop
    @Override
    public int pack(long[] source, int source_pos, int source_len, ByteWriter writer) {
        for (PackNode child : children) {
            source_pos = child.pack(source, source_pos, source_len, writer);
        }

        if (CompilerDirectives.inInterpreter()) {
            getRootNode().reportLoopCount(children.length);
        }

        return source_pos;
    }

    @ExplodeLoop
    @Override
    public int pack(double[] source, int source_pos, int source_len, ByteWriter writer) {
        for (PackNode child : children) {
            source_pos = child.pack(source, source_pos, source_len, writer);
        }

        if (CompilerDirectives.inInterpreter()) {
            getRootNode().reportLoopCount(children.length);
        }

        return source_pos;
    }

    @ExplodeLoop
    @Override
    public int pack(Object[] source, int source_pos, int source_len, ByteWriter writer) {
        for (PackNode child : children) {
            source_pos = child.pack(source, source_pos, source_len, writer);
        }

        if (CompilerDirectives.inInterpreter()) {
            getRootNode().reportLoopCount(children.length);
        }

        return source_pos;
    }

}
