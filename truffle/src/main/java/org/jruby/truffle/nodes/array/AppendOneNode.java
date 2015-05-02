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

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.ArrayGuards;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.util.ArrayUtils;

import java.util.Arrays;

@NodeChildren({
        @NodeChild("array"),
        @NodeChild("value"),
})
@ImportStatic(ArrayGuards.class)
public abstract class AppendOneNode extends RubyNode {

    public AppendOneNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public abstract RubyArray executeAppendOne(RubyArray array, Object value);

    // Append into an array with null storage

    @Specialization(guards = "isNull(array)")
    public RubyArray appendNull(RubyArray array, int value) {
        array.setStore(new int[]{value}, 1);
        return array;
    }

    @Specialization(guards = "isNull(array)")
    public RubyArray appendNull(RubyArray array, long value) {
        array.setStore(new long[]{value}, 1);
        return array;
    }

    @Specialization(guards = "isNull(array)")
    public RubyArray appendNull(RubyArray array, Object value) {
        array.setStore(new Object[]{value}, 1);
        return array;
    }

    // Append into empty, but non-null storage; we would be better off reusing any existing space, but don't worry for now

    @Specialization(guards = {"!isNull(array)", "isEmpty(array)"})
    public RubyArray appendEmpty(RubyArray array, int value) {
        array.setStore(new int[]{value}, 1);
        return array;
    }

    @Specialization(guards = {"!isNull(array)", "isEmpty(array)"})
    public RubyArray appendEmpty(RubyArray array, long value) {
        array.setStore(new long[]{value}, 1);
        return array;
    }

    @Specialization(guards ={"!isNull(array)", "isEmpty(array)"})
    public RubyArray appendEmpty(RubyArray array, Object value) {
        array.setStore(new Object[]{value}, 1);
        return array;
    }

    // Append of the correct type

    @Specialization(guards = "isIntegerFixnum(array)")
    public RubyArray appendInteger(RubyArray array, int value,
                                   @Cached("createBinaryProfile()") ConditionProfile extendProfile) {
        final int oldSize = array.getSize();
        final int newSize = oldSize + 1;

        int[] store = (int[]) array.getStore();

        if (extendProfile.profile(newSize > store.length)) {
            store = Arrays.copyOf(store, ArrayUtils.capacity(store.length, newSize));
        }

        store[oldSize] = value;
        array.setStore(store, newSize);
        return array;
    }

    @Specialization(guards = "isLongFixnum(array)")
    public RubyArray appendLong(RubyArray array, long value,
                                @Cached("createBinaryProfile()") ConditionProfile extendProfile) {
        final int oldSize = array.getSize();
        final int newSize = oldSize + 1;

        long[] store = (long[]) array.getStore();

        if (extendProfile.profile(newSize > store.length)) {
            store = Arrays.copyOf(store, ArrayUtils.capacity(store.length, newSize));
        }

        store[oldSize] = value;
        array.setStore(store, newSize);
        return array;
    }

    @Specialization(guards = "isObject(array)")
    public RubyArray appendObject(RubyArray array, Object value,
                                  @Cached("createBinaryProfile()") ConditionProfile extendProfile) {
        final int oldSize = array.getSize();
        final int newSize = oldSize + 1;

        Object[] store = (Object[]) array.getStore();

        if (extendProfile.profile(newSize > store.length)) {
            store = Arrays.copyOf(store, ArrayUtils.capacity(store.length, newSize));
        }

        store[oldSize] = value;
        array.setStore(store, newSize);
        return array;
    }

    // Append forcing a generalization

    @Specialization(guards = "isIntegerFixnum(array)")
    public RubyArray appendLongIntoInteger(RubyArray array, long value) {
        final int oldSize = array.getSize();
        final int newSize = oldSize + 1;

        final int[] oldStore = (int[]) array.getStore();
        long[] newStore = ArrayUtils.longCopyOf(oldStore, ArrayUtils.capacity(oldStore.length, newSize));

        newStore[oldSize] = value;
        array.setStore(newStore, newSize);
        return array;
    }

    @Specialization(guards = {"isIntegerFixnum(array)", "!isInteger(value)", "!isLong(value)"})
    public RubyArray appendObjectIntoInteger(RubyArray array, Object value) {
        final int oldSize = array.getSize();
        final int newSize = oldSize + 1;

        final int[] oldStore = (int[]) array.getStore();
        Object[] newStore = ArrayUtils.box(oldStore, ArrayUtils.capacity(oldStore.length, newSize) - oldStore.length);

        newStore[oldSize] = value;
        array.setStore(newStore, newSize);
        return array;
    }

    @Specialization(guards = {"isLongFixnum(array)", "!isInteger(value)", "!isLong(value)"})
    public RubyArray appendObjectIntoLong(RubyArray array, Object value) {
        final int oldSize = array.getSize();
        final int newSize = oldSize + 1;

        final long[] oldStore = (long[]) array.getStore();
        Object[] newStore = ArrayUtils.box(oldStore, ArrayUtils.capacity(oldStore.length, newSize) - oldStore.length);

        newStore[oldSize] = value;
        array.setStore(newStore, newSize);
        return array;
    }

}
