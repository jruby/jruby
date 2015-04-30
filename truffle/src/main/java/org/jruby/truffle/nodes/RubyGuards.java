/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes;

import com.oracle.truffle.api.interop.TruffleObject;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.core.*;

public abstract class RubyGuards {

    public static boolean isUndefinedPlaceholder(Object value) {
        return value instanceof UndefinedPlaceholder;
    }

    public static boolean isBoolean(Object value) {
        return value instanceof Boolean;
    }

    public static boolean isInteger(Object value) {
        return value instanceof Integer;
    }

    public static boolean isLong(Object value) {
        return value instanceof Long;
    }

    public static boolean isDouble(Object value) {
        return value instanceof Double;
    }

    public static boolean isRubyBignum(Object value) {
        return value instanceof RubyBignum;
    }

    public static boolean isIntegerFixnumRange(Object value) {
        return value instanceof RubyRange.IntegerFixnumRange;
    }

    public static boolean isRubyArray(Object value) {
        return value instanceof RubyArray;
    }

    public static boolean isRubyBinding(Object value) {
        return value instanceof RubyBinding;
    }

    public static boolean isRubyClass(Object value) {
        return value instanceof RubyClass;
    }

    public static boolean isRubyHash(Object value) {
        return value instanceof RubyHash;
    }

    public static boolean isRubyModule(Object value) {
        return value instanceof RubyModule;
    }

    public static boolean isRubyNilClass(Object value) {
        return value instanceof RubyNilClass;
    }

    public static boolean isRubyRange(Object value) {
        return value instanceof RubyRange;
    }

    public static boolean isRubyRegexp(Object value) {
        return value instanceof RubyRegexp;
    }

    public static boolean isRubyString(Object value) {
        return value instanceof RubyString;
    }

    public static boolean isRubyEncoding(Object value) {
        return value instanceof RubyEncoding;
    }

    public static boolean isRubySymbol(Object value) {
        return value instanceof RubySymbol;
    }

    public static boolean isRubyMethod(Object value) {
        return value instanceof RubyMethod;
    }

    public static boolean isRubyUnboundMethod(Object value) {
        return value instanceof RubyUnboundMethod;
    }

    public static boolean isRubyBasicObject(Object value) {
        return value instanceof RubyBasicObject;
    }

    public static boolean isThreadLocal(Object value) {
        return value instanceof ThreadLocal;
    }

    public static boolean isForeignObject(Object object) {
        return (object instanceof TruffleObject) && !(object instanceof RubyBasicObject);
    }

    public static boolean isTrue(boolean value) {
        return value;
    }

    public static boolean isNaN(double value) {
        return Double.isNaN(value);
    }

    public static boolean isInfinity(double value) {
        return Double.isInfinite(value);
    }

}
