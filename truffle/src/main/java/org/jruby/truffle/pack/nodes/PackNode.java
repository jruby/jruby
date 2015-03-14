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
import com.oracle.truffle.api.nodes.Node;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.truffle.pack.runtime.ByteWriter;

public abstract class PackNode extends Node {

    @CompilerDirectives.CompilationFinal private boolean seenInt;
    @CompilerDirectives.CompilationFinal private boolean seenLong;
    @CompilerDirectives.CompilationFinal private boolean seenDouble;
    @CompilerDirectives.CompilationFinal private boolean seenIObject;
    @CompilerDirectives.CompilationFinal private boolean seenObject;

    public abstract int pack(int[] source, int source_pos, int source_len, ByteWriter writer);
    public abstract int pack(long[] source, int source_pos, int source_len, ByteWriter writer);
    public abstract int pack(double[] source, int source_pos, int source_len, ByteWriter writer);
    public abstract int pack(IRubyObject[] source, int source_pos, int source_len, ByteWriter writer);
    public abstract int pack(Object[] source, int source_pos, int source_len, ByteWriter writer);

    public int pack(Object source, int source_pos, int source_len, ByteWriter writer) {
        if (seenInt && source instanceof int[]) {
            return pack((int[]) source, source_pos, source_len, writer);
        }

        if (seenLong && source instanceof long[]) {
            return pack((long[]) source, source_pos, source_len, writer);
        }

        if (seenDouble && source instanceof double[]) {
            return pack((double[]) source, source_pos, source_len, writer);
        }

        if (seenIObject && source instanceof IRubyObject[]) {
            return pack((IRubyObject[]) source, source_pos, source_len, writer);
        }

        if (seenObject && source instanceof Object[]) {
            return pack((Object[]) source, source_pos, source_len, writer);
        }

        CompilerDirectives.transferToInterpreterAndInvalidate();

        if (source instanceof int[]) {
            seenInt = true;
            return pack((int[]) source, source_pos, source_len, writer);
        }

        if (source instanceof long[]) {
            seenLong = true;
            return pack((long[]) source, source_pos, source_len, writer);
        }

        if (source instanceof double[]) {
            seenDouble = true;
            return pack((double[]) source, source_pos, source_len, writer);
        }

        if (source instanceof IRubyObject[]) {
            seenIObject = true;
            return pack((IRubyObject[]) source, source_pos, source_len, writer);
        }

        if (source instanceof Object[]) {
            seenObject = true;
            return pack((Object[]) source, source_pos, source_len, writer);
        }

        throw new UnsupportedOperationException();
    }

}
