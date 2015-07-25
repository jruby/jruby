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
import org.jruby.truffle.nodes.core.*;
import org.jruby.truffle.nodes.core.array.ArrayNodes;
import org.jruby.truffle.nodes.core.hash.HashNodes;
import org.jruby.truffle.nodes.ext.BigDecimalNodes;
import org.jruby.truffle.nodes.rubinius.ByteArrayNodes;
import org.jruby.truffle.nodes.rubinius.PointerNodes;
import org.jruby.truffle.runtime.NotProvided;
import org.jruby.truffle.runtime.ThreadLocalObject;
import org.jruby.truffle.runtime.core.*;

public abstract class RubyGuards {

    // Basic Java types

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

    // Ruby types

    public static boolean isRubyBignum(Object value) {
        return (value instanceof RubyBasicObject) && isRubyBignum((RubyBasicObject) value);
    }

    public static boolean isRubyBignum(RubyBasicObject value) {
        return value.getDynamicObject().getShape().getObjectType() == BignumNodes.BIGNUM_TYPE;
    }

    public static boolean isRubyBigDecimal(RubyBasicObject value) {
        return value.getDynamicObject().getShape().getObjectType() == BigDecimalNodes.BIG_DECIMAL_TYPE;
    }

    public static boolean isIntegerFixnumRange(Object object) {
        return (object instanceof RubyBasicObject) && isIntegerFixnumRange((RubyBasicObject) object);
    }

    public static boolean isIntegerFixnumRange(RubyBasicObject object) {
        return RangeNodes.INTEGER_FIXNUM_RANGE_LAYOUT.isIntegerFixnumRange(object.getDynamicObject());
    }

    public static boolean isLongFixnumRange(Object object) {
        return (object instanceof RubyBasicObject) && isLongFixnumRange((RubyBasicObject) object);
    }

    public static boolean isLongFixnumRange(RubyBasicObject object) {
        return RangeNodes.LONG_FIXNUM_RANGE_LAYOUT.isLongFixnumRange(object.getDynamicObject());
    }

    public static boolean isObjectRange(Object object) {
        return (object instanceof RubyBasicObject) && isObjectRange((RubyBasicObject) object);
    }

    public static boolean isObjectRange(RubyBasicObject object) {
        return RangeNodes.OBJECT_RANGE_LAYOUT.isObjectRange(object.getDynamicObject());
    }

    public static boolean isRubyRange(Object value) {
        return isIntegerFixnumRange(value) || isLongFixnumRange(value) || isObjectRange(value);
    }

    public static boolean isRubyArray(Object value) {
        return (value instanceof RubyBasicObject) && isRubyArray((RubyBasicObject) value);
    }

    public static boolean isRubyArray(RubyBasicObject value) {
        return ArrayNodes.ARRAY_LAYOUT.isArray(value.getDynamicObject());
    }

    public static boolean isRubyBinding(RubyBasicObject object) {
        return BindingNodes.BINDING_LAYOUT.isBinding(object.getDynamicObject());
    }

    public static boolean isRubyClass(Object value) {
        return (value instanceof RubyBasicObject) && isRubyClass((RubyBasicObject) value);
    }

    public static boolean isRubyClass(RubyBasicObject value) {
        return ClassNodes.CLASS_LAYOUT.isClass(value.getDynamicObject());
    }

    public static boolean isRubyHash(Object value) {
        return (value instanceof RubyBasicObject) && isRubyHash((RubyBasicObject) value);
    }

    public static boolean isRubyHash(RubyBasicObject value) {
        return HashNodes.HASH_LAYOUT.isHash(value.getDynamicObject());
    }

    public static boolean isRubyModule(Object value) {
        return value instanceof RubyModule;
    }

    public static boolean isRubyRegexp(Object value) {
        return (value instanceof RubyBasicObject) && isRubyRegexp((RubyBasicObject) value);
    }

    public static boolean isRubyRegexp(RubyBasicObject value) {
        return RegexpNodes.REGEXP_LAYOUT.isRegexp(value.getDynamicObject());
    }

    public static boolean isRubyString(Object value) {
        return (value instanceof RubyBasicObject) && isRubyString((RubyBasicObject) value);
    }

    public static boolean isRubyString(RubyBasicObject value) {
        return StringNodes.STRING_LAYOUT.isString(value.getDynamicObject());
    }

    public static boolean isRubyEncoding(Object object) {
        return (object instanceof RubyBasicObject) && isRubyEncoding((RubyBasicObject) object);
    }

    public static boolean isRubyEncoding(RubyBasicObject object) {
        return EncodingNodes.ENCODING_LAYOUT.isEncoding(object.getDynamicObject());
    }

    public static boolean isRubySymbol(Object value) {
        return (value instanceof RubyBasicObject) && isRubySymbol((RubyBasicObject) value);
    }

    public static boolean isRubySymbol(RubyBasicObject value) {
        return value.getDynamicObject().getShape().getObjectType() == SymbolNodes.SYMBOL_TYPE;
    }

    public static boolean isRubyMethod(Object value) {
        return (value instanceof RubyBasicObject) && isRubyMethod((RubyBasicObject) value);
    }

    public static boolean isRubyMethod(RubyBasicObject value) {
        return value.getDynamicObject().getShape().getObjectType() == MethodNodes.METHOD_TYPE;
    }

    public static boolean isRubyUnboundMethod(Object value) {
        return (value instanceof RubyBasicObject) && isRubyUnboundMethod((RubyBasicObject) value);
    }

    public static boolean isRubyUnboundMethod(RubyBasicObject value) {
        return value.getDynamicObject().getShape().getObjectType() == UnboundMethodNodes.UNBOUND_METHOD_TYPE;
    }

    public static boolean isRubyMutex(RubyBasicObject value) {
        return value.getDynamicObject().getShape().getObjectType() == MutexNodes.MUTEX_TYPE;
    }

    public static boolean isRubyBasicObject(Object value) {
        return value instanceof RubyBasicObject;
    }

    public static boolean isRubyPointer(Object value) {
        return (value instanceof RubyBasicObject) && isRubyPointer((RubyBasicObject) value);
    }

    public static boolean isRubyPointer(RubyBasicObject value) {
        return value.getDynamicObject().getShape().getObjectType() == PointerNodes.POINTER_TYPE;
    }

    public static boolean isRubiniusByteArray(Object value) {
        return (value instanceof RubyBasicObject) && isRubiniusByteArray((RubyBasicObject) value);
    }

    public static boolean isRubiniusByteArray(RubyBasicObject value) {
        return value.getDynamicObject().getShape().getObjectType() == ByteArrayNodes.BYTE_ARRAY_TYPE;
    }

    public static boolean isRubyProc(Object object) {
        return (object instanceof RubyBasicObject) && isRubyProc((RubyBasicObject) object);
    }

    public static boolean isRubyProc(RubyBasicObject object) {
        return ProcNodes.PROC_LAYOUT.isProc(object.getDynamicObject());
    }

    public static boolean isRubyEncodingConverter(RubyBasicObject encodingConverter) {
        return EncodingConverterNodes.ENCODING_CONVERTER_LAYOUT.isEncodingConverter(encodingConverter.getDynamicObject());
    }

    public static boolean isRubyTime(RubyBasicObject object) {
        return TimeNodes.TIME_LAYOUT.isTime(object.getDynamicObject());
    }

    public static boolean isRubyException(Object value) {
        return (value instanceof RubyBasicObject) && isRubyException((RubyBasicObject) value);
    }

    public static boolean isRubyException(RubyBasicObject object) {
        return ExceptionNodes.EXCEPTION_LAYOUT.isException(object.getDynamicObject());
    }

    public static boolean isRubyFiber(Object object) {
        return (object instanceof RubyBasicObject) && isRubyFiber((RubyBasicObject) object);
    }

    public static boolean isRubyFiber(RubyBasicObject object) {
        return FiberNodes.FIBER_LAYOUT.isFiber(object.getDynamicObject());
    }

    public static boolean isRubyThread(Object object) {
        return (object instanceof RubyBasicObject) && isRubyThread((RubyBasicObject) object);
    }

    public static boolean isRubyThread(RubyBasicObject object) {
        return ThreadNodes.THREAD_LAYOUT.isThread(object.getDynamicObject());
    }

    public static boolean isRubyMatchData(Object object) {
        return (object instanceof RubyBasicObject) && isRubyMatchData((RubyBasicObject) object);
    }

    public static boolean isRubyMatchData(RubyBasicObject object) {
        return MatchDataNodes.MATCH_DATA_LAYOUT.isMatchData(object.getDynamicObject());
    }

    // Internal types

    public static boolean isThreadLocal(Object value) {
        return value instanceof ThreadLocalObject;
    }

    public static boolean isForeignObject(Object object) {
        return (object instanceof TruffleObject) && !(object instanceof RubyBasicObject);
    }

    // Sentinels

    public static boolean wasProvided(Object value) {
        return !(wasNotProvided(value));
    }

    public static boolean wasNotProvided(Object value) {
        return value == NotProvided.INSTANCE;
    }

    // Values

    public static boolean isNaN(double value) {
        return Double.isNaN(value);
    }

    public static boolean isInfinity(double value) {
        return Double.isInfinite(value);
    }
}
