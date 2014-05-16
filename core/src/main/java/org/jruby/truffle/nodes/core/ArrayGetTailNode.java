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

import com.oracle.truffle.api.SourceSection;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.array.*;

import java.util.Arrays;

@NodeInfo(shortName = "array-tail")
@NodeChildren({@NodeChild(value = "array", type = RubyNode.class)})
public abstract class ArrayGetTailNode extends RubyNode {

    final int index;

    public ArrayGetTailNode(RubyContext context, SourceSection sourceSection, int index) {
        super(context, sourceSection);
        this.index = index;
    }

    public ArrayGetTailNode(ArrayGetTailNode prev) {
        super(prev);
        index = prev.index;
    }

    @Specialization(guards = "isNull", order = 1)
    public RubyArray getTailNull(RubyArray array) {
        return new RubyArray(getContext().getCoreLibrary().getArrayClass());
    }

    @Specialization(guards = "isIntegerFixnum", order = 2)
    public RubyArray getTailIntegerFixnum(RubyArray array) {
        return new RubyArray(getContext().getCoreLibrary().getArrayClass(), Arrays.copyOfRange((int[]) array.store, index, array.size - index), array.size - 1);
    }

    @Specialization(guards = "isLongFixnum", order = 3)
    public RubyArray getTailLongFixnum(RubyArray array) {
        return new RubyArray(getContext().getCoreLibrary().getArrayClass(), Arrays.copyOfRange((long[]) array.store, index, array.size - index), array.size - 1);
    }

    @Specialization(guards = "isFloat", order = 4)
    public RubyArray getTailFloat(RubyArray array) {
        return new RubyArray(getContext().getCoreLibrary().getArrayClass(), Arrays.copyOfRange((double[]) array.store, index, array.size - index), array.size - 1);
    }

    @Specialization(guards = "isObject", order = 5)
    public RubyArray getTailObject(RubyArray array) {
        return new RubyArray(getContext().getCoreLibrary().getArrayClass(), Arrays.copyOfRange((Object[]) array.store, index, array.size - index), array.size - 1);
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
