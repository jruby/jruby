/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.format.nodes.read;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import org.jruby.truffle.format.nodes.PackNode;
import org.jruby.truffle.format.nodes.SourceNode;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;

@NodeChildren({
        @NodeChild(value = "source", type = SourceNode.class),
})
public abstract class ReadHashValueNode extends PackNode {

    private final Object key;

    public ReadHashValueNode(RubyContext context, Object key) {
        super(context);
        this.key = key;
    }

    @Specialization
    @CompilerDirectives.TruffleBoundary
    public Object read(Object[] source) {
        if (source.length != 1 || !RubyGuards.isRubyHash(source[0])) {
            throw new RaiseException(getContext().getCoreLibrary().argumentError("one hash required", this));
        }

        // We're not in a Ruby frame here, so we can't run arbitrary Ruby nodes like Hash#[]. Instead use a slow send.

        return getContext().send(source[0], "fetch", null, key);
    }

}
