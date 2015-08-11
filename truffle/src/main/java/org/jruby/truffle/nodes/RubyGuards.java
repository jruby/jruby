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
import com.oracle.truffle.api.object.DynamicObject;
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

    public static boolean isRubyBasicObject(Object object) {
        return BasicObjectNodes.BASIC_OBJECT_LAYOUT.isBasicObject(object);
    }

    public static boolean isRubyBignum(Object value) {
        return (value instanceof DynamicObject) && isRubyBignum((DynamicObject) value);
    }

    public static boolean isRubyBignum(DynamicObject value) {
        return BignumNodes.BIGNUM_LAYOUT.isBignum(BasicObjectNodes.getDynamicObject(value));
    }

    public static boolean isRubyBigDecimal(DynamicObject value) {
        return BigDecimalNodes.BIG_DECIMAL_LAYOUT.isBigDecimal(BasicObjectNodes.getDynamicObject(value));
    }

    public static boolean isIntegerFixnumRange(Object object) {
        return (object instanceof DynamicObject) && isIntegerFixnumRange((DynamicObject) object);
    }

    public static boolean isIntegerFixnumRange(DynamicObject object) {
        return RangeNodes.INTEGER_FIXNUM_RANGE_LAYOUT.isIntegerFixnumRange(BasicObjectNodes.getDynamicObject(object));
    }

    public static boolean isLongFixnumRange(Object object) {
        return (object instanceof DynamicObject) && isLongFixnumRange((DynamicObject) object);
    }

    public static boolean isLongFixnumRange(DynamicObject object) {
        return RangeNodes.LONG_FIXNUM_RANGE_LAYOUT.isLongFixnumRange(BasicObjectNodes.getDynamicObject(object));
    }

    public static boolean isObjectRange(Object object) {
        return (object instanceof DynamicObject) && isObjectRange((DynamicObject) object);
    }

    public static boolean isObjectRange(DynamicObject object) {
        return RangeNodes.OBJECT_RANGE_LAYOUT.isObjectRange(BasicObjectNodes.getDynamicObject(object));
    }

    public static boolean isRubyRange(Object value) {
        return isIntegerFixnumRange(value) || isLongFixnumRange(value) || isObjectRange(value);
    }

    public static boolean isRubyArray(Object value) {
        return (value instanceof DynamicObject) && isRubyArray((DynamicObject) value);
    }

    public static boolean isRubyArray(DynamicObject value) {
        return ArrayNodes.ARRAY_LAYOUT.isArray(BasicObjectNodes.getDynamicObject(value));
    }

    public static boolean isRubyBinding(DynamicObject object) {
        return BindingNodes.BINDING_LAYOUT.isBinding(BasicObjectNodes.getDynamicObject(object));
    }

    public static boolean isRubyClass(Object value) {
        return (value instanceof DynamicObject) && isRubyClass((DynamicObject) value);
    }

    public static boolean isRubyClass(DynamicObject value) {
        return ClassNodes.CLASS_LAYOUT.isClass(BasicObjectNodes.getDynamicObject(value));
    }

    public static boolean isRubyHash(Object value) {
        return (value instanceof DynamicObject) && isRubyHash((DynamicObject) value);
    }

    public static boolean isRubyHash(DynamicObject value) {
        return HashNodes.HASH_LAYOUT.isHash(BasicObjectNodes.getDynamicObject(value));
    }

    public static boolean isRubyModule(Object value) {
        return (value instanceof DynamicObject) && isRubyModule((DynamicObject) value);
    }

    public static boolean isRubyModule(DynamicObject value) {
        return ModuleNodes.MODULE_LAYOUT.isModule(BasicObjectNodes.getDynamicObject(value));
    }

    public static boolean isRubyRegexp(Object value) {
        return (value instanceof DynamicObject) && isRubyRegexp((DynamicObject) value);
    }

    public static boolean isRubyRegexp(DynamicObject value) {
        return RegexpNodes.REGEXP_LAYOUT.isRegexp(BasicObjectNodes.getDynamicObject(value));
    }

    public static boolean isRubyString(Object value) {
        return (value instanceof DynamicObject) && isRubyString((DynamicObject) value);
    }

    public static boolean isRubyString(DynamicObject value) {
        return StringNodes.STRING_LAYOUT.isString(BasicObjectNodes.getDynamicObject(value));
    }

    public static boolean isRubyEncoding(Object object) {
        return (object instanceof DynamicObject) && isRubyEncoding((DynamicObject) object);
    }

    public static boolean isRubyEncoding(DynamicObject object) {
        return EncodingNodes.ENCODING_LAYOUT.isEncoding(BasicObjectNodes.getDynamicObject(object));
    }

    public static boolean isRubySymbol(Object value) {
        return (value instanceof DynamicObject) && isRubySymbol((DynamicObject) value);
    }

    public static boolean isRubySymbol(DynamicObject value) {
        return SymbolNodes.SYMBOL_LAYOUT.isSymbol(BasicObjectNodes.getDynamicObject(value));
    }

    public static boolean isRubyMethod(Object value) {
        return (value instanceof DynamicObject) && isRubyMethod((DynamicObject) value);
    }

    public static boolean isRubyMethod(DynamicObject value) {
        return MethodNodes.METHOD_LAYOUT.isMethod(BasicObjectNodes.getDynamicObject(value));
    }

    public static boolean isRubyUnboundMethod(Object value) {
        return (value instanceof DynamicObject) && isRubyUnboundMethod((DynamicObject) value);
    }

    public static boolean isRubyUnboundMethod(DynamicObject value) {
        return UnboundMethodNodes.UNBOUND_METHOD_LAYOUT.isUnboundMethod(BasicObjectNodes.getDynamicObject(value));
    }

    public static boolean isRubyMutex(DynamicObject value) {
        return MutexNodes.MUTEX_LAYOUT.isMutex(BasicObjectNodes.getDynamicObject(value));
    }

    public static boolean isDynamicObject(Object value) {
        return value instanceof DynamicObject;
    }

    public static boolean isRubyPointer(Object value) {
        return (value instanceof DynamicObject) && isRubyPointer((DynamicObject) value);
    }

    public static boolean isRubyPointer(DynamicObject value) {
        return PointerNodes.POINTER_LAYOUT.isPointer(BasicObjectNodes.getDynamicObject(value));
    }

    public static boolean isRubiniusByteArray(Object value) {
        return (value instanceof DynamicObject) && isRubiniusByteArray((DynamicObject) value);
    }

    public static boolean isRubiniusByteArray(DynamicObject value) {
        return ByteArrayNodes.BYTE_ARRAY_LAYOUT.isByteArray(BasicObjectNodes.getDynamicObject(value));
    }

    public static boolean isRubyProc(Object object) {
        return (object instanceof DynamicObject) && isRubyProc((DynamicObject) object);
    }

    public static boolean isRubyProc(DynamicObject object) {
        return ProcNodes.PROC_LAYOUT.isProc(BasicObjectNodes.getDynamicObject(object));
    }

    public static boolean isRubyEncodingConverter(DynamicObject encodingConverter) {
        return EncodingConverterNodes.ENCODING_CONVERTER_LAYOUT.isEncodingConverter(BasicObjectNodes.getDynamicObject(encodingConverter));
    }

    public static boolean isRubyTime(DynamicObject object) {
        return TimeNodes.TIME_LAYOUT.isTime(BasicObjectNodes.getDynamicObject(object));
    }

    public static boolean isRubyException(Object value) {
        return (value instanceof DynamicObject) && isRubyException((DynamicObject) value);
    }

    public static boolean isRubyException(DynamicObject object) {
        return ExceptionNodes.EXCEPTION_LAYOUT.isException(BasicObjectNodes.getDynamicObject(object));
    }

    public static boolean isRubyFiber(Object object) {
        return (object instanceof DynamicObject) && isRubyFiber((DynamicObject) object);
    }

    public static boolean isRubyFiber(DynamicObject object) {
        return FiberNodes.FIBER_LAYOUT.isFiber(BasicObjectNodes.getDynamicObject(object));
    }

    public static boolean isRubyThread(Object object) {
        return (object instanceof DynamicObject) && isRubyThread((DynamicObject) object);
    }

    public static boolean isRubyThread(DynamicObject object) {
        return ThreadNodes.THREAD_LAYOUT.isThread(BasicObjectNodes.getDynamicObject(object));
    }

    public static boolean isRubyMatchData(Object object) {
        return (object instanceof DynamicObject) && isRubyMatchData((DynamicObject) object);
    }

    public static boolean isRubyMatchData(DynamicObject object) {
        return MatchDataNodes.MATCH_DATA_LAYOUT.isMatchData(BasicObjectNodes.getDynamicObject(object));
    }

    // Internal types

    public static boolean isThreadLocal(Object value) {
        return value instanceof ThreadLocalObject;
    }

    public static boolean isForeignObject(Object object) {
        return (object instanceof TruffleObject) && !(object instanceof DynamicObject);
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
