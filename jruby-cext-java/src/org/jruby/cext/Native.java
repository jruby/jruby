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

    public synchronized static final Native getInstance(Ruby runtime) {
        if (INSTANCE == null) {
            INSTANCE = new Native();
            INSTANCE.load(runtime);
        }

        return INSTANCE;
    }


    private void load(Ruby runtime) {

        // Force the shim library to load into the global namespace
        if ((shim = Library.openLibrary(System.mapLibraryName("jruby-cext"), Library.NOW | Library.GLOBAL)) == null) {
            throw new UnsatisfiedLinkError("failed to load shim library, error: " + Library.getLastError());
        }
        
        System.loadLibrary("jruby-cext");
        initNative(runtime);
    }


    private final native void initNative(Ruby runtime);
    
    public final native long callInit(ThreadContext ctx, long init);
    public final native IRubyObject callMethod(ThreadContext ctx, long fn, IRubyObject recv, int arity, IRubyObject[] args);

    public final native long newHandle(IRubyObject obj);
    public final native void freeHandle(long handle);
    public final native void markHandle(long handle);
    public final native void unmarkHandle(long handle);
}
