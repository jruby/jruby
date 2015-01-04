/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.dsl.ImportGuards;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyNilClass;

/**
 * Index an array, without using any method lookup. This isn't a call - it's an operation on a core
 * class.
 */
@NodeChildren({@NodeChild(value = "array", type = RubyNode.class)})
@ImportGuards(ArrayGuards.class)
public abstract class ArrayIndexNode extends RubyNode {

    final int index;

    public ArrayIndexNode(RubyContext context, SourceSection sourceSection, int index) {
        super(context, sourceSection);
        this.index = index;
    }

    public ArrayIndexNode(ArrayIndexNode prev) {
        super(prev);
        index = prev.index;
    }

    @Specialization(guards = "isNull")
    public RubyNilClass getNull(RubyArray array) {
        return getContext().getCoreLibrary().getNilObject();
    }

    @Specialization(guards = "isIntegerFixnum", rewriteOn=UnexpectedResultException.class)
    public int getIntegerFixnumInBounds(RubyArray array) throws UnexpectedResultException {
        int normalisedIndex = array.normaliseIndex(index);

        if (normalisedIndex < 0 || normalisedIndex >= array.getSize()) {
            throw new UnexpectedResultException(getContext().getCoreLibrary().getNilObject());
        } else {
            return ((int[]) array.getStore())[normalisedIndex];
        }
    }

    @Specialization(guards = "isIntegerFixnum")
    public Object getIntegerFixnum(RubyArray array) {
        int normalisedIndex = array.normaliseIndex(index);

        if (normalisedIndex < 0 || normalisedIndex >= array.getSize()) {
            return getContext().getCoreLibrary().getNilObject();
        } else {
            return ((int[]) array.getStore())[normalisedIndex];
        }
    }

    @Specialization(guards = "isLongFixnum", rewriteOn=UnexpectedResultException.class)
    public long getLongFixnumInBounds(RubyArray array) throws UnexpectedResultException {
        int normalisedIndex = array.normaliseIndex(index);

        if (normalisedIndex < 0 || normalisedIndex >= array.getSize()) {
            throw new UnexpectedResultException(getContext().getCoreLibrary().getNilObject());
        } else {
            return ((long[]) array.getStore())[normalisedIndex];
        }
    }

    @Specialization(guards = "isLongFixnum")
    public Object getLongFixnum(RubyArray array) {
        int normalisedIndex = array.normaliseIndex(index);

        if (normalisedIndex < 0 || normalisedIndex >= array.getSize()) {
            return getContext().getCoreLibrary().getNilObject();
        } else {
            return ((long[]) array.getStore())[normalisedIndex];
        }
    }

    @Specialization(guards = "isFloat", rewriteOn=UnexpectedResultException.class)
    public double getFloatInBounds(RubyArray array) throws UnexpectedResultException {
        int normalisedIndex = array.normaliseIndex(index);

        if (normalisedIndex < 0 || normalisedIndex >= array.getSize()) {
            throw new UnexpectedResultException(getContext().getCoreLibrary().getNilObject());
        } else {
            return ((double[]) array.getStore())[normalisedIndex];
        }
    }

    @Specialization(guards = "isFloat")
    public Object getFloat(RubyArray array) {
        int normalisedIndex = array.normaliseIndex(index);

        if (normalisedIndex < 0 || normalisedIndex >= array.getSize()) {
            return getContext().getCoreLibrary().getNilObject();
        } else {
            return ((double[]) array.getStore())[normalisedIndex];
        }
    }

    @Specialization(guards = "isObject")
    public Object getObject(RubyArray array) {
        int normalisedIndex = array.normaliseIndex(index);

        if (normalisedIndex < 0 || normalisedIndex >= array.getSize()) {
            return getContext().getCoreLibrary().getNilObject();
        } else {
            return ((Object[]) array.getStore())[normalisedIndex];
        }
    }

}
