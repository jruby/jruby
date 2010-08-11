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
import org.jruby.RubyArray;
import org.jruby.RubyBignum;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyProc;
import org.jruby.RubyString;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.GlobalVariable;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import com.kenai.jaffl.LastError;

/**
 *
 */
public class JRuby {

    public static long callRubyMethod(IRubyObject recv, Object methodName, IRubyObject[] args) {
        IRubyObject retval = recv.callMethod(recv.getRuntime().getCurrentContext(),
                methodName.toString(), args);

        return Handle.nativeHandle(retval);
    }

    public static long callRubyMethod0(IRubyObject recv, Object methodName) {
        IRubyObject retval = RuntimeHelpers.invoke(recv.getRuntime().getCurrentContext(),
                recv, methodName.toString());

        return Handle.nativeHandle(retval);
    }

    public static long callRubyMethod1(IRubyObject recv, Object methodName, IRubyObject arg1) {
        IRubyObject retval = RuntimeHelpers.invoke(recv.getRuntime().getCurrentContext(),
                recv, methodName.toString(), arg1);

        return Handle.nativeHandle(retval);
    }

    public static long callRubyMethod2(IRubyObject recv, Object methodName, IRubyObject arg1, IRubyObject arg2) {
        IRubyObject retval = RuntimeHelpers.invoke(recv.getRuntime().getCurrentContext(),
                recv, methodName.toString(), arg1, arg2);

        return Handle.nativeHandle(retval);
    }

    public static long callRubyMethod3(IRubyObject recv, Object methodName, IRubyObject arg1,
            IRubyObject arg2, IRubyObject arg3) {
        IRubyObject retval = RuntimeHelpers.invoke(recv.getRuntime().getCurrentContext(),
                recv, methodName.toString(), arg1, arg2, arg3);

        return Handle.nativeHandle(retval);
    }

    public static long callSuperMethod(Ruby runtime, IRubyObject[] args) {
        ThreadContext currentContext = runtime.getCurrentContext();
        IRubyObject retval = RuntimeHelpers.invokeSuper(currentContext, 
                runtime.getCurrentContext().getFrameSelf(), args, Block.NULL_BLOCK);

        return Handle.nativeHandle(retval);
    }

    public static long instanceEval(IRubyObject self, IRubyObject[] args) {
        Ruby runtime = self.getRuntime();
        ThreadContext ctxt = runtime.getCurrentContext();
        Block block = ctxt.getFrameBlock();
        IRubyObject retval = self.callMethod(ctxt, "instance_eval", args, block);
        return Handle.nativeHandle(retval);
    }


    public static long newString(Ruby runtime, byte[] bytes, boolean tainted) {
        IRubyObject retval = RubyString.newStringNoCopy(runtime, bytes);
        if (tainted) {
            retval.setTaint(tainted);
        }

        return Handle.nativeHandle(retval);
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
        GC.register(f, Handle.newHandle(runtime, f, handle));

        return f;
    }

    public static long getRString(RubyString str) {
        return RString.valueOf(str).address();
    }
    
    /** rb_yield */
    public static IRubyObject yield(Ruby runtime, RubyArray args) {
        return runtime.getCurrentContext().getFrameBlock().call(runtime.getCurrentContext(), args.toJavaArray());
    }

    /** rb_block_given_p */
    public static int blockGiven(Ruby runtime) {
        return runtime.getCurrentContext().getFrameBlock().isGiven() ? 1 : 0;
    }

    /** rb_block_proc */
    public static RubyProc getBlockProc(Ruby runtime) {
        Block block = runtime.getCurrentContext().getFrameBlock();
        RubyProc p = RubyProc.newProc(runtime, block, block.type);
        return p;
    }

    public static long ll2inum(Ruby runtime, long l) {
        RubyFixnum n = RubyFixnum.newFixnum(runtime, l);
        Handle h = GC.lookup(n);
        if (h != null) {
            return h.getAddress();
        }
        h = Handle.newHandle(runtime, n, Native.getInstance(runtime).newFixnumHandle(n, l));
        GC.register(n, h);

        return h.getAddress();
    }

    private static final BigInteger UINT64_BASE = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);

    public static long ull2inum(Ruby runtime, long l) {
        RubyObject n = l < 0
                    ? RubyBignum.newBignum(runtime, BigInteger.valueOf(l & 0x7fffffffffffffffL).add(UINT64_BASE))
                    : runtime.newFixnum(l);

        Handle h = GC.lookup(n);
        if (h != null) {
            return h.getAddress();
        }
        // FIXME should create Bignum handle for Bignum values
        h = Handle.newHandle(runtime, n, Native.getInstance(runtime).newFixnumHandle(n, l));
        GC.register(n, h);

        return h.getAddress();
    }

    public static long int2big(Ruby runtime, long l) {
        return Handle.nativeHandle(RubyBignum.newBignum(runtime, l));
    }

    public static long uint2big(Ruby runtime, long l) {
        IRubyObject retval = l < 0
                    ? RubyBignum.newBignum(runtime, BigInteger.valueOf(l & 0x7fffffffffffffffL).add(UINT64_BASE))
                    : RubyBignum.newBignum(runtime, l);

        return Handle.nativeHandle(retval);
    }

    /** rb_gv_set */
    public static long gv_set(Ruby runtime, String name, IRubyObject value) {
        return Handle.nativeHandle(runtime.getGlobalVariables().set(name, value));
    }

    /** rb_gv_get */
    public static long gv_get(Ruby runtime, String name) {
        return Handle.nativeHandle(runtime.getGlobalVariables().get(name));
    }

    public static void clearErrorInfo(Ruby runtime) {
        runtime.getCurrentContext().setErrorInfo(runtime.getNil());
    }

    /** rb_sys_fail */
    public static void sysFail(Ruby runtime, String message) {
        final int n = LastError.getLastError();

        IRubyObject arg = (message != null) ? runtime.newString(message) : runtime.getNil();

        RubyClass instance = runtime.getErrno(n);
        if(instance == null) {
            instance = runtime.getSystemCallError();
            throw new RaiseException((RubyException)(instance.newInstance(runtime.getCurrentContext(), new IRubyObject[]{arg, runtime.newFixnum(n)}, Block.NULL_BLOCK)));
        } else {
            throw new RaiseException((RubyException)(instance.newInstance(runtime.getCurrentContext(), new IRubyObject[]{arg}, Block.NULL_BLOCK)));
        }
    }
}
