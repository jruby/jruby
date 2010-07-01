/*
 * Copyright (C) 2008, 2009 Wayne Meissner
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

import com.kenai.jffi.Library;
import org.jruby.Ruby;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;


final class Native {
    private static Native INSTANCE;
    private static Library shim = null; // keep a hard ref to avoid GC

    private final Ruby runtime;

    static Native getInstance(Ruby runtime) {
        if (INSTANCE == null) {
            INSTANCE = new Native(runtime);
            INSTANCE.load(runtime);
            GC.init(INSTANCE);
        
        } else if (INSTANCE.runtime != runtime) {
            throw runtime.newRuntimeError("invalid runtime");
        }

        return INSTANCE;
    }

    private Native(Ruby runtime) {
        this.runtime = runtime;
    }

    private void load(Ruby runtime) {

        // Force the shim library to load into the global namespace
        if ((shim = Library.openLibrary(System.mapLibraryName("jruby-cext"), Library.NOW | Library.GLOBAL)) == null) {
            throw new UnsatisfiedLinkError("failed to load shim library, error: " + Library.getLastError());
        }
        
        System.loadLibrary("jruby-cext");
        // Register Qfalse, Qtrue, Qnil constants to avoid reverse lookups in native code
        GC.register(runtime.getFalse(), Handle.newHandle(runtime, runtime.getFalse(), getFalse()));
        GC.register(runtime.getTrue(), Handle.newHandle(runtime, runtime.getTrue(), getTrue()));
        GC.register(runtime.getNil(), Handle.newHandle(runtime, runtime.getNil(), getNil()));

        initNative(runtime);        
    }


    private final native void initNative(Ruby runtime);
    
    public final native long callInit(ThreadContext ctx, long init);


    public final IRubyObject callMethod(ThreadContext ctx, long fn, IRubyObject recv, int arity, IRubyObject[] args) {
        long[] largs = new long[args.length];
        for (int i = 0; i < largs.length; ++i) {
            largs[i] = Handle.nativeHandleLocked(args[i]);
        }
        return callMethod(ctx, fn, Handle.nativeHandleLocked(recv), arity, largs);
    }

    public final native IRubyObject callMethod(ThreadContext ctx, long fn, long recv, int arity, long[] args);
    final native IRubyObject callMethod0(long fn, long recv);
    final native IRubyObject callMethod1(long fn, long recv, long arg0);
    final native IRubyObject callMethod2(long fn, long recv, long arg0, long arg1);
    final native IRubyObject callMethod3(long fn, long recv, long arg0, long arg1, long arg2);

    public final native long newHandle(IRubyObject obj);
    public final native long newFixnumHandle(IRubyObject obj, long value);
    
    public final native void gc();
    public final native Object pollGC();

    private final native int getNil();
    private final native int getTrue();
    private final native int getFalse();

    
}
