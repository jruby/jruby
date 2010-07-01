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

import org.jruby.Ruby;
import org.jruby.RubyFixnum;
import org.jruby.runtime.builtin.IRubyObject;

public final class Handle {
    private final Ruby runtime;
    private final long address;
    
    static Handle newHandle(Ruby runtime, Object rubyObject, long nativeHandle) {
        return new Handle(runtime, nativeHandle);
    }
    
    private Handle(Ruby runtime, long address) {
        this.runtime = runtime;
        this.address = address;
    }
    
    public final long getAddress() {
        return address;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Handle other = (Handle) obj;
        return this.address == other.address;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 37 * hash + (int) (this.address ^ (this.address >>> 32));
        return hash;
    }

    @Override
    public String toString() {
        return "Native ruby object " + Long.toString(address);
    }

    static Handle valueOfLocked(IRubyObject obj) {
        Handle h = GC.lookup(obj);
        if (h != null) {
            return h;
        }

        Ruby runtime = obj.getRuntime();
        long nativeHandle;

        if (obj instanceof RubyFixnum) {
            nativeHandle = Native.getInstance(runtime).newFixnumHandle(obj, ((RubyFixnum) obj).getLongValue());
        } else {
            nativeHandle = Native.getInstance(runtime).newHandle(obj);
        }

        Handle handle = newHandle(runtime, obj, nativeHandle);

        GC.register(obj, handle);

        return handle;
    }

    public static synchronized Handle valueOf(IRubyObject obj) {
        GIL.acquire();
        try {
            return valueOfLocked(obj);
        } finally {
            GIL.releaseNoCleanup();
        }
    }

    public static long nativeHandle(IRubyObject obj) {
        return Handle.valueOf(obj).getAddress();
    }

    static long nativeHandleLocked(IRubyObject obj) {
        return Handle.valueOfLocked(obj).getAddress();
    }
}
