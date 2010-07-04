/*
 * Copyright (C) 2010 Wayne Meissner
 *
 * This file is part of jruby-cext.
 *
 * This code is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details.
 *
 * You should have received a copy of the GNU General Public License
 * version 3 along with this work.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jruby.cext;

import java.math.BigInteger;
import org.jruby.Ruby;
import org.jruby.RubyBignum;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
public class JRuby {

    public static long callRubyMethod(IRubyObject recv, Object methodName, IRubyObject[] args) {
        IRubyObject retval = recv.callMethod(recv.getRuntime().getCurrentContext(),
                methodName.toString(), args);

        return Handle.nativeHandleLocked(retval);
    }

    public static long newString(Ruby runtime, byte[] bytes, boolean tainted) {
        IRubyObject retval = RubyString.newStringNoCopy(runtime, bytes);
        if (tainted) {
            retval.setTaint(tainted);
        }

        return Handle.nativeHandleLocked(retval);
    }

    public static DynamicMethod newMethod(RubyModule module, long fn, int arity) {
        switch (arity) {
            case 0:
                return new NativeMethod0(module, arity, fn);

            case 1:
                return new NativeMethod1(module, arity, fn);

            case 2:
                return new NativeMethod2(module, arity, fn);

            case 3:
                return new NativeMethod3(module, arity, fn);

            default:
                return new NativeMethod(module, arity, fn);
        }
    }

    public static RubyFloat newFloat(Ruby runtime, long handle, double value) {
        final RubyFloat f = RubyFloat.newFloat(runtime, value);
        f.fastSetInternalVariable(GC.NATIVE_REF_KEY, Handle.newHandle(runtime, f, handle));

        return f;
    }

    public static long getRString(RubyString str) {
        Object ivar = str.fastGetInternalVariable("rstring-cext");
        if (ivar instanceof RString) {
            return ((RString) ivar).address();
        }
        
        long address = Native.newRString();
        RString rstring = RString.newRString(str, address);
        str.fastSetInternalVariable("rstring-cext", rstring);

        return address;
    }

    public static long ll2inum(Ruby runtime, long l) {
        return Handle.nativeHandleLocked(RubyFixnum.newFixnum(runtime, l));
    }

    private static final BigInteger UINT64_BASE = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);

    public static long ull2inum(Ruby runtime, long l) {
        IRubyObject retval = l < 0
                    ? RubyBignum.newBignum(runtime, BigInteger.valueOf(l & 0x7fffffffffffffffL).add(UINT64_BASE))
                    : runtime.newFixnum(l);

        return Handle.nativeHandleLocked(retval);
    }

    public static long int2big(Ruby runtime, long l) {
        return Handle.nativeHandleLocked(RubyBignum.newBignum(runtime, l));
    }

    public static long uint2big(Ruby runtime, long l) {
        IRubyObject retval = l < 0
                    ? RubyBignum.newBignum(runtime, BigInteger.valueOf(l & 0x7fffffffffffffffL).add(UINT64_BASE))
                    : RubyBignum.newBignum(runtime, l);

        return Handle.nativeHandleLocked(retval);
    }
}
