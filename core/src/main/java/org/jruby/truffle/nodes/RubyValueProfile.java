/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes;

import com.oracle.truffle.api.CompilerDirectives;

public class RubyValueProfile {

    private static final Object UNINITIALIZED = new Object();
    private static final Object GENERIC = new Object();

    @CompilerDirectives.CompilationFinal
    private Object cachedValue = UNINITIALIZED;

    public Object profile(Object value) {
        if (cachedValue != GENERIC) {
            if (cachedValue instanceof Boolean && value instanceof Boolean
                    && (boolean) cachedValue == (boolean) value) {
                return cachedValue;
            } else if (cachedValue instanceof Integer && value instanceof Integer
                    && (int) cachedValue == (int) value) {
                return cachedValue;
            } else if (cachedValue instanceof Long && value instanceof Long
                    && (long) cachedValue == (long) value) {
                return cachedValue;
            } else if (cachedValue instanceof Double && value instanceof Double
                    && exactCompare((double) cachedValue, (double) value)) {
                return cachedValue;
            } else {
                cacheMiss(value);
            }
        }
        return value;
    }

    public boolean profile(boolean value) {
        if (cachedValue != GENERIC) {
            if (cachedValue instanceof Integer && (boolean) cachedValue == value) {
                return (boolean) cachedValue;
            } else {
                cacheMiss(value);
            }
        }
        return value;
    }

    public int profile(int value) {
        if (cachedValue != GENERIC) {
            if (cachedValue instanceof Integer && (int) cachedValue == value) {
                return (int) cachedValue;
            } else {
                cacheMiss(value);
            }
        }
        return value;
    }

    public long profile(long value) {
        if (cachedValue != GENERIC) {
            if (cachedValue instanceof Integer && (long) cachedValue == value) {
                return (long) cachedValue;
            } else {
                cacheMiss(value);
            }
        }
        return value;
    }

    public double profile(double value) {
        if (cachedValue != GENERIC) {
            if (cachedValue instanceof Double && exactCompare((double) cachedValue, value)) {
                return (int) cachedValue;
            } else {
                cacheMiss(value);
            }
        }
        return value;
    }

    private void cacheMiss(Object value) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        if (cachedValue == UNINITIALIZED) {
            cachedValue = value;
        } else {
            cachedValue = GENERIC;
        }
    }

    private static boolean exactCompare(double a, double b) {
        // -0.0 == 0.0, but you can tell the difference through other means so need to know the difference
        return Double.doubleToRawLongBits(a) == Double.doubleToRawLongBits(b);
    }

}
