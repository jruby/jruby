/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core.array;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.CoreLibrary;

/**
 * Passes through {@code int} values unmodified, but will convert a {@code long} value to an {@code int}, if it fits
 * within the range of an {@code int}. Leaves all other values unmodified. Used where a specialisation only accepts
 * {@code int}, such as Java array indexing, but we would like to also handle {@code long} if they also fit within an
 * {@code int}.
 */
@NodeChild(value="value", type=RubyNode.class)
public abstract class NewFixnumLowerNode extends RubyNode {

    public NewFixnumLowerNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    @Specialization
    public int lower(int value) {
        return value;
    }

    @Specialization(guards="canLower(value)")
    public int lower(long value) {
        return (int) value;
    }

    @Specialization(guards="!canLower(value)")
    public long lowerFails(long value) {
        return value;
    }

    protected static boolean canLower(long value) {
        return CoreLibrary.fitsIntoInteger(value);
    }

}
