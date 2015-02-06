/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.array;

import com.oracle.truffle.api.dsl.ImportGuards;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.ArrayGuards;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyNilClass;

/**
 * Does simple indexing of an array with a single integral index value that may be in or out of bounds. You may often
 * want to use this node with an index that is a {@link NewFixnumLowerNode} so that the node will accept a {@code long}
 * within range as well as {@code int}, but we do not require this as sometimes it is redundant.
 */
@NodeChildren({
        @NodeChild(value="array", type=RubyNode.class),
        @NodeChild(value="index", type=RubyNode.class)
})
@ImportGuards(ArrayGuards.class)
public abstract class ArrayReadNode extends RubyNode {

    public ArrayReadNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public ArrayReadNode(ArrayReadNode prev) {
        super(prev);
    }

    public abstract Object executeRead(VirtualFrame frame, RubyArray array, int index);

    @Specialization(
            guards="isNullArray"
    )
    public RubyNilClass readNull(RubyArray array, int index) {
        return getContext().getCoreLibrary().getNilObject();
    }

    @Specialization(
            guards="isIntegerArray",
            rewriteOn=UnexpectedResultException.class
    )
    public int readIntegerInBounds(RubyArray array, int index) throws UnexpectedResultException {
        int normalisedIndex = array.normaliseIndex(index);

        if (normalisedIndex < 0 || normalisedIndex >= array.getSize()) {
            throw new UnexpectedResultException(getContext().getCoreLibrary().getNilObject());
        } else {
            return ((int[]) array.getStore())[normalisedIndex];
        }
    }

    @Specialization(
            guards="isIntegerArray",
            contains="readIntegerInBounds"
    )
    public Object readIntegerMaybeOutOfBounds(RubyArray array, int index) {
        int normalisedIndex = array.normaliseIndex(index);

        if (normalisedIndex < 0 || normalisedIndex >= array.getSize()) {
            return getContext().getCoreLibrary().getNilObject();
        } else {
            return ((int[]) array.getStore())[normalisedIndex];
        }
    }

    @Specialization(
            guards="isLongArray",
            rewriteOn=UnexpectedResultException.class
    )
    public long readLongInBounds(RubyArray array, int index) throws UnexpectedResultException {
        int normalisedIndex = array.normaliseIndex(index);

        if (normalisedIndex < 0 || normalisedIndex >= array.getSize()) {
            throw new UnexpectedResultException(getContext().getCoreLibrary().getNilObject());
        } else {
            return ((long[]) array.getStore())[normalisedIndex];
        }
    }

    @Specialization(
            guards="isLongArray",
            contains="readLongInBounds"
    )
    public Object readLongMaybeOutOfBounds(RubyArray array, int index) {
        int normalisedIndex = array.normaliseIndex(index);

        if (normalisedIndex < 0 || normalisedIndex >= array.getSize()) {
            return getContext().getCoreLibrary().getNilObject();
        } else {
            return ((long[]) array.getStore())[normalisedIndex];
        }
    }

    @Specialization(
            guards="isDoubleArray",
            rewriteOn=UnexpectedResultException.class
    )
    public double readDoubleInBounds(RubyArray array, int index) throws UnexpectedResultException {
        int normalisedIndex = array.normaliseIndex(index);

        if (normalisedIndex < 0 || normalisedIndex >= array.getSize()) {
            throw new UnexpectedResultException(getContext().getCoreLibrary().getNilObject());
        } else {
            return ((double[]) array.getStore())[normalisedIndex];
        }
    }

    @Specialization(
            guards="isDoubleArray",
            contains="readDoubleInBounds"
    )
    public Object readDoubleMaybeOutOfBounds(RubyArray array, int index) {
        int normalisedIndex = array.normaliseIndex(index);

        if (normalisedIndex < 0 || normalisedIndex >= array.getSize()) {
            return getContext().getCoreLibrary().getNilObject();
        } else {
            return ((double[]) array.getStore())[normalisedIndex];
        }
    }

    @Specialization(
            guards="isObjectArray",
            rewriteOn=UnexpectedResultException.class
    )
    public Object readObjectInBounds(RubyArray array, int index) throws UnexpectedResultException {
        int normalisedIndex = array.normaliseIndex(index);

        if (normalisedIndex < 0 || normalisedIndex >= array.getSize()) {
            throw new UnexpectedResultException(getContext().getCoreLibrary().getNilObject());
        } else {
            return ((Object[]) array.getStore())[normalisedIndex];
        }
    }

    @Specialization(
            guards="isObjectArray",
            contains="readObjectInBounds"
    )
    public Object readObjectMaybeOutOfBounds(RubyArray array, int index) {
        int normalisedIndex = array.normaliseIndex(index);

        if (normalisedIndex < 0 || normalisedIndex >= array.getSize()) {
            return getContext().getCoreLibrary().getNilObject();
        } else {
            return ((Object[]) array.getStore())[normalisedIndex];
        }
    }

}
