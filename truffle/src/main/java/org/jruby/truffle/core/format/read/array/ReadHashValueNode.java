/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.format.read.array;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.format.FormatNode;
import org.jruby.truffle.core.format.read.SourceNode;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.control.RaiseException;

@NodeChildren({
        @NodeChild(value = "source", type = SourceNode.class),
})
public abstract class ReadHashValueNode extends FormatNode {

    private final Object key;

    private final ConditionProfile oneHashProfile = ConditionProfile.createBinaryProfile();

    public ReadHashValueNode(RubyContext context, Object key) {
        super(context);
        this.key = key;
    }

    @Specialization
    @TruffleBoundary
    public Object read(Object[] source) {
        if (oneHashProfile.profile(source.length != 1 || !RubyGuards.isRubyHash(source[0]))) {
            throw new RaiseException(getContext().getCoreLibrary().argumentErrorOneHashRequired(this));
        }

        // We're not in a Ruby frame here, so we can't run arbitrary Ruby nodes like Hash#[]. Instead use a slow send.

        return getContext().send(source[0], "fetch", null, key);
    }

}
