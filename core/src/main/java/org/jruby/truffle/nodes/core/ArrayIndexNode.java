/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.nodes.*;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.array.RubyArray;

/**
 * Index an array, without using any method lookup. This isn't a call - it's an operation on a core
 * class.
 */
@NodeInfo(shortName = "array-index")
@NodeChildren({@NodeChild(value = "array", type = RubyNode.class)})
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

    @Specialization(guards = "isIntegerFixnum", rewriteOn=UnexpectedResultException.class, order = 2)
    public int getIntegerFixnumInBounds(RubyArray array) throws UnexpectedResultException {
        notDesignedForCompilation();

        int normalisedIndex = array.normaliseIndex(index);

        if (normalisedIndex < 0) {
            throw new UnexpectedResultException(NilPlaceholder.INSTANCE);
        } else if (normalisedIndex >= array.size) {
            throw new UnexpectedResultException(NilPlaceholder.INSTANCE);
        } else {
            return ((int[]) array.store)[normalisedIndex];
        }
    }

    @Specialization(guards = "isIntegerFixnum", order = 3)
    public Object getIntegerFixnum(RubyArray array) {
        notDesignedForCompilation();

        int normalisedIndex = array.normaliseIndex(index);

        if (normalisedIndex < 0) {
            return NilPlaceholder.INSTANCE;
        } else if (normalisedIndex >= array.size) {
            return NilPlaceholder.INSTANCE;
        } else {
            return ((int[]) array.store)[normalisedIndex];
        }
    }

    @Specialization(guards = "isLongFixnum", rewriteOn=UnexpectedResultException.class, order = 4)
    public long getLongFixnumInBounds(RubyArray array) throws UnexpectedResultException {
        notDesignedForCompilation();

        int normalisedIndex = array.normaliseIndex(index);

        if (normalisedIndex < 0) {
            throw new UnexpectedResultException(NilPlaceholder.INSTANCE);
        } else if (normalisedIndex >= array.size) {
            throw new UnexpectedResultException(NilPlaceholder.INSTANCE);
        } else {
            return ((long[]) array.store)[normalisedIndex];
        }
    }

    @Specialization(guards = "isLongFixnum", order = 5)
    public Object getLongFixnum(RubyArray array) {
        notDesignedForCompilation();

        int normalisedIndex = array.normaliseIndex(index);

        if (normalisedIndex < 0) {
            return NilPlaceholder.INSTANCE;
        } else if (normalisedIndex >= array.size) {
            return NilPlaceholder.INSTANCE;
        } else {
            return ((long[]) array.store)[normalisedIndex];
        }
    }

    @Specialization(guards = "isFloat", rewriteOn=UnexpectedResultException.class, order = 6)
    public double getFloatInBounds(RubyArray array) throws UnexpectedResultException {
        notDesignedForCompilation();

        int normalisedIndex = array.normaliseIndex(index);

        if (normalisedIndex < 0) {
            throw new UnexpectedResultException(NilPlaceholder.INSTANCE);
        } else if (normalisedIndex >= array.size) {
            throw new UnexpectedResultException(NilPlaceholder.INSTANCE);
        } else {
            return ((double[]) array.store)[normalisedIndex];
        }
    }

    @Specialization(guards = "isIntegerFixnum", order = 7)
    public Object getFloat(RubyArray array) {
        notDesignedForCompilation();

        int normalisedIndex = array.normaliseIndex(index);

        if (normalisedIndex < 0) {
            return NilPlaceholder.INSTANCE;
        } else if (normalisedIndex >= array.size) {
            return NilPlaceholder.INSTANCE;
        } else {
            return ((double[]) array.store)[normalisedIndex];
        }
    }

    @Specialization(guards = "isObject", order = 8)
    public Object getObject(RubyArray array) {
        notDesignedForCompilation();

        int normalisedIndex = array.normaliseIndex(index);

        if (normalisedIndex < 0) {
            return NilPlaceholder.INSTANCE;
        } else if (normalisedIndex >= array.size) {
            return NilPlaceholder.INSTANCE;
        } else {
            return ((Object[]) array.store)[normalisedIndex];
        }
    }

    // TODO(CS): copied and pasted from ArrayCoreMethodNode - need a way to use statics from other classes in the DSL

    protected boolean isNull(RubyArray array) {
        return array.store == null;
    }

    protected boolean isIntegerFixnum(RubyArray array) {
        return array.store instanceof int[];
    }

    protected boolean isLongFixnum(RubyArray array) {
        return array.store instanceof long[];
    }

    protected boolean isFloat(RubyArray array) {
        return array.store instanceof double[];
    }

    protected boolean isObject(RubyArray array) {
        return array.store instanceof Object[];
    }

    protected boolean isOtherNull(RubyArray array, RubyArray other) {
        return other.store == null;
    }

    protected boolean isOtherIntegerFixnum(RubyArray array, RubyArray other) {
        return other.store instanceof int[];
    }

    protected boolean isOtherLongFixnum(RubyArray array, RubyArray other) {
        return other.store instanceof long[];
    }

    protected boolean isOtherFloat(RubyArray array, RubyArray other) {
        return other.store instanceof double[];
    }

    protected boolean isOtherObject(RubyArray array, RubyArray other) {
        return other.store instanceof Object[];
    }

    protected boolean areBothNull(RubyArray a, RubyArray b) {
        return a.store == null && b.store == null;
    }

    protected boolean areBothIntegerFixnum(RubyArray a, RubyArray b) {
        return a.store instanceof int[] && b.store instanceof int[];
    }

    protected boolean areBothLongFixnum(RubyArray a, RubyArray b) {
        return a.store instanceof long[] && b.store instanceof long[];
    }

    protected boolean areBothFloat(RubyArray a, RubyArray b) {
        return a.store instanceof double[] && b.store instanceof double[];
    }

    protected boolean areBothObject(RubyArray a, RubyArray b) {
        return a.store instanceof Object[] && b.store instanceof Object[];
    }

}
