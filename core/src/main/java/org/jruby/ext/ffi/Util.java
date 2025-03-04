/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2008 JRuby project
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.ext.ffi;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.jruby.*;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static com.headius.backport9.buffer.Buffers.positionBuffer;
import static org.jruby.api.Convert.toDouble;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.typeError;

/**
 *
 */
public final class Util {
    private Util() {}
    public static final byte int8Value(IRubyObject parameter) {
        return (byte) longValue(parameter);
    }

    public static final short uint8Value(IRubyObject parameter) {
        return (short) longValue(parameter);
    }

    public static final short int16Value(IRubyObject parameter) {
        return (short) longValue(parameter);
    }
    
    public static final int uint16Value(IRubyObject parameter) {
        return (int) longValue(parameter);
    }

    public static final int int32Value(IRubyObject parameter) {
        return (int) longValue(parameter);
    }

    public static final long uint32Value(IRubyObject parameter) {
        return longValue(parameter);
    }

    public static final long int64Value(IRubyObject parameter) {
        return longValue(parameter);
    }

    public static final long uint64Value(IRubyObject parameter) {
        final long value = parameter instanceof RubyBignum
                ? ((RubyBignum) parameter).getValue().longValue()
                :ulongValue(parameter);
        return value;
    }

    @Deprecated(since = "10.0")
    public static final float floatValue(IRubyObject parameter) {
        return floatValue(((RubyBasicObject) parameter).getCurrentContext(), parameter);
    }

    public static final float floatValue(ThreadContext context, IRubyObject parameter) {
        return (float) toDouble(context, parameter);
    }

    @Deprecated(since = "10.0")
    public static final double doubleValue(IRubyObject parameter) {
        return doubleValue(((RubyBasicObject) parameter).getCurrentContext(), parameter);
    }

    public static final double doubleValue(ThreadContext context, IRubyObject parameter) {
        return toDouble(context, parameter);
    }

    /**
     * Converts characters like 'a' or 't' to an integer value
     *
     * @param parameter
     * @return
     */
    public static final long longValue(IRubyObject parameter) {
        return RubyNumeric.num2long(parameter);
    }

    @Deprecated(since = "10.0")
    public static int intValue(IRubyObject obj, RubyHash enums) {
        var context = ((RubyBasicObject) obj).getCurrentContext();
        if (obj instanceof RubyInteger obji) return obji.asInt(context);
        if (obj instanceof RubySymbol) {
            IRubyObject value = enums.fastARef(obj);
            if (value.isNil()) throw argumentError(context, "invalid enum value, " + obj.inspect(context));

            return (int) longValue(value);
        } else {
            return (int) longValue(obj);
        }
    }

    public static final long ulongValue(IRubyObject parameter) {
        return RubyNumeric.num2ulong(parameter);
    }

    public static final IRubyObject newSigned8(Ruby runtime, byte value) {
        return RubyFixnum.newFixnum(runtime, value);
    }

    public static final IRubyObject newUnsigned8(Ruby runtime, byte value) {
        return RubyFixnum.newFixnum(runtime, value < 0 ? (long)((value & 0x7FL) + 0x80L) : value);
    }

    public static final IRubyObject newSigned16(Ruby runtime, short value) {
        return RubyFixnum.newFixnum(runtime, value);
    }

    public static final IRubyObject newUnsigned16(Ruby runtime, short value) {
        return RubyFixnum.newFixnum(runtime, value < 0 ? (long)((value & 0x7FFFL) + 0x8000L) : value);
    }

    public static final IRubyObject newSigned32(Ruby runtime, int value) {
        return RubyFixnum.newFixnum(runtime, value);
    }

    public static final IRubyObject newUnsigned32(Ruby runtime, int value) {
        return RubyFixnum.newFixnum(runtime, value < 0 ? (long)((value & 0x7FFFFFFFL) + 0x80000000L) : value);
    }

    public static final IRubyObject newSigned64(Ruby runtime, long value) {
        return RubyFixnum.newFixnum(runtime, value);
    }

    private static final BigInteger UINT64_BASE = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
    public static final IRubyObject newUnsigned64(Ruby runtime, long value) {
        return value < 0
                    ? RubyBignum.newBignum(runtime, BigInteger.valueOf(value & 0x7fffffffffffffffL).add(UINT64_BASE))
                    : RubyFixnum.newFixnum(runtime, value);
    }

    @Deprecated
    public static final <T> T convertParameter(IRubyObject parameter, Class<T> paramClass) {
        return paramClass.cast(parameter.toJava(paramClass));
    }

    public static final ByteBuffer slice(ByteBuffer buf, int offset) {
        ByteBuffer tmp = buf.duplicate();
        positionBuffer(tmp, offset);
        return tmp.slice();
    }

    public static final void checkBounds(Ruby runtime, long size, long off, long len) {
        if ((off | len | (off + len) | (size - (off + len))) < 0) {
            throw runtime.newIndexError("Memory access offset="
                    + off + " size=" + len + " is out of bounds");
        }
    }

    public static Type findType(ThreadContext context, IRubyObject name) {
        return context.runtime.getFFI().getTypeResolver().findType(context.runtime, name);
    }

    public static Type findType(ThreadContext context, IRubyObject name, IRubyObject typeMap) {
        return context.runtime.getFFI().getTypeResolver().findType(context.runtime, name, typeMap);
    }

    public static ByteOrder parseByteOrder(Ruby runtime, IRubyObject byte_order) {
        if (!(byte_order instanceof RubySymbol) && !(byte_order instanceof RubyString)) {
            throw typeError(runtime.getCurrentContext(), byte_order , "Symbol or String");
        }

        switch (byte_order.asJavaString()) {
            case "network": case "big": return ByteOrder.BIG_ENDIAN;
            case "little": return ByteOrder.LITTLE_ENDIAN;
            default: throw argumentError(runtime.getCurrentContext(), "unknown byte order");
        }
    }

    public static int roundUpToPowerOfTwo(int v) {
        if (v <= 1) return 1;
        return Integer.MIN_VALUE >>> Integer.numberOfLeadingZeros(v - 1) << 1;
    }
}
