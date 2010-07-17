/*
 * Copyright (C) 2009 Wayne Meissner
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
import org.jruby.internal.runtime.methods.CallConfiguration;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

public abstract class AbstractNativeMethod extends DynamicMethod {
    protected final Arity arity;
    protected final long function;
    private final Native nativeInstance;

    public AbstractNativeMethod(RubyModule clazz, int arity, long function) {
        super(clazz, Visibility.PUBLIC, CallConfiguration.FrameBacktraceScopeFull);
        this.arity = Arity.createArity(arity);
        this.function = function;
        this.nativeInstance = Native.getInstance(clazz.getRuntime());
    }

    @Override
    public final DynamicMethod dup() {
        return this;
    }

    @Override
    public final Arity getArity() {
        return arity;
    }

    @Override
    public final boolean isNative() {
        return true;
    }

    static void pre(ThreadContext context, IRubyObject self, RubyModule klazz, String name) {
        context.preMethodFrameOnly(self.getType(), name, self, Block.NULL_BLOCK);
        GIL.acquire();
    }

    static void pre(ThreadContext context, IRubyObject self, RubyModule klazz, String name, Block block) {
        context.preMethodFrameOnly(self.getType(), name, self, block);
        GIL.acquire();
    }


    static void post(ThreadContext context) {
        GIL.release(context);
        context.postMethodFrameOnly();
    }

    final Native getNativeInstance() {
        return nativeInstance;
    }

    
    @Override
    public IRubyObject call(ThreadContext context, IRubyObject recv, RubyModule clazz,
            String name, IRubyObject[] args) {
        pre(context, recv, clazz, name);
        try {
            return getNativeInstance().callMethod(context, function, recv, arity.getValue(), args);
        } finally {
            post(context);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject recv, RubyModule clazz,
            String name, IRubyObject[] args, Block block) {

        pre(context, recv, clazz, name, block);
        try {
            return getNativeInstance().callMethod(context, function, recv, arity.getValue(), args);
        } finally {
            post(context);
        }
    }

}
