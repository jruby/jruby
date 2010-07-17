/*
 * Copyright (C) 2009, 2010 Wayne Meissner
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

import org.jruby.RubyModule;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public final class NativeMethod3 extends AbstractNativeMethod {
    public NativeMethod3(RubyModule clazz, int arity, long function) {
        super(clazz, arity, function);
    }


    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, 
            IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        pre(context, self, klazz, name);
        try {
            return Native.getInstance(context.getRuntime()).callMethod3(function,
                    Handle.nativeHandle(self),
                    Handle.nativeHandle(arg0),
                    Handle.nativeHandle(arg1),
                    Handle.nativeHandle(arg2));
        } finally {
            post(context);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name,
            IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        pre(context, self, klazz, name, block);
        try {
            return Native.getInstance(context.getRuntime()).callMethod3(function,
                    Handle.nativeHandle(self),
                    Handle.nativeHandle(arg0),
                    Handle.nativeHandle(arg1),
                    Handle.nativeHandle(arg2));
        } finally {
            post(context);
        }
    }

}
