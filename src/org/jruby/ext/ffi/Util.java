/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.ext.ffi;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import org.jruby.Ruby;
import org.jruby.RubyBignum;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.javasupport.JavaObject;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 *
 */
public final class Util {
    private Util() {}
    public static final byte int8Value(IRubyObject parameter) {
        final long value = longValue(parameter);
        if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE) {
            throw parameter.getRuntime().newRangeError("Value "
                    + value + " outside char range");
        }
        return (byte) value;
    }
    public static final short uint8Value(IRubyObject parameter) {
        final long value = longValue(parameter);
        if (value < 0 || value > 0xffL) {
            throw parameter.getRuntime().newRangeError("Value "
                    + value + " outside unsigned char range");
        }
        return (short) value;
    }
    public static final short int16Value(IRubyObject parameter) {
        final long value = longValue(parameter);
        if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
            throw parameter.getRuntime().newRangeError("Value "
                    + value + " outside short range");
        }
        return (short) value;
    }
    public static final int uint16Value(IRubyObject parameter) {
        final long value = longValue(parameter);
        if (value < 0 || value > 0xffffL) {
            throw parameter.getRuntime().newRangeError("Value "
                    + value + " outside unsigned short range");
        }
        return (int) value;
    }
    public static final int int32Value(IRubyObject parameter) {
        final long value = longValue(parameter);
        if (value < Integer.MIN_VALUE || value > 0xffffffffL) {
            throw parameter.getRuntime().newRangeError("Value "
                    + value + " outside integer range");
        }
        // This also handles unsigned int -> negative signed int conversion
        return (int) value;
    }
    public static final long uint32Value(IRubyObject parameter) {
        final long value = longValue(parameter);
        if (value < 0 || value > 0xffffffffL) {
            throw parameter.getRuntime().newRangeError("Value "
                    + value + " outside unsigned integer range");
        }
        return value;
    }
    public static final long int64Value(IRubyObject parameter) {
        return longValue(parameter);
    }
    public static final long uint64Value(IRubyObject parameter) {
        final long value = parameter instanceof RubyBignum
                ? ((RubyBignum) parameter).getValue().longValue()
                :longValue(parameter);
        return value;
    }
    public static final float floatValue(IRubyObject parameter) {
        return (float) RubyNumeric.num2dbl(parameter);
    }
    public static final double doubleValue(IRubyObject parameter) {
        return RubyNumeric.num2dbl(parameter);
    }

    /**
     * Converts characters like 'a' or 't' to an integer value
     *
     * @param parameter
     * @return
     */
    public static final long longValue(IRubyObject parameter) {
        if (parameter instanceof RubyNumeric) {
            return ((RubyNumeric) parameter).getLongValue();
        } else if (parameter.isNil()) {
            return 0L;
        } else if (parameter instanceof RubyString) {
            return longValue((RubyString) parameter);
        }
        throw parameter.getRuntime().newRangeError("Value "
                    + parameter + " is not an integer");
    }
    private static final long longValue(RubyString parameter) {
        CharSequence cs = parameter.asJavaString();
        if (cs.length() == 1) {
            return cs.charAt(0);
        }
        throw parameter.getRuntime().newRangeError("Value "
                    + parameter + " is not an integer");
    }
    public static final IRubyObject newSigned8(Ruby runtime, int value) {
        value &= 0xff;
        return runtime.newFixnum(value < 0x80 ? value : -0x80 + (value - 0x80));
    }
    public static final IRubyObject newUnsigned8(Ruby runtime, int value) {
        return runtime.newFixnum(value < 0 ? (long)((value & 0x7FL) + 0x80L) : value);
    }
    public static final IRubyObject newSigned16(Ruby runtime, int value) {
        value &= 0xffff;
        return runtime.newFixnum(value < 0x8000 ? value : -0x8000 + (value - 0x8000));
    }
    public static final IRubyObject newUnsigned16(Ruby runtime, int value) {
        return runtime.newFixnum(value < 0 ? (long)((value & 0x7FFFL) + 0x8000L) : value);
    }
    public static final IRubyObject newSigned32(Ruby runtime, int value) {
        return runtime.newFixnum(value);
    }
    public static final IRubyObject newUnsigned32(Ruby runtime, int value) {
        return runtime.newFixnum(value < 0 ? (long)((value & 0x7FFFFFFFL) + 0x80000000L) : value);
    }
    public static final IRubyObject newSigned64(Ruby runtime, long value) {
        return runtime.newFixnum(value);
    }
    private static final BigInteger UINT64_BASE = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
    public static final IRubyObject newUnsigned64(Ruby runtime, long value) {
        return value < 0
                    ? RubyBignum.newBignum(runtime, BigInteger.valueOf(value & 0x7fffffffffffffffL).add(UINT64_BASE))
                    : runtime.newFixnum(value);
    }
    @Deprecated
    public static final <T> T convertParameter(IRubyObject parameter, Class<T> paramClass) {
        return paramClass.cast(parameter instanceof JavaObject
            ? ((JavaObject) parameter).getValue()
            : parameter.toJava(paramClass));
    }
    public static final ByteBuffer slice(ByteBuffer buf, int offset) {
        ByteBuffer tmp = buf.duplicate();
        tmp.position((int) offset);
        return tmp.slice();
    }
    public static final void checkStringSafety(Ruby runtime, IRubyObject value) {
        RubyString s = value.asString();
        if (runtime.getSafeLevel() > 0 && s.isTaint()) {
            throw runtime.newSecurityError("Unsafe string parameter");
        }
        ByteList bl = s.getByteList();
        final byte[] array = bl.unsafeBytes();
        final int end = bl.length();
        for (int i = bl.begin(); i < end; ++i) {
            if (array[i] == (byte) 0) {
                throw runtime.newSecurityError("string contains null byte");
            }
        }
    }

    public static final void checkBounds(Ruby runtime, long size, long off, long len) {
        if ((off | len | (off + len) | (size - (off + len))) < 0) {
            throw runtime.newIndexError("Memory access offset="
                    + off + " size=" + len + " is out of bounds");
        }
    }
}
