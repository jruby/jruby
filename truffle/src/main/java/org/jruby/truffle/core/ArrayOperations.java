/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.core.array.ArrayReflector;
import org.jruby.truffle.core.array.ArrayUtils;

import java.util.Arrays;

public abstract class ArrayOperations {

    public static int normalizeIndex(int length, int index, ConditionProfile negativeIndexProfile) {
        if (negativeIndexProfile.profile(index < 0)) {
            return length + index;
        } else {
            return index;
        }
    }

    public static int normalizeIndex(int length, int index) {
        if (CompilerDirectives.injectBranchProbability(CompilerDirectives.SLOWPATH_PROBABILITY, index < 0)) {
            return length + index;
        } else {
            return index;
        }
    }

    public static int clampExclusiveIndex(int length, int index) {
        if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, index < 0)) {
            return 0;
        } else if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, index > length)) {
            return length;
        } else {
            return index;
        }
    }

    public static Object[] toObjectArray(DynamicObject array) {
        return ArrayReflector.reflect(Layouts.ARRAY.getStore(array)).getBoxedCopy(Layouts.ARRAY.getSize(array));
    }

    public static Iterable<Object> toIterable(DynamicObject array) {
        return ArrayReflector.reflect(Layouts.ARRAY.getStore(array)).iterableUntil(Layouts.ARRAY.getSize(array));
    }

    public static void append(DynamicObject array, Object value) {
        assert RubyGuards.isRubyArray(array);
        Layouts.ARRAY.setStore(array, Arrays.copyOf(ArrayUtils.box(Layouts.ARRAY.getStore(array)), Layouts.ARRAY.getSize(array) + 1));
        Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array));
        ((Object[]) Layouts.ARRAY.getStore(array))[Layouts.ARRAY.getSize(array)] = value;
        Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array) + 1);
    }

}
