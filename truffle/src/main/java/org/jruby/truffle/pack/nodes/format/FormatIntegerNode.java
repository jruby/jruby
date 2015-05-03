/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.pack.nodes.format;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import org.jruby.truffle.pack.nodes.PackNode;
import org.jruby.truffle.pack.nodes.SourceNode;
import org.jruby.truffle.runtime.core.RubyBignum;
import org.jruby.util.ByteList;

import java.nio.charset.StandardCharsets;

@NodeChildren({
        @NodeChild(value = "value", type = PackNode.class),
})
public abstract class FormatIntegerNode extends PackNode {

    @CompilerDirectives.TruffleBoundary
    @Specialization
    public ByteList formatInteger(int value) {
        return new ByteList(Integer.toString(value).getBytes(StandardCharsets.US_ASCII));
    }

    @CompilerDirectives.TruffleBoundary
    @Specialization
    public ByteList formatInteger(long value) {
        return new ByteList(Long.toString(value).getBytes(StandardCharsets.US_ASCII));
    }

    @CompilerDirectives.TruffleBoundary
    @Specialization
    public ByteList formatInteger(RubyBignum value) {
        return new ByteList(value.bigIntegerValue().toString().getBytes(StandardCharsets.US_ASCII));
    }

}
