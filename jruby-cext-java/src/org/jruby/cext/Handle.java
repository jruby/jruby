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
import org.jruby.RubyBoolean;
import org.jruby.RubyFixnum;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubySymbol;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.builtin.IRubyObject;

public final class Handle {
    private static final long FIXNUM_MAX = Integer.getInteger("sun.arch.data.model") == 32
            ? (Long.MAX_VALUE >> 1) : ((long) Integer.MAX_VALUE >> 1);
    private static final long FIXNUM_MIN = Integer.getInteger("sun.arch.data.model") == 32
            ? (Long.MIN_VALUE >> 1) : ((long) Integer.MIN_VALUE >> 1);

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

    static Handle valueOf(IRubyObject obj) {
        Handle h = GC.lookup(obj);
        if (h != null) {
            return h;
        }

        Ruby runtime = obj.getRuntime();
        long nativeHandle;


        if (obj instanceof RubyObject) {
            int type = ((RubyObject) obj).getNativeTypeIndex();
            switch (type) {
                case ClassIndex.FIXNUM: {
                    final long val = ((RubyFixnum) obj).getLongValue();
                    nativeHandle = (val < FIXNUM_MAX && val >= FIXNUM_MIN)
                            ? ((val << 1) | 0x1)
                            : Native.getInstance(runtime).newFixnumHandle(obj, val);
                    }
                    break;

                case ClassIndex.FLOAT:
                    nativeHandle = Native.getInstance(runtime).newFloatHandle(obj, ((RubyNumeric) obj).getDoubleValue());
                    break;

                case ClassIndex.SYMBOL:
                    Native.getInstance(runtime).newSymbolHandle((RubySymbol) obj);
                    nativeHandle = ((long) ((RubySymbol) obj).getId() << 8) | 0xeL;
                    break;

                default:
                    nativeHandle = Native.getInstance(runtime).newHandle(obj, type);
                    break;
            }
        } else {
            nativeHandle = Native.getInstance(runtime).newHandle(obj, ClassIndex.OBJECT);
        }

        Handle handle = newHandle(runtime, obj, nativeHandle);

        GC.register(obj, handle);

        return handle;
    }

    static long nativeHandle(IRubyObject obj) {
        if (obj.getClass() == RubyFixnum.class) {
            final long val = ((RubyFixnum) obj).getLongValue();
            if (val < FIXNUM_MAX && val >= FIXNUM_MIN) {
                return ((val << 1) | 0x1);
            }
        }
        return Handle.valueOf(obj).getAddress();
    }
}
