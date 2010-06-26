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

import java.util.ArrayList;
import java.util.List;
import org.jruby.Ruby;
import org.jruby.RubyFixnum;
import org.jruby.runtime.builtin.IRubyObject;

public final class Handle {
    private final Ruby runtime;
    private final long address;

    private List<IRubyObject> linkedObjects = null;

    Handle(Ruby runtime, long address) {
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


    void link(List<IRubyObject> fields) {
        this.linkedObjects = new ArrayList<IRubyObject>(fields);
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            Native.getInstance(runtime).freeHandle(address);
        } finally {
            super.finalize();
        }
    }



    public static final synchronized Handle valueOf(IRubyObject obj) {
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
        Handle handle = new Handle(runtime, nativeHandle);

        GC.register(obj, handle);

        return handle;
    }

    public static long nativeHandle(IRubyObject obj) {
        return Handle.valueOf(obj).getAddress();
    }

}
