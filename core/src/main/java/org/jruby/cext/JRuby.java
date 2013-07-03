/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2010 Wayne Meissner
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


package org.jruby.cext;

import java.math.BigInteger;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBignum;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyMethod;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyProc;
import org.jruby.RubyString;
import org.jruby.RubyThread;
import org.jruby.RubyThread.BlockingTask;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import jnr.ffi.LastError;

/**
 *
 */
public class JRuby {

    public static long callRubyMethod(IRubyObject recv, Object methodName, IRubyObject[] args) {
        IRubyObject retval = recv.callMethod(recv.getRuntime().getCurrentContext(),
                methodName.toString(), args);

        return Handle.nativeHandle(retval);
    }

    public static long callRubyMethodB(IRubyObject recv, Object methodName, IRubyObject[] args, IRubyObject blockProc) {
        IRubyObject retval = recv.callMethod(recv.getRuntime().getCurrentContext(),
                methodName.toString(), args, ((RubyProc)blockProc).getBlock());

        return Handle.nativeHandle(retval);
    }

    public static long callRubyMethod0(IRubyObject recv, Object methodName) {
        IRubyObject retval = Helpers.invoke(recv.getRuntime().getCurrentContext(),
                recv, methodName.toString());

        return Handle.nativeHandle(retval);
    }

    public static long callRubyMethod1(IRubyObject recv, Object methodName, IRubyObject arg1) {
        IRubyObject retval = Helpers.invoke(recv.getRuntime().getCurrentContext(),
                recv, methodName.toString(), arg1);

        return Handle.nativeHandle(retval);
    }

    public static long callRubyMethod2(IRubyObject recv, Object methodName, IRubyObject arg1, IRubyObject arg2) {
        IRubyObject retval = Helpers.invoke(recv.getRuntime().getCurrentContext(),
                recv, methodName.toString(), arg1, arg2);

        return Handle.nativeHandle(retval);
    }

    public static long callRubyMethod3(IRubyObject recv, Object methodName, IRubyObject arg1,
            IRubyObject arg2, IRubyObject arg3) {
        IRubyObject retval = Helpers.invoke(recv.getRuntime().getCurrentContext(),
                recv, methodName.toString(), arg1, arg2, arg3);

        return Handle.nativeHandle(retval);
    }

    public static long callSuperMethod(Ruby runtime, IRubyObject[] args) {
        ThreadContext currentContext = runtime.getCurrentContext();
        IRubyObject retval = Helpers.invokeSuper(currentContext,
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


    public static long newString(Ruby runtime, byte[] bytes, int length, boolean tainted) {
        IRubyObject retval = RubyString.newStringNoCopy(runtime, bytes);
        if (tainted) {
            retval.setTaint(tainted);
        }
        ((RubyString) retval).getByteList().setRealSize(length);

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

    public static IRubyObject newThread(Ruby runtime, long fn, IRubyObject args_ary) {
        RubyProc proc = (RubyProc) newProc(runtime, fn);
        IRubyObject[] args = (args_ary instanceof RubyArray) ? ((RubyArray)args_ary).toJavaArray() : new IRubyObject[] {args_ary};
        return RubyThread.newInstance(runtime.getThread(), args, proc.getBlock());
    }

    public static IRubyObject newProc(Ruby runtime, long fn) {
        String name = System.currentTimeMillis() + "$block_jruby-cext";
        IRubyObject recv = runtime.getCurrentContext().getFrameSelf();
        RubyMethod method = RubyMethod.newMethod(recv.getMetaClass(), name, recv.getMetaClass(), name,
                new NativeProcMethod(recv.getMetaClass(), fn), recv);
        IRubyObject proc = method.to_proc(runtime.getCurrentContext(), Block.NULL_BLOCK);
        return proc;
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
        final int n = LastError.getLastError(jnr.ffi.Runtime.getSystemRuntime());
        sysFail(runtime, message, n);
    }

    public static void sysFail(Ruby runtime, String message, int n) {
        IRubyObject arg = (message != null) ? runtime.newString(message) : runtime.getNil();

        RubyClass instance = runtime.getErrno(n);
        if(instance == null) {
            instance = runtime.getSystemCallError();
            throw new RaiseException((RubyException)(instance.newInstance(runtime.getCurrentContext(), new IRubyObject[]{arg, runtime.newFixnum(n)}, Block.NULL_BLOCK)));
        } else {
            throw new RaiseException((RubyException)(instance.newInstance(runtime.getCurrentContext(), new IRubyObject[]{arg}, Block.NULL_BLOCK)));
        }
    }

    public static void threadSleep(Ruby runtime, int interval) {
        try {
            runtime.getCurrentContext().getThread().sleep(interval);
        } catch (InterruptedException e) {
            // Thread wakeup, do nothing
        }
    }
    
    public static long getMetaClass(IRubyObject object) {
        RubyClass metaClass = object.getMetaClass();
        return Handle.nativeHandle(metaClass);
    }

    public static final class NativeFunctionTask implements BlockingTask {

        private Native nativeInstance;
        private long run, run_data, wakeup, wakeup_data = 0;
        public long retval = 4; // 4 is VALUE Qnil

        public NativeFunctionTask(Native nativeInstance, long run, long run_data,
                long wakeup, long wakeup_data) {
            this.nativeInstance = nativeInstance;
            this.run = run;
            this.run_data = run_data;
            this.wakeup = wakeup;
            this.wakeup_data = wakeup_data;
        }

        public void run() throws InterruptedException {
            retval = nativeInstance.callFunction(run, run_data);
        }

        public void wakeup() {
            nativeInstance.callFunction(wakeup, wakeup_data);
        }
    }

    public static long nativeBlockingRegion(Ruby runtime, long blocking_func, long blocking_data,
            long unblocking_func, long unblocking_data) {
        RubyThread thread = runtime.getCurrentContext().getThread();
        NativeFunctionTask task = new NativeFunctionTask(Native.getInstance(runtime), blocking_func,
                blocking_data, unblocking_func, unblocking_data);

        GC.disable();
        int lockCount = GIL.releaseAllLocks();
        try {
            thread.executeBlockingTask(task);
        } catch (InterruptedException e) {
            // ignore
        } finally  {
            GIL.acquire(lockCount);
            GC.enable();
        }

        return task.retval;
    }
}
